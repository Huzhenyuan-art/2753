# 用户管理系统 (User Management System)

基于 SpringBoot 3.2.x + Vue 3 + Element Plus 的全栈用户管理系统。

## 🛠 技术栈
- **Frontend**: Vue 3 + Vite + Element Plus + Pinia + Axios
- **Backend**: SpringBoot 3.2.x + MyBatis-Plus + MySQL 8.0 + JWT + Maven
- **Infrastructure**: Docker + Nginx

## 🚀 启动指南 (How to Run)
1. 确保已安装 Docker 和 Docker Compose。
2. 在项目根目录下执行：
   ```bash
   docker compose up --build
   ```
3. 等待所有容器启动完成。

## 🔗 服务地址
- **前端页面**: [http://localhost:32753](http://localhost:32753)
- **后端接口**: [http://localhost:22753](http://localhost:22753)
- **数据库**: `localhost:12753` (root / root)

## 🧪 测试账号
- **管理员**: admin / 123456
- **测试用户**: zhangsan / 123456

## ✨ 功能亮点
- **审美现代**: 采用左右布局登录页，深色渐变背景，UI 交互流畅。
- **安全可靠**: JWT 认证，BCrypt 密码加密，统一异常处理。
- **容器化**: 一键启动，环境零依赖。
