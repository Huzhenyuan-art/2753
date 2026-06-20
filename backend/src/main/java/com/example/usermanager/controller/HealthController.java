package com.example.usermanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanager.common.Result;
import com.example.usermanager.dto.DatabaseStatusDTO;
import com.example.usermanager.dto.MemoryStatusDTO;
import com.example.usermanager.dto.OnlineUsersDTO;
import com.example.usermanager.dto.SystemStatusDTO;
import com.example.usermanager.entity.User;
import com.example.usermanager.service.OnlineUserService;
import com.example.usermanager.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OnlineUserService onlineUserService;

    @Autowired
    private UserService userService;

    @Value("${spring.application.name:user-manager}")
    private String applicationName;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    private static final LocalDateTime startTime = LocalDateTime.now();

    @GetMapping("/actuator/health")
    public Map<String, Object> actuatorHealth() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        return data;
    }

    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        return Result.success(data);
    }

    @GetMapping("/api/health/detail")
    public Result<SystemStatusDTO> healthDetail() {
        SystemStatusDTO status = new SystemStatusDTO();
        status.setTimestamp(LocalDateTime.now());
        status.setApplicationName(applicationName);
        status.setVersion(appVersion);
        status.setStartTime(startTime);
        status.setUptimeSeconds(java.time.Duration.between(startTime, LocalDateTime.now()).getSeconds());

        boolean allUp = true;

        DatabaseStatusDTO dbStatus = checkDatabase();
        status.setDatabase(dbStatus);
        if (!"UP".equals(dbStatus.getStatus())) {
            allUp = false;
        }

        MemoryStatusDTO memoryStatus = getMemoryStatus();
        status.setMemory(memoryStatus);

        OnlineUsersDTO onlineUsers = getOnlineUsersStatus();
        status.setOnlineUsers(onlineUsers);

        status.setStatus(allUp ? "UP" : "DEGRADED");

        return Result.success(status);
    }

    private DatabaseStatusDTO checkDatabase() {
        DatabaseStatusDTO dto = new DatabaseStatusDTO();
        dto.setType("MySQL");
        long start = System.currentTimeMillis();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dto.setStatus("UP");
            dto.setResponseTimeMs(System.currentTimeMillis() - start);
        } catch (Exception e) {
            dto.setStatus("DOWN");
            dto.setResponseTimeMs(System.currentTimeMillis() - start);
            dto.setError(e.getMessage());
        }
        return dto;
    }

    private MemoryStatusDTO getMemoryStatus() {
        MemoryStatusDTO dto = new MemoryStatusDTO();
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        dto.setTotalMemoryMB(totalMemory / (1024 * 1024));
        dto.setUsedMemoryMB(usedMemory / (1024 * 1024));
        dto.setFreeMemoryMB(freeMemory / (1024 * 1024));
        dto.setUsagePercent(Math.round((double) usedMemory / totalMemory * 10000) / 100.0);

        return dto;
    }

    private OnlineUsersDTO getOnlineUsersStatus() {
        OnlineUsersDTO dto = new OnlineUsersDTO();
        dto.setCount(onlineUserService.getOnlineUserCount());

        long totalUsers = userService.count();
        dto.setTotalUsers((int) totalUsers);

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long activeToday = userService.count(new LambdaQueryWrapper<User>()
                .ge(User::getUpdateTime, todayStart));
        dto.setActiveUsersToday((int) activeToday);

        return dto;
    }
}
