-- Database Initialization Script
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS user_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_management;

-- User Table
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    avatar VARCHAR(255) COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    password_changed_at DATETIME COMMENT '密码最后修改时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Role Table
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '角色编码',
    description VARCHAR(255) COMMENT '角色描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- Permission Table
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '权限名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    type VARCHAR(20) DEFAULT 'BUTTON' COMMENT '权限类型：MENU-菜单，BUTTON-按钮',
    description VARCHAR(255) COMMENT '权限描述',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- User-Role Association Table
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- Role-Permission Association Table
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- Seed Data (Password is '123456' encrypted with BCrypt)
INSERT IGNORE INTO sys_user (username, password, nickname, email) VALUES 
('admin', '$2a$10$P1UK1iryZJXe9T5ZpfCHF.6BzLBokxBYzQfHx1P99d/snxM4KFETe', '管理员', 'admin@example.com'),
('zhangsan', '$2a$10$P1UK1iryZJXe9T5ZpfCHF.6BzLBokxBYzQfHx1P99d/snxM4KFETe', '张三', 'zhangsan@example.com'),
('lisi', '$2a$10$P1UK1iryZJXe9T5ZpfCHF.6BzLBokxBYzQfHx1P99d/snxM4KFETe', '李四', 'lisi@example.com');

-- Seed Role Data
INSERT IGNORE INTO sys_role (name, code, description) VALUES 
('超级管理员', 'ADMIN', '拥有所有权限'),
('编辑者', 'EDITOR', '可查看、新增、编辑用户'),
('浏览者', 'VIEWER', '仅可查看用户列表');

-- Seed Permission Data
INSERT IGNORE INTO sys_permission (name, code, type, description) VALUES 
('查看用户列表', 'user:list', 'BUTTON', '查看用户列表权限'),
('新增用户', 'user:add', 'BUTTON', '新增用户权限'),
('编辑用户', 'user:edit', 'BUTTON', '编辑用户权限'),
('删除用户', 'user:delete', 'BUTTON', '删除用户权限'),
('切换用户状态', 'user:status', 'BUTTON', '启用/禁用用户权限'),
('查看审计日志', 'audit:list', 'BUTTON', '查看操作审计日志权限'),
('查看部门列表', 'dept:list', 'BUTTON', '查看部门列表权限'),
('新增部门', 'dept:add', 'BUTTON', '新增部门权限'),
('编辑部门', 'dept:edit', 'BUTTON', '编辑部门权限'),
('删除部门', 'dept:delete', 'BUTTON', '删除部门权限');

-- Seed Role-Permission Association
-- ADMIN 拥有所有权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p WHERE r.code = 'ADMIN';

-- EDITOR 拥有 list, add, edit, status
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'EDITOR' AND p.code IN ('user:list', 'user:add', 'user:edit', 'user:status');

-- VIEWER 仅拥有 list
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r, sys_permission p
WHERE r.code = 'VIEWER' AND p.code IN ('user:list');

-- Seed User-Role Association
-- admin -> ADMIN
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'admin' AND r.code = 'ADMIN';

-- zhangsan -> EDITOR
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'zhangsan' AND r.code = 'EDITOR';

-- lisi -> VIEWER
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u, sys_role r WHERE u.username = 'lisi' AND r.code = 'VIEWER';

-- Department Table
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '部门名称',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '部门编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    leader VARCHAR(50) COMMENT '负责人',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- User-Department Association Table
CREATE TABLE IF NOT EXISTS sys_user_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    UNIQUE KEY uk_user_dept (user_id, dept_id),
    INDEX idx_user_id (user_id),
    INDEX idx_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户部门关联表';

-- Audit Log Table
CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT COMMENT '操作人ID',
    username VARCHAR(50) COMMENT '操作人用户名',
    nickname VARCHAR(50) COMMENT '操作人昵称',
    operation VARCHAR(50) NOT NULL COMMENT '操作类型：LOGIN-登录，CREATE-新增，UPDATE-编辑，DELETE-删除，STATUS-状态变更，ASSIGN_ROLE-分配角色，ASSIGN_DEPT-分配部门',
    module VARCHAR(50) COMMENT '操作模块',
    description VARCHAR(500) COMMENT '操作描述',
    method VARCHAR(200) COMMENT '请求方法',
    params TEXT COMMENT '请求参数',
    result TEXT COMMENT '返回结果',
    ip VARCHAR(50) COMMENT 'IP地址',
    status TINYINT DEFAULT 1 COMMENT '操作状态：1-成功，0-失败',
    error_msg VARCHAR(1000) COMMENT '错误信息',
    cost_time BIGINT COMMENT '耗时（毫秒）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_operation (operation),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表';

-- Seed Department Data
INSERT IGNORE INTO sys_dept (name, code, parent_id, sort_order, leader, phone, email) VALUES 
('总公司', 'HQ', 0, 1, '张总', '010-88888888', 'hq@company.com'),
('技术部', 'TECH', 1, 1, '李技术', '010-88888801', 'tech@company.com'),
('市场部', 'MARKET', 1, 2, '王市场', '010-88888802', 'market@company.com'),
('人事部', 'HR', 1, 3, '赵人事', '010-88888803', 'hr@company.com'),
('财务部', 'FINANCE', 1, 4, '孙财务', '010-88888804', 'finance@company.com'),
('前端开发组', 'TECH-FE', 2, 1, '周前端', '010-88888811', 'tech-fe@company.com'),
('后端开发组', 'TECH-BE', 2, 2, '吴后端', '010-88888812', 'tech-be@company.com'),
('测试组', 'TECH-QA', 2, 3, '郑测试', '010-88888813', 'tech-qa@company.com'),
('运营组', 'MARKET-OP', 3, 1, '冯运营', '010-88888821', 'market-op@company.com'),
('销售组', 'MARKET-SALE', 3, 2, '陈销售', '010-88888822', 'market-sale@company.com');

-- Seed User-Department Association
INSERT IGNORE INTO sys_user_dept (user_id, dept_id)
SELECT u.id, 2 FROM sys_user u WHERE u.username = 'admin';

INSERT IGNORE INTO sys_user_dept (user_id, dept_id)
SELECT u.id, 6 FROM sys_user u WHERE u.username = 'zhangsan';

INSERT IGNORE INTO sys_user_dept (user_id, dept_id)
SELECT u.id, 9 FROM sys_user u WHERE u.username = 'lisi';
