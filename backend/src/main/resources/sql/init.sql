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
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Seed Data (Password is '123456' encrypted with BCrypt)
INSERT IGNORE INTO sys_user (username, password, nickname, email) VALUES 
('admin', '$2a$10$P1UK1iryZJXe9T5ZpfCHF.6BzLBokxBYzQfHx1P99d/snxM4KFETe', '管理员', 'admin@example.com'),
('zhangsan', '$2a$10$P1UK1iryZJXe9T5ZpfCHF.6BzLBokxBYzQfHx1P99d/snxM4KFETe', '张三', 'zhangsan@example.com'),
('lisi', '$2a$10$P1UK1iryZJXe9T5ZpfCHF.6BzLBokxBYzQfHx1P99d/snxM4KFETe', '李四', 'lisi@example.com');
