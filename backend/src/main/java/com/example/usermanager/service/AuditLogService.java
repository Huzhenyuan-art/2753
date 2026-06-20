package com.example.usermanager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.usermanager.entity.AuditLog;

import java.time.LocalDateTime;

public interface AuditLogService extends IService<AuditLog> {

    void saveLog(AuditLog auditLog);

    Page<AuditLog> getLogPage(Integer pageNum, Integer pageSize, String username, String operation,
                               LocalDateTime startTime, LocalDateTime endTime);
}
