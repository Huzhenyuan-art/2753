package com.example.usermanager.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.usermanager.common.Result;
import com.example.usermanager.entity.AuditLog;
import com.example.usermanager.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/list")
    public Result<Page<AuditLog>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) String operation,
                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                       @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<AuditLog> page = auditLogService.getLogPage(pageNum, pageSize, username, operation, startTime, endTime);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    public Result<AuditLog> getById(@PathVariable Long id) {
        AuditLog log = auditLogService.getById(id);
        if (log == null) {
            return Result.error(404, "日志不存在");
        }
        return Result.success(log);
    }
}
