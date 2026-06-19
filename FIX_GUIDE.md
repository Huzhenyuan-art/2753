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
