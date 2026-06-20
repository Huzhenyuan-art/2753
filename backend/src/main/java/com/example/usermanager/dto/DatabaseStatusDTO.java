package com.example.usermanager.dto;

import lombok.Data;

@Data
public class DatabaseStatusDTO {
    private String status;
    private String type;
    private long responseTimeMs;
    private String error;
}
