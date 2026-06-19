# Pinia 状态管理修复指南

## 问题描述

个人资料页面（Profile.vue）中使用 `v-model` 直接绑定 Pinia store 中 `userInfo` 对象的嵌套属性，违反 Pinia 状态管理最佳实践，可能引发 Vue 编译警告或运行时响应式更新异常。

### 错误代码示例

```vue
<!-- 错误：直接 v-model 绑定 store 的嵌套属性 -->
<el-input v-model="userStore.userInfo?.username" disabled />
<el-input v-model="userStore.userInfo?.nickname" disabled />
```

---

## 修复方案

### 方案一：使用本地响应式数据 + store 同步（推荐用于只读展示）

在组件内创建本地 `reactive` 对象，从 store 读取数据后同步到本地，模板只绑定本地数据。

```typescript
import { reactive, onMounted } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

const formData = reactive({
  username: '',
  nickname: '',
  email: ''
})

const syncFormFromStore = () => {
  if (userStore.userInfo) {
    formData.username = userStore.userInfo.username
    formData.nickname = userStore.userInfo.nickname
    formData.email = userStore.userInfo.email
  }
}

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  syncFormFromStore()
})
```

```vue
<el-input :value="formData.username" disabled />
<el-input :value="formData.nickname" disabled />
```

### 方案二：在 store 中添加 action 方法，使用 $patch 更新（推荐用于可编辑场景）

在 store 中定义专门的 action，通过 `$patch` 批量更新状态。

```typescript
// store/user.ts
actions: {
  setUserInfo(info: Partial<UserInfo>) {
    if (!this.userInfo) return
    this.$patch({
      userInfo: {
        ...this.userInfo,
        ...info
      }
    })
  }
}
```

组件中使用 `computed` + `v-model` 双向绑定：

```typescript
import { computed } from 'vue'

const nickname = computed({
  get: () => userStore.userInfo?.nickname || '',
  set: (val) => userStore.setUserInfo({ nickname: val })
})
```

```vue
<el-input v-model="nickname" />
```

### 方案三：使用 storeToRefs（仅读取，不建议写）

Pinia 提供 `storeToRefs` 可以解构出响应式的 state 属性，但**仅适用于读取**，写入仍需通过 action。

```typescript
import { storeToRefs } from 'pinia'

const { userInfo } = storeToRefs(userStore)  // 只读，不要直接修改 userInfo.value.xxx
```

---

## Pinia 最佳实践总结

| 操作 | 推荐方式 | 禁止方式 |
|------|----------|----------|
| 读取状态 | `store.state` 或 `storeToRefs` | - |
| 修改单个属性 | 通过 action 方法 | `store.prop = value` |
| 修改多个属性 | `store.$patch({...})` 或 action | 逐个赋值 |
| 修改嵌套对象 | `$patch` 合并更新 | 直接 `store.obj.key = value` |
| 异步操作 | 在 action 中处理 | 组件内直接修改 store |

### 为什么不能直接修改 store？

1. **调试困难**：无法通过 devtools 追踪状态变化来源
2. **违反单向数据流**：状态修改应该是可预测、可追踪的
3. **响应式异常**：直接修改嵌套对象可能导致某些场景下响应式失效
4. **代码维护性差**：状态散落在各个组件中，难以统一管理

---

## 本次修复涉及文件

| 文件 | 修改内容 |
|------|----------|
| `frontend/src/store/user.ts` | 新增 `setUserInfo` action，使用 `$patch` 方式更新 |
| `frontend/src/views/Profile.vue` | 改用本地 `reactive` 对象 `formData` 存储表单展示数据，模板只绑定本地数据，通过 `syncFormFromStore()` 从 store 同步 |

---

## 验证方法

1. 打开个人中心页面，确认用户信息正常展示
2. 打开 Vue Devtools，查看 Pinia 状态变更是否有 action 日志
3. 控制台无 Vue/Pinia 相关警告

---

# 登录 502 Bad Gateway 修复指南

## 问题描述

用户在登录过程中出现 `502 Bad Gateway` 错误，无法正常登录系统。该问题主要发生在 Docker Compose 部署环境下，也可能出现在本地开发环境。

---

## 根因分析

通过对服务器配置、网络连接、后端服务状态及负载均衡设置的全面检查，定位到以下三个连锁问题：

### 根因一：后端健康检查接口需要认证（主要原因）

Docker Compose 中 backend 服务的 `healthcheck` 配置如下：

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget --spider http://localhost:8080/api/user/list?pageNum=1&pageSize=1 || exit 1"]
```

该健康检查调用了 `/api/user/list` 接口，但在 Spring Security 配置中：

```java
// SecurityConfig.java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/user/login").permitAll()
    .anyRequest().authenticated()  // 其他所有接口（含 /api/user/list）都需要认证
)
```

因此 `wget` 请求会被 Spring Security 拦截并返回 `401 Unauthorized`，导致：
- `wget --spider` 检测到非 200 响应码
- Docker 将 backend 服务标记为 **unhealthy**
- 前端容器依赖 `backend: condition: service_healthy`，永远等不到 backend healthy
- Nginx 启动后转发请求时，backend 状态异常，返回 `502 Bad Gateway`

### 根因二：healthcheck 命令中的 `&` 符号未转义

在 shell 命令中，`&` 是特殊字符，表示将前面的命令放到后台执行。命令：

```bash
wget --spider http://localhost:8080/api/user/list?pageNum=1&pageSize=1
```

实际被 shell 解释为：
1. 后台执行 `wget --spider http://localhost:8080/api/user/list?pageNum=1`
2. 再执行 `pageSize=1`（被当作变量赋值，无实际作用）

导致 healthcheck 请求实际缺失参数，且无法正确判断执行结果。

### 根因三：前端开发环境代理超时

本地开发使用 Vite 代理时，默认超时较短，后端冷启动较慢时也可能出现类似 502/超时现象。

---

## 修复方案

### 步骤一：新增公开健康检查端点

创建 `backend/src/main/java/com/example/usermanager/controller/HealthController.java`：

```java
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        return Result.success(data);
    }
}
```

该接口返回：
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "status": "UP",
    "timestamp": 1718745600000
  }
}
```

### 步骤二：在 Spring Security 中放行健康检查端点

修改 `SecurityConfig.java`：

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/user/login", "/api/health").permitAll()  // 新增 /api/health
    .anyRequest().authenticated()
)
```

### 步骤三：在 JWT 过滤器中跳过公开路径

修改 `JwtAuthenticationFilter.java`，在过滤器最开头放行公开路径，避免用户状态检查误拦截健康检查：

```java
@Override
protected void doFilterInternal(...) {
    String path = request.getRequestURI();
    if ("/api/health".equals(path) || "/api/user/login".equals(path)) {
        filterChain.doFilter(request, response);
        return;
    }
    // ... 其余 JWT 校验逻辑
}
```

### 步骤四：修正 Docker Compose healthcheck

修改 `docker-compose.yml` 中 backend 的 healthcheck：

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget -q --spider http://localhost:8080/api/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 40s
```

改动要点：
- 使用无需认证的 `/api/health` 接口
- 添加 `-q` 参数静默 wget 输出，减少日志噪音
- URL 不再包含 `&` 参数，避免 shell 解析问题

### 步骤五：优化 Vite 开发代理超时

修改 `frontend/vite.config.ts`，增加代理超时：

```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      timeout: 60000,       // 新增：连接超时 60s
      proxyTimeout: 60000,  // 新增：响应超时 60s
    },
  },
},
```

---

## 本次修复涉及文件

| 文件 | 修改内容 |
|------|----------|
| `backend/src/main/java/com/example/usermanager/controller/HealthController.java` | **新增文件**，提供公开的 `/api/health` 健康检查端点 |
| `backend/src/main/java/com/example/usermanager/config/SecurityConfig.java` | 在 `permitAll` 列表中新增 `/api/health` |
| `backend/src/main/java/com/example/usermanager/security/JwtAuthenticationFilter.java` | 在过滤器开头放行 `/api/health` 和 `/api/user/login`，避免用户状态检查误拦截 |
| `docker-compose.yml` | backend healthcheck 改用 `/api/health`，添加 `-q` 静默参数 |
| `frontend/vite.config.ts` | 新增代理超时配置 `timeout` 和 `proxyTimeout` 为 60s |

---

## 验证方法

### 一、Docker 部署环境验证

```bash
# 1. 重新构建并启动所有服务
docker-compose up -d --build

# 2. 观察容器健康状态
docker ps
# 预期输出中 backend 容器 STATUS 列显示 "(healthy)"

# 3. 单独测试健康检查接口
docker exec user-manager-backend wget -qO- http://localhost:8080/api/health
# 预期输出：{"code":200,"message":"成功","data":{"status":"UP","timestamp":...}}

# 4. 测试登录接口
curl -X POST http://localhost:32753/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# 预期返回 code=200 且 data 为 token 字符串

# 5. 浏览器访问 http://localhost:32753，使用 admin/123456 登录
# 预期：登录成功，无 502 错误
```

### 二、本地开发环境验证

```bash
# 1. 启动后端（确保 MySQL 已运行在 12753 端口）
cd backend && mvn spring-boot:run

# 2. 测试健康检查
curl http://localhost:8080/api/health
# 预期：{"code":200,...,"status":"UP"}

# 3. 启动前端
cd frontend && npm run dev

# 4. 浏览器访问 http://localhost:3000 登录
# 预期：登录流程顺畅，无超时或 502
```

### 三、边界条件测试

| 测试场景 | 操作步骤 | 预期结果 |
|----------|----------|----------|
| 正常登录 | admin / 123456 | 登录成功，跳转到首页 |
| 禁用账号登录 | 先将 zhangsan 禁用，再尝试登录 | 提示"账号已被禁用，请联系管理员"，停留在登录页 |
| 登录后被禁用 | 用 zhangsan 登录后，在另一浏览器将其禁用，再刷新页面 | 自动退出并跳转到登录页，提示禁用信息 |
| 健康检查匿名访问 | 未携带 token 直接访问 `/api/health` | 返回 200 和 status=UP |
| 容器重启顺序 | `docker-compose restart backend` | backend 标记 healthy 后，nginx 自动恢复转发，无需手动干预 |

---

## 问题预防建议

1. **健康检查端点必须匿名访问**：所有容器健康检查使用的接口必须在安全框架中显式放行，切勿依赖认证接口。
2. **shell 特殊字符转义**：`docker-compose.yml` 中涉及 URL 参数时，避免使用 `&`，或用引号包裹整个 URL。
3. **JWT 过滤器白名单**：对于登录、健康检查等公开路径，应在自定义过滤器最开头直接放行，避免后续逻辑误拦截。
4. **合理超时配置**：反向代理（Nginx/Vite）和健康检查都应设置合理的超时时间，覆盖 Spring Boot 冷启动耗时（通常 20-40 秒）。

---

# 后端循环依赖阻塞容器启动修复指南

## 问题描述

后端 Spring Boot 容器启动失败，抛出 `BeanCurrentlyInCreationException` 或 `UnsatisfiedDependencyException`（循环依赖异常），Docker 将其标记为 unhealthy，前端容器因 `depends_on: backend: condition: service_healthy` 永远等待，导致整个系统无法启动。

典型错误日志：
```
org.springframework.beans.factory.BeanCurrentlyInCreationException:
  Error creating bean with name 'securityConfig':
    Requested bean is currently in creation: Is there an unresolvable circular reference?
```

---

## 根因分析

### 循环依赖链（创建顺序）

```
① Spring 容器启动，开始扫描并创建 @Configuration 类

② 创建 SecurityConfig (@Configuration)
   ├─ 发现 @Autowired JwtAuthenticationFilter 字段（字段注入）
   │    → Spring 必须先创建 JwtAuthenticationFilter
   │
   └─ 发现类内部声明的 @Bean PasswordEncoder
        → 但 @Bean 方法要等 SecurityConfig 实例化后才会执行

③ 创建 JwtAuthenticationFilter (@Component)
   └─ @Autowired UserService
        → Spring 必须先创建 UserService

④ 创建 UserServiceImpl (@Service，实现 UserService)
   └─ @Autowired PasswordEncoder
        → Spring 必须先找到 PasswordEncoder Bean

⑤ 找 PasswordEncoder Bean
   └→ 定义在 SecurityConfig 内部的 @Bean
        → 但 SecurityConfig 还在第 ② 步，实例尚未完成
           → 回到第 ② 步，形成死锁！

⑥ Spring 抛出循环依赖异常，容器启动失败
```

### 三个设计缺陷共同导致循环

| # | 位置 | 缺陷 |
|---|------|------|
| 1 | `SecurityConfig` | **字段注入** `JwtAuthenticationFilter`，而不是 @Bean 方法参数注入 |
| 2 | `SecurityConfig` | 将 `PasswordEncoder` / `AuthenticationManager` 的 @Bean 声明在同一个配置类里 |
| 3 | `JwtAuthenticationFilter` / `UserServiceImpl` | 大量使用 **立即依赖**（字段 @Autowired 无 @Lazy），即使只是运行期才用到的依赖 |

---

## 修复方案

采用 **三层解耦** 组合方案，彻底打破循环：

### 方案一：拆分配置类（结构性重构，消除主循环）

#### 新建 SecurityBeanConfig.java

将 `PasswordEncoder` 和 `AuthenticationManager` 从 `SecurityConfig` 拆分到独立的配置类：

```java
// config/SecurityBeanConfig.java （新建）
@Configuration
public class SecurityBeanConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
```

#### 重构 SecurityConfig.java

- 移除 `@Autowired JwtAuthenticationFilter` 字段注入
- 改为在 `filterChain()` 方法**参数列表**中注入（Spring 延迟解析依赖）
- 移除已拆分的两个 @Bean 声明

```java
// config/SecurityConfig.java （重构后）
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        // ... 过滤器注册逻辑
        return http.build();
    }
}
```

> **原理**：@Bean 方法参数依赖由 Spring 在真正调用方法时才解析，而不是配置类实例化阶段。这切断了 `SecurityConfig → JwtAuthenticationFilter` 的硬依赖。

### 方案二：关键路径使用 @Lazy 延迟初始化（保险层）

对仍可能产生循环的运行期依赖，使用 `@Lazy` 注解：

#### JwtAuthenticationFilter.java

```java
// security/JwtAuthenticationFilter.java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    @Lazy                  // ← 新增：首次实际调用时才创建代理对象
    private UserService userService;
    // ...
}
```

#### UserServiceImpl.java

```java
// service/impl/UserServiceImpl.java
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    @Lazy                  // ← 新增
    private PasswordEncoder passwordEncoder;

    @Autowired
    @Lazy                  // ← 新增
    private JwtUtils jwtUtils;
    // ...
}
```

> **原理**：@Lazy 会生成一个代理对象注入，第一次实际方法调用时才触发目标 Bean 的初始化。完美适用于"只有请求进来才会用到"的运行期依赖。

### 方案三：优化 Docker Compose 启动容错（运维层）

```yaml
# docker-compose.yml
services:
  db:
    restart: unless-stopped              # 异常自动重启
    healthcheck:
      retries: 10                        # MySQL 启动慢，增加重试

  backend:
    restart: unless-stopped              # 异常自动重启
    healthcheck:
      retries: 12                        # Spring 冷启动慢，增加重试
      start_period: 60s                  # 延长启动宽限期

  frontend:
    restart: unless-stopped              # 异常自动重启
```

---

## 修复后的依赖图（无环）

```
┌───────────────────────────────┐
│  SecurityBeanConfig           │ 独立存在，只产出 Bean
│  └─ PasswordEncoder @Bean     │ 不依赖其他组件
│  └─ AuthenticationManager     │
└──────────────┬────────────────┘
               │产出
               ▼
┌───────────────────────────────┐      ┌──────────────────────────┐
│  UserServiceImpl              │──@Lazy→│  JwtUtils               │
│  └─ @Lazy PasswordEncoder     │      │  （纯工具类，无依赖）     │
└──────┬────────────────────────┘      └──────────────────────────┘
       │@Lazy（反向代理，非强依赖）
       ▼
┌───────────────────────────────┐
│  JwtAuthenticationFilter      │
│  └─ JwtUtils                  │
│  └─ @Lazy UserService         │← 首次请求才初始化，不参与启动链
└──────┬────────────────────────┘
       │方法参数注入（@Bean 调用时才解析）
       ▼
┌───────────────────────────────┐
│  SecurityConfig               │ 最轻量，只组装 Filter 链
│  └─ filterChain(@Bean 方法)   │
└───────────────────────────────┘
```

---

## 本次修复涉及文件

| 文件 | 修改内容 |
|------|----------|
| `backend/src/main/java/com/example/usermanager/config/SecurityBeanConfig.java` | **新增文件**，拆分 PasswordEncoder 和 AuthenticationManager 的 @Bean 声明 |
| `backend/src/main/java/com/example/usermanager/config/SecurityConfig.java` | 移除字段注入和两个 @Bean，改用 filterChain 方法参数注入 JwtAuthenticationFilter |
| `backend/src/main/java/com/example/usermanager/security/JwtAuthenticationFilter.java` | UserService 字段添加 `@Lazy` |
| `backend/src/main/java/com/example/usermanager/service/impl/UserServiceImpl.java` | PasswordEncoder 和 JwtUtils 字段添加 `@Lazy` |
| `docker-compose.yml` | 三服务添加 `restart: unless-stopped`，db/backend healthcheck 的 retries 和 start_period 放宽 |

---

## 验证方法

### 一、后端独立启动验证（无前端容器干扰）

```bash
cd backend

# 1. 编译检查（Maven）
mvn clean compile -q
# 预期：BUILD SUCCESS，无循环依赖异常

# 2. 启动后端（需本地 MySQL 运行在 12753）
mvn spring-boot:run

# 3. 观察启动日志
# 预期：
#   - 无 BeanCurrentlyInCreationException
#   - 无 UnsatisfiedDependencyException
#   - 最终出现 "Started UserManagerApplication in X.XXX seconds"

# 4. 测试健康检查
curl http://localhost:8080/api/health
# 预期：{"code":200,...,"status":"UP"}

# 5. 测试登录
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# 预期：code=200，返回 token
```

### 二、Docker 全链路启动验证

```bash
# 1. 清理旧容器和镜像（可选）
docker-compose down -v

# 2. 重新构建并启动
docker-compose up -d --build

# 3. 实时观察启动过程（关键！）
docker-compose logs -f backend
# 预期：
#   30-60 秒内出现 "Started UserManagerApplication"
#   无循环依赖异常栈

# 4. 检查容器健康状态（持续观察 2 分钟）
watch -n 5 'docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"'
# 预期（按时间顺序）：
#   T+30s  db: healthy
#   T+60s  backend: healthy
#   T+70s  frontend: Up

# 5. 前端可访问性测试
curl -s -o /dev/null -w "%{http_code}" http://localhost:32753/
# 预期：200

# 6. 端到端登录测试（前端 → Nginx → 后端）
curl -X POST http://localhost:32753/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# 预期：code=200，返回 token（不是 502）
```

### 三、边界条件与故障恢复测试

| 测试场景 | 操作步骤 | 预期结果 |
|----------|----------|----------|
| 后端启动慢 | 故意限制 CPU 或第一次冷启动 | 前端容器不会报"依赖超时"，backend 通过重启策略和更长的 start_period 成功启动 |
| 后端运行时崩溃 | `docker kill user-manager-backend` | `restart: unless-stopped` 自动重启；重启期间前端返回 502，但恢复后自动正常（无手动干预） |
| MySQL 重启 | `docker-compose restart db` | backend healthcheck 短暂失败，DB 恢复后 backend 自动重新连接，无需重启 backend 容器 |
| 循环依赖回归 | 尝试在 SecurityConfig 中重新添加字段注入 Filter | 编译 / 启动阶段立即报错（可通过 mvn test 提前发现） |

---

## 问题预防建议（Spring 循环依赖最佳实践）

### 1. @Configuration 类：方法参数注入 > 字段注入

❌ **错误**：字段注入会强制前置依赖
```java
@Configuration
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter filter;  // 配置类还没创建就需要 Filter！
}
```

✅ **正确**：方法参数由 Spring 在调用 @Bean 时才解析
```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter filter) {
    // ...
}
```

### 2. @Bean 与依赖它的 @Component：拆到不同 @Configuration

同一个配置类里既声明 Bean 又依赖其他 Bean，是循环依赖的重灾区。遵循**单一职责**：
- `*BeanConfig` / `*Config`：只声明 @Bean（工具类、编码器、管理器等）
- `*ChainConfig` / `*WebConfig`：只做装配和串联

### 3. 运行期依赖一律加 @Lazy

以下场景的依赖，启动时根本用不到，加 `@Lazy` 零副作用：
- `@Service` 中的 `JwtUtils`、`PasswordEncoder`、`RedisTemplate`（只有登录/请求时才用）
- `@Filter` 中的 `UserService`、权限校验服务（只有请求进来才用）
- `@RestControllerAdvice` 中的邮件/告警服务（只有异常时才用）

### 4. 优先使用构造器注入（配合 @Lazy）

构造器注入能让依赖关系更清晰，配合 @Lazy 可在构造时标记：

```java
@Service
public class UserServiceImpl {
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
}
```

### 5. CI 中加入循环依赖检测

在 `pom.xml` 中加 Maven Enforcer 规则，或在启动脚本后 `grep` 关键异常词，提前拦截。

---

## 总结：三层解耦方法论

| 层次 | 手段 | 适用场景 | 解决的问题 |
|------|------|----------|------------|
| 结构层 | 拆分 @Configuration，方法参数注入 | 配置类之间的循环 | 消除启动阶段的硬依赖 |
| 注入层 | @Lazy 延迟初始化 | 运行期才使用的依赖 | 避免创建顺序死锁 |
| 运维层 | restart 策略 + 健康检查容错 | 外部依赖慢启动 | 非代码问题的自愈 |

三层组合使用后，Spring Boot 启动链从「多米诺骨牌」变成「独立单元」，任一组件初始化问题不会阻塞整个系统。
