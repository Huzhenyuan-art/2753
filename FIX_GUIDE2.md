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
