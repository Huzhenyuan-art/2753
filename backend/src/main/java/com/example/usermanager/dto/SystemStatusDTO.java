package com.example.usermanager.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SystemStatusDTO {
    private String status;
    private String applicationName;
    private String version;
    private LocalDateTime startTime;
    private long uptimeSeconds;
    private DatabaseStatusDTO database;
    private OnlineUsersDTO onlineUsers;
    private MemoryStatusDTO memory;
    private LocalDateTime timestamp;
}
