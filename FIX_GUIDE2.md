# Pinia 与 Vue 组合式 API 依赖冲突修复指南

## 问题描述

前端项目执行 `npm ci` 时因 Pinia 版本与 Vue 组合式 API 存在依赖冲突而安装失败，报 `ERESOLVE` 依赖解析错误。

## 冲突根因分析

### 1. 主要冲突：@vue/composition-api 与 Vue 3 不兼容

- **pinia 2.1.7** 的 `peerDependencies` 中包含 `@vue/composition-api: ^1.4.0`（标记为 optional）
- **@vue/composition-api** 是 Vue 2 的组合式 API polyfill，其自身 `peerDependencies` 要求 `vue: <2.7`
- 在 Vue 3 项目中，npm 严格的 peer 依赖解析器会沿着 `pinia → @vue/composition-api → vue<2.7` 的链路检测到冲突，触发 `ERESOLVE` 错误
- 即使是 `peerDependenciesMeta` 标记为 optional 的依赖，在 npm v7+ 严格解析模式下仍会导致安装失败

### 2. 次要问题：package.json 与 package-lock.json 版本不一致

- `package.json` 声明的是精确版本（`pinia: 2.1.7`、`vue: 3.4.21`）
- `package-lock.json` 中记录的版本声明带 `^` 范围符（如 `"pinia": "^2.1.7"`），实际解析到了更高版本
- `npm ci` 要求 `package.json` 与 `package-lock.json` 的版本声明必须一致，否则可能导致安装失败或依赖树不一致

### 3. 版本兼容矩阵

| 包 | 版本 | peer 依赖 vue 要求 | 是否兼容 Vue 3.4 | 是否兼容 Vue 3.5 | 是否依赖 @vue/composition-api |
|---|---|---|---|---|---|
| pinia | 2.1.7 | ^2.6.14 \|\| ^3.3.0 | ✅ | ✅ | ⚠️ 有（冲突根源） |
| pinia | 2.3.1 | ^2.7.0 \|\| ^3.5.11 | ❌ 版本不足 | ✅ | ✅ 已移除 |
| vue-router | 4.3.0 | ^3.2.0 | ✅ | ✅ | ❌ |
| vue-router | 4.6.4 | ^3.5.0 | ❌ 版本不足 | ✅ | ❌ |
| element-plus | 2.13.2 | ^3.3.0 | ✅ | ✅ | ❌ |

## 修复方案

### 方案选择

采用 **升级依赖版本** 的方案：
- 将 `pinia` 升级到 **2.3.1** —— 该版本已彻底移除 `@vue/composition-api` 依赖，从根源解决冲突
- 将 `vue` 升级到 **3.5.27** —— 满足 pinia 2.3.x 的 peer 依赖要求（`vue: ^3.5.11`）
- 将 `vue-router` 升级到 **4.6.4** —— 与 vue 3.5.x 兼容，保持与 lock 文件实际版本一致

### 修改的文件

#### 1. `frontend/package.json`

```diff
 "dependencies": {
-  "vue": "3.4.21",
-  "vue-router": "4.3.0",
-  "pinia": "2.1.7",
+  "vue": "3.5.27",
+  "vue-router": "4.6.4",
+  "pinia": "2.3.1",
   "axios": "1.6.7",
```

#### 2. `frontend/package-lock.json`

同步更新以下内容：
- 根目录版本声明：`pinia`、`vue`、`vue-router` 统一为精确版本号
- `node_modules/pinia`：版本从 2.1.7 → 2.3.1，移除 `@vue/composition-api` peer 依赖
- `node_modules/vue-router`：版本从 4.3.0 → 4.6.4，peer 依赖更新为 `vue: ^3.5.0`
- `node_modules/vue`：版本保持 3.5.27（lock 文件中已是此版本）

## 验证方式

### 1. 验证依赖安装

```bash
cd frontend

rm -rf node_modules
npm ci
```

预期结果：安装成功，无 `ERESOLVE` 错误。

### 2. 验证类型检查

```bash
npm run type-check
```

预期结果：TypeScript 类型检查通过，无 Pinia 相关类型错误。

### 3. 验证项目构建

```bash
npm run build
```

预期结果：Vite 构建成功，产物正常生成。

### 4. 验证开发启动

```bash
npm run dev
```

预期结果：开发服务器正常启动，Pinia store 功能正常。

## 备选方案（如无法升级 Vue）

如果项目因某些原因必须锁定在 Vue 3.4.x，可以使用以下替代方案：

```bash
npm install pinia@2.1.7 --legacy-peer-deps
```

或在项目根目录创建 `.npmrc` 文件：

```ini
legacy-peer-deps=true
```

> **注意**：`--legacy-peer-deps` 会跳过 peer 依赖严格检查，属于治标不治本的方案。推荐优先使用升级依赖的方案。

## 相关参考

- [Pinia 2.2 发布说明](https://github.com/vuejs/pinia/blob/main/packages/pinia/CHANGELOG.md)
- [npm peerDependencies 文档](https://docs.npmjs.com/cli/v10/configuring-npm/package-json#peerdependencies)
