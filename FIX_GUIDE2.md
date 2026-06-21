# 前端依赖版本不一致修复指南

## 问题描述

前端项目执行 `npm ci` 时校验失败，原因是 `package.json` 与 `package-lock.json` 之间存在大量依赖版本信息不一致。

## 根因分析

### 1. Pinia peer 依赖冲突（主要问题）

- **pinia 2.1.7** 的 `peerDependencies` 中包含 `@vue/composition-api: ^1.4.0`（标记为 optional）
- **@vue/composition-api** 是 Vue 2 的组合式 API polyfill，其自身 peer 要求 `vue: <2.7`
- npm v7+ 严格解析模式下，会沿着 `pinia → @vue/composition-api → vue<2.7` 链路检测到冲突，触发 `ERESOLVE`
- pinia 2.3.1 已彻底移除 `@vue/composition-api` 依赖，是解决此冲突的正确版本

### 2. package.json 与 package-lock.json 版本不一致（核心问题）

`npm ci` 的校验逻辑要求 `package.json` 中的依赖版本必须与 `package-lock.json` 中记录的版本完全一致。但本次排查发现：

- `package.json` 使用精确版本号（如 `"axios": "1.6.7"`）
- `package-lock.json` 根目录声明使用 `^` 范围符（如 `"axios": "^1.6.7"`），实际解析到的版本远高于 package.json 声明

### 3. 版本不一致明细

| 包 | package.json 旧版本 | lock 声明（带 ^） | lock 实际版本 | 差异说明 |
|---|---|---|---|---|
| `axios` | 1.6.7 | ^1.6.7 | **1.13.4** | ❌ 主版本内跨多个 minor |
| `element-plus` | 2.6.1 | ^2.6.1 | **2.13.2** | ❌ 主版本内跨多个 minor |
| `@element-plus/icons-vue` | 2.3.1 | ^2.3.1 | **2.3.2** | ❌ minor 版本差异 |
| `@vitejs/plugin-vue` | 5.0.4 | ^5.0.4 | **5.2.4** | ❌ minor 版本差异 |
| `typescript` | 5.4.2 | ^5.4.2 | **5.9.3** | ❌ 主版本内跨多个 minor |
| `vite` | 5.1.6 | ^5.1.6 | **5.4.21** | ❌ 主版本内跨多个 minor |
| `vue-tsc` | 2.0.6 | ^2.0.6 | **2.2.12** | ❌ minor 版本差异 |
| `@types/node` | 20.11.28 | ^20.11.28 | **20.19.32** | ❌ 主版本内跨多个 minor |
| `vue` | 3.4.21→3.5.27 | 3.5.27 | 3.5.27 | ✅ 已在前一轮修复对齐 |
| `vue-router` | 4.3.0→4.6.4 | 4.6.4 | 4.6.4 | ✅ 已在前一轮修复对齐 |
| `pinia` | 2.1.7→2.3.1 | 2.3.1 | 2.3.1 | ✅ 已在前一轮修复对齐 |

## 修复方案

### 修复策略

采用 **将 package.json 版本对齐到 lock 文件实际版本** 的策略：
1. 将 `package.json` 中所有依赖版本更新为 `package-lock.json` 中实际安装的版本号
2. 将 `package-lock.json` 根目录声明中的 `^` 范围符全部移除，改为精确版本号
3. 同步 `name` 和 `version` 字段

### 修改的文件

#### 1. `frontend/package.json`

```diff
 "dependencies": {
   "vue": "3.5.27",
   "vue-router": "4.6.4",
   "pinia": "2.3.1",
-  "axios": "1.6.7",
-  "element-plus": "2.6.1",
-  "@element-plus/icons-vue": "2.3.1"
+  "axios": "1.13.4",
+  "element-plus": "2.13.2",
+  "@element-plus/icons-vue": "2.3.2"
 },
 "devDependencies": {
-  "@vitejs/plugin-vue": "5.0.4",
-  "typescript": "5.4.2",
-  "vite": "5.1.6",
-  "vue-tsc": "2.0.6",
-  "@types/node": "20.11.28"
+  "@vitejs/plugin-vue": "5.2.4",
+  "typescript": "5.9.3",
+  "vite": "5.4.21",
+  "vue-tsc": "2.2.12",
+  "@types/node": "20.19.32"
 }
```

#### 2. `frontend/package-lock.json`

同步更新以下内容：
- 根级 `version`: `"0.0.0"` → `"1.0.0"`（与 package.json 一致）
- 根目录 `packages[""]` 的 `version`: `"0.0.0"` → `"1.0.0"`
- 所有 `dependencies` 和 `devDependencies` 声明：移除 `^` 范围符，使用精确版本号与实际安装版本一致

修改前 → 修改后（lock 文件头部声明）：

```diff
 "": {
   "name": "frontend",
-  "version": "0.0.0",
+  "version": "1.0.0",
   "dependencies": {
-    "@element-plus/icons-vue": "^2.3.1",
-    "axios": "^1.6.7",
-    "element-plus": "^2.6.1",
+    "@element-plus/icons-vue": "2.3.2",
+    "axios": "1.13.4",
+    "element-plus": "2.13.2",
     "pinia": "2.3.1",
     "vue": "3.5.27",
     "vue-router": "4.6.4"
   },
   "devDependencies": {
-    "@types/node": "^20.11.28",
-    "@vitejs/plugin-vue": "^5.0.4",
-    "typescript": "^5.4.2",
-    "vite": "^5.1.6",
-    "vue-tsc": "^2.0.6"
+    "@types/node": "20.19.32",
+    "@vitejs/plugin-vue": "5.2.4",
+    "typescript": "5.9.3",
+    "vite": "5.4.21",
+    "vue-tsc": "2.2.12"
   }
 }
```

### 修复后版本一致性验证

| 包 | package.json | lock 声明 | lock 实际版本 | 状态 |
|---|---|---|---|---|
| `vue` | 3.5.27 | 3.5.27 | 3.5.27 | ✅ |
| `vue-router` | 4.6.4 | 4.6.4 | 4.6.4 | ✅ |
| `pinia` | 2.3.1 | 2.3.1 | 2.3.1 | ✅ |
| `axios` | 1.13.4 | 1.13.4 | 1.13.4 | ✅ |
| `element-plus` | 2.13.2 | 2.13.2 | 2.13.2 | ✅ |
| `@element-plus/icons-vue` | 2.3.2 | 2.3.2 | 2.3.2 | ✅ |
| `@vitejs/plugin-vue` | 5.2.4 | 5.2.4 | 5.2.4 | ✅ |
| `typescript` | 5.9.3 | 5.9.3 | 5.9.3 | ✅ |
| `vite` | 5.4.21 | 5.4.21 | 5.4.21 | ✅ |
| `vue-tsc` | 2.2.12 | 2.2.12 | 2.2.12 | ✅ |
| `@types/node` | 20.19.32 | 20.19.32 | 20.19.32 | ✅ |

## 验证方式

### 1. 验证依赖安装

```bash
cd frontend
rm -rf node_modules
npm ci
```

预期结果：安装成功，无 `ERESOLVE` 错误，无版本校验失败。

### 2. 验证类型检查

```bash
npm run type-check
```

预期结果：TypeScript 类型检查通过。

### 3. 验证项目构建

```bash
npm run build
```

预期结果：Vite 构建成功，产物正常生成到 `dist/` 目录。

### 4. 验证开发启动

```bash
npm run dev
```

预期结果：开发服务器正常启动，所有功能正常。

## 注意事项

1. **精确版本号策略**：修复后所有依赖均使用精确版本号（不带 `^` 或 `~`），可避免未来 `npm install` 时自动升级导致版本漂移
2. **升级影响范围**：本次修复涉及多个依赖升级（特别是 vue 3.4→3.5、vite 5.1→5.4），建议全面回归测试
3. **后续维护建议**：如需更新依赖版本，建议执行 `npm install <package>@<version>` 并提交更新后的 `package.json` 和 `package-lock.json`，而非手动编辑版本号

## 备选方案（如无法升级 Vue）

如果项目因某些原因必须锁定在 Vue 3.4.x，可以使用以下替代方案：

```bash
npm install --legacy-peer-deps
```

或在项目根目录创建 `.npmrc` 文件：

```ini
legacy-peer-deps=true
```

> **注意**：`--legacy-peer-deps` 会跳过 peer 依赖严格检查，属于治标不治本的方案。推荐优先使用升级依赖的方案。

## 相关参考

- [npm ci 文档](https://docs.npmjs.com/cli/v10/commands/npm-ci)
- [Pinia CHANGELOG](https://github.com/vuejs/pinia/blob/main/packages/pinia/CHANGELOG.md)
- [npm peerDependencies 文档](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#peerdependencies)

---

## 三、TypeScript 编译错误修复

### 问题描述

前端项目在 TypeScript 编译过程中出现两个错误：
1. `ImportMeta` 类型定义缺失错误
2. `DeptInfo` 类型未导出错误

---

### 问题 1：ImportMeta 类型定义缺失

#### 根因分析

- 项目使用了 Vite 构建，在 `src/utils/request.ts` 中通过 `import.meta.env.VITE_API_URL` 访问环境变量
- 项目中没有任何 `.d.ts` 类型声明文件来提供 Vite 客户端类型
- `tsconfig.json` 的 `lib` 配置只包含 `["ESNext", "DOM"]`，不含 Vite 定义的 `ImportMeta` 扩展类型
- TypeScript 无法识别 `import.meta.env` 的类型，导致编译报错

#### 修复方案

新增 `frontend/src/vite-env.d.ts` 文件：

```typescript
/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string
  readonly VITE_API_PROXY_TARGET: string
  readonly VITE_APP_ENV: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

说明：
- `/// <reference types="vite/client" />` 引入 Vite 官方提供的客户端类型声明（包含基础的 `ImportMeta` 定义和 `import.meta.env` 类型）
- 自定义 `ImportMetaEnv` 接口显式列出项目使用的所有 `VITE_*` 环境变量，提供完整的类型提示和校验
- `tsconfig.json` 已配置 `"include": ["src/**/*.ts", "src/**/*.d.ts", ...]`，新文件会被自动包含，无需修改配置

#### 影响的文件

| 文件 | 变更 |
|---|---|
| [vite-env.d.ts](file:///d:/Desktop/%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9%20(2)/label-2753/2753/frontend/src/vite-env.d.ts) | 新增（含 ImportMeta 类型扩展） |

---

### 问题 2：DeptInfo 类型未导出

#### 根因分析

- `DeptInfo` 接口定义在 `src/store/user.ts` 中（第 28 行），但未使用 `export` 关键字导出
- `src/views/Home.vue`（第 505 行）和 `src/views/Dept.vue`（第 202 行）都尝试使用 `import type { DeptInfo } from '@/store/user'` 导入
- TypeScript 严格模式下，模块内部未导出的类型无法被外部文件引用，导致编译报错 "Module has no exported member 'DeptInfo'"

#### 修复方案

在 `frontend/src/store/user.ts` 中为所有接口类型添加 `export` 关键字：

```diff
- interface RoleInfo {
+ export interface RoleInfo {
    id: number
    ...
  }

- interface PermissionInfo {
+ export interface PermissionInfo {
    id: number
    ...
  }

- interface DeptInfo {
+ export interface DeptInfo {
    id: number
    ...
    children?: DeptInfo[]
  }

- interface UserInfo {
+ export interface UserInfo {
    userId: number
    ...
  }
```

#### 验证导入文件

所有导入 `DeptInfo` 的文件语句均正确，无需修改：

| 文件 | 导入语句 | 状态 |
|---|---|---|
| [Home.vue](file:///d:/Desktop/%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9%20(2)/label-2753/2753/frontend/src/views/Home.vue#L505-L505) | `import type { DeptInfo } from '@/store/user'` | ✅ 正确 |
| [Dept.vue](file:///d:/Desktop/%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9%20(2)/label-2753/2753/frontend/src/views/Dept.vue#L202-L202) | `import type { DeptInfo } from '@/store/user'` | ✅ 正确 |

#### 影响的文件

| 文件 | 变更 |
|---|---|
| [user.ts](file:///d:/Desktop/%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9%20(2)/label-2753/2753/frontend/src/store/user.ts) | `RoleInfo`、`PermissionInfo`、`DeptInfo`、`UserInfo` 四个接口添加 `export` 关键字 |

---

### 验证方式

```bash
cd frontend

# 1. 类型检查（核心验证）
npm run type-check

# 2. 构建验证
npm run build
```

预期结果：TypeScript 编译通过，不再出现 `ImportMeta` 和 `DeptInfo` 相关错误。

### 相关参考

- [Vite 环境变量与模式](https://cn.vitejs.dev/guide/env-and-mode.html)
- [Vite 客户端类型](https://cn.vitejs.dev/guide/features.html#client-types)
- [TypeScript - Exporting types](https://www.typescriptlang.org/docs/handbook/2/modules.html#type-only-exports-and-imports)

---

## 四、Docker 数据库容器启动失败修复

### 问题描述

在 Docker 环境执行 `docker compose up -d` 启动容器时，`2753-db`（db 依赖容器）持续处于 `Restarting (1)` 状态，无法正常运行。后端和前端容器也因依赖 db 健康检查而无法启动。

### 错误现象

```bash
$ docker ps -a
CONTAINER ID   IMAGE        STATUS                          NAMES
fe07e7029317   mysql:8.0    Restarting (1) 23 seconds ago   2753-db
```

```bash
$ docker logs 2753-db
[ERROR] [Entrypoint]: Database is uninitialized and password option is not specified
    You need to specify one of the following as an environment variable:
    - MYSQL_ROOT_PASSWORD
    - MYSQL_ALLOW_EMPTY_PASSWORD
    - MYSQL_RANDOM_ROOT_PASSWORD
```

---

### 根因分析

MySQL 官方镜像（mysql:8.0）在**首次初始化**（空数据目录）时，**强制要求**必须指定以下三个环境变量之一来确定 root 密码策略：

| 变量 | 含义 |
|---|---|
| `MYSQL_ROOT_PASSWORD` | 设置固定的 root 密码（推荐） |
| `MYSQL_ALLOW_EMPTY_PASSWORD` | 允许 root 密码为空（不推荐） |
| `MYSQL_RANDOM_ROOT_PASSWORD` | 随机生成 root 密码并打印到日志 |

**本项目问题**：

1. **缺少 `.env` 文件**：项目根目录没有提供任何环境变量文件，导致所有 `${VAR}` 替换为空字符串
2. **`docker-compose.yml` 缺少默认值兜底**：与 `docker-compose.dev.yml` 对比，生产环境 compose 文件中多个关键变量未设置 `:-默认值` 语法：

| 环境变量 | docker-compose.yml（修复前） | docker-compose.dev.yml | 问题 |
|---|---|---|---|
| `MYSQL_ROOT_PASSWORD` | `${MYSQL_ROOT_PASSWORD}`（无默认） | `${MYSQL_ROOT_PASSWORD:-root}` | ❌ 空值导致 MySQL 拒绝启动 |
| `healthcheck` 中的密码 | `-p${MYSQL_ROOT_PASSWORD}`（无默认） | `-p${MYSQL_ROOT_PASSWORD:-root}` | ❌ 即使启动也无法通过健康检查 |
| `SPRING_DATASOURCE_URL` | `${SPRING_DATASOURCE_URL}`（无默认） | 写死完整 JDBC URL | ❌ 后端无法连接数据库 |
| `SPRING_DATASOURCE_PASSWORD` | `${SPRING_DATASOURCE_PASSWORD}`（无默认） | `${MYSQL_ROOT_PASSWORD:-root}` | ❌ 后端认证失败 |
| `JWT_SECRET` | `${JWT_SECRET}`（无默认） | 有默认值 | ❌ JWT 签名密钥为空存在安全隐患 |

3. **残留的损坏数据卷**：之前多次尝试启动失败，`2753_mysql_data` 数据卷中留下了不完整的初始化目录（ibdata1 等文件已创建但未完成），即使后续修复环境变量，复用该卷也会导致不一致状态。

---

### 修复方案（三步走）

#### 步骤 1：清理损坏状态（强制重新初始化）

```bash
# 1. 停止并删除失败重启的容器
docker stop 2753-db
docker rm 2753-db

# 2. 删除损坏的 MySQL 数据卷（非空目录会跳过初始化）
docker volume rm 2753_mysql_data
```

> **关键说明**：MySQL 官方镜像的 entrypoint 脚本仅在 `/var/lib/mysql` **为空目录**时才执行初始化（执行 `/docker-entrypoint-initdb.d/*.sql`）。若该目录已存在文件（哪怕是不完整的），则直接跳初始化尝试启动，此时若之前未设置密码则 root 用户不存在，依然会失败。因此必须删除数据卷以保证**干净的首次初始化**。

#### 步骤 2：创建项目根目录 `.env` 文件

新增 [.env](file:///d:/Desktop/%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9%20(2)/label-2753/2753/.env)，完整声明所有 compose 引用的变量：

```dotenv
COMPOSE_PROJECT_NAME=2753

# MySQL
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=user_management
MYSQL_PUBLISH_PORT=12753
MYSQL_CHARSET=utf8mb4
MYSQL_COLLATION=utf8mb4_unicode_ci

# Backend
SPRING_PROFILES_ACTIVE=production
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/user_management?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=root
JWT_SECRET=change_this_secret_key_in_production_please_use_a_long_random_string
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
BACKEND_PUBLISH_PORT=22753
FILE_UPLOAD_DIR=/app/uploads/avatars

# Frontend
VITE_API_BASE_URL=/api
FRONTEND_PUBLISH_PORT=32753

# Timezone
TZ=Asia/Shanghai
```

> **安全提示**：生产环境部署前务必修改 `JWT_SECRET` 为足够长的随机字符串（建议 64+ 字符），`MYSQL_ROOT_PASSWORD` 也建议改为强密码。

#### 步骤 3：修复 `docker-compose.yml` 默认值兜底（双保险）

即使 `.env` 缺失，也应确保 compose 文件能通过默认值正常启动。在 [docker-compose.yml](file:///d:/Desktop/%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9%20(2)/label-2753/2753/docker-compose.yml) 中为以下 5 个变量添加 `:-默认值`：

```diff
 services:
   db:
     environment:
-      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
+      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}
       ...
     healthcheck:
-      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
+      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-root}"]

   backend:
     environment:
-      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
+      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL:-jdbc:mysql://db:3306/user_management?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false}
-      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
+      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD:-root}
-      JWT_SECRET: ${JWT_SECRET}
+      JWT_SECRET: ${JWT_SECRET:-change_this_jwt_secret_in_production}
```

---

### 验证结果

执行以下命令验证 db 容器已完全恢复：

```bash
# 单独启动 db 服务（依赖最小）
docker compose up -d db
```

**1. 容器状态验证：**

```bash
$ docker ps -a --filter name=2753-db
CONTAINER ID   IMAGE       STATUS                    PORTS                                           NAMES
d78fff44a067   mysql:8.0   Up 42 seconds (healthy)   0.0.0.0:12753->3306/tcp, [::]:12753->3306/tcp   2753-db
```

✅ `STATUS` 显示 `(healthy)` — 健康检查通过  
✅ `PORTS` 显示 12753 → 3306 端口映射正常

**2. 数据库内容验证（通过容器内 mysql 客户端）：**

```bash
docker exec 2753-db mysql -uroot -proot -e "
  SHOW DATABASES;
  USE user_management;
  SHOW TABLES;
  SELECT id, username, nickname FROM sys_user;
"
```

预期输出：
- `user_management` 数据库存在
- 8 张业务表全部创建：`sys_user`、`sys_role`、`sys_permission`、`sys_user_role`、`sys_role_permission`、`sys_dept`、`sys_user_dept`、`sys_audit_log`
- `sys_user` 表包含 3 条种子数据：`admin`、`zhangsan`、`lisi`

**3. 端口连通性验证（宿主主机）：**

```bash
# Windows PowerShell
Test-NetConnection -ComputerName 127.0.0.1 -Port 12753
# 或使用 MySQL 客户端
mysql -h 127.0.0.1 -P 12753 -uroot -proot -e "SELECT 1;"
```

---

### 影响的文件

| 文件 | 变更类型 | 说明 |
|---|---|---|
| [.env](file:///d:/Desktop/%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9%20(2)/label-2753/2753/.env) | 新增 | 项目根目录环境变量配置（所有 compose 变量值） |
| [docker-compose.yml](file:///d:/Desktop/%E6%96%B0%E5%BB%BA%E6%96%87%E4%BB%B6%E5%A4%B9%20(2)/label-2753/2753/docker-compose.yml) | 修改 | 5 处环境变量添加 `:-默认值` 兜底 |

**操作记录（容器/数据卷）：**
- 删除旧容器 `2753-db`（Restarting 状态）
- 删除数据卷 `2753_mysql_data`（损坏的初始化目录）
- 重新创建并启动 `2753-db` 容器（全新数据卷 + 完整初始化）

---

### 常见问题排查清单

若后续再次遇到 MySQL 容器启动问题，按以下优先级排查：

| 序号 | 检查项 | 命令/方法 | 预期结果 |
|---|---|---|---|
| 1 | `.env` 文件是否存在 | `Test-Path .env`（PS）或 `ls .env` | 存在且可读 |
| 2 | `MYSQL_ROOT_PASSWORD` 是否已设置 | `docker compose config \| Select-String MYSQL_ROOT_PASSWORD` | 值非空 |
| 3 | 数据卷是否为全新空卷 | `docker volume inspect 2753_mysql_data` 后查看挂载目录 | 空或仅含初始化后内容 |
| 4 | 容器日志有无 ERROR | `docker logs 2753-db \| Select-String ERROR` | 无匹配 |
| 5 | 健康检查状态 | `docker inspect 2753-db \| Select-String Health` | 显示 `healthy` |
| 6 | init.sql 是否被执行 | 检查 `docker-entrypoint-initdb.d` 挂载 | 容器内 `/docker-entrypoint-initdb.d/init.sql` 存在 |

---

### 相关参考

- [MySQL Docker Official Image - Environment Variables](https://hub.docker.com/_/mysql#environment-variables)
- [Docker Compose - Variable Substitution（`${VAR:-default}` 语法）](https://docs.docker.com/compose/compose-file/12-interpolation/)
- [Docker Compose - Environment File (.env)](https://docs.docker.com/compose/environment-variables/set-environment-variables/)
- [MySQL 8.0 - Server Command Options](https://dev.mysql.com/doc/refman/8.0/en/server-options.html)

