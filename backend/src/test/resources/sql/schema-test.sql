DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_permission;
DROP TABLE IF EXISTS sys_user_dept;
DROP TABLE IF EXISTS sys_audit_log;
DROP TABLE IF EXISTS sys_permission;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT IDENTITY PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    avatar VARCHAR(255),
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    password_changed_at TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(20) DEFAULT 'BUTTON',
    description VARCHAR(255),
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT IDENTITY PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    leader VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sys_user_dept (
    id BIGINT IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    UNIQUE (user_id, dept_id)
);

CREATE TABLE IF NOT EXISTS sys_audit_log (
    id BIGINT IDENTITY PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    nickname VARCHAR(50),
    operation VARCHAR(50) NOT NULL,
    module VARCHAR(50),
    description VARCHAR(500),
    method VARCHAR(200),
    params CLOB,
    result CLOB,
    ip VARCHAR(50),
    status SMALLINT DEFAULT 1,
    error_msg VARCHAR(1000),
    cost_time BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
