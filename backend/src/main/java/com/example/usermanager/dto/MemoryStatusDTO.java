package com.example.usermanager.dto;

import lombok.Data;

@Data
public class MemoryStatusDTO {
    private long totalMemoryMB;
    private long usedMemoryMB;
    private long freeMemoryMB;
    private double usagePercent;
}
