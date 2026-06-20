package com.example.usermanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditLogSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initAuditLogTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_audit_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
                    user_id BIGINT COMMENT '操作人ID',
                    username VARCHAR(50) COMMENT '操作人用户名',
                    nickname VARCHAR(50) COMMENT '操作人昵称',
                    operation VARCHAR(50) NOT NULL COMMENT '操作类型',
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作审计日志表'
                """);
            log.info("审计日志表 sys_audit_log 检查/创建完成");

            jdbcTemplate.update("""
                INSERT IGNORE INTO sys_permission (name, code, type, description)
                VALUES ('查看审计日志', 'audit:list', 'BUTTON', '查看操作审计日志权限')
                """);
            log.info("审计日志权限 audit:list 检查/插入完成");

            jdbcTemplate.update("""
                INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
                SELECT r.id, p.id FROM sys_role r, sys_permission p
                WHERE r.code = 'ADMIN' AND p.code = 'audit:list'
                """);
            log.info("ADMIN 角色的审计日志权限关联检查/插入完成");
        } catch (Exception e) {
            log.error("审计日志模块数据库初始化失败: {}", e.getMessage(), e);
        }
    }
}
