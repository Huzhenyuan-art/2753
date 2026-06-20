package com.example.usermanager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.usermanager.entity.AuditLog;
import com.example.usermanager.mapper.AuditLogMapper;
import com.example.usermanager.service.AuditLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AuditLogServiceImpl extends ServiceImpl<AuditLogMapper, AuditLog> implements AuditLogService {

    @Override
    @Async
    public void saveLog(AuditLog auditLog) {
        this.save(auditLog);
    }

    @Override
    public Page<AuditLog> getLogPage(Integer pageNum, Integer pageSize, String username, String operation,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        Page<AuditLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(username)) {
            wrapper.like(AuditLog::getUsername, username).or().like(AuditLog::getNickname, username);
        }
        if (StringUtils.hasText(operation)) {
            wrapper.eq(AuditLog::getOperation, operation);
        }
        if (startTime != null) {
            wrapper.ge(AuditLog::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AuditLog::getCreateTime, endTime);
        }

        wrapper.orderByDesc(AuditLog::getCreateTime);
        return this.page(page, wrapper);
    }
}
