package com.example.usermanager.dto;

import lombok.Data;

@Data
public class ImportErrorItem {

    private Integer rowNum;

    private String username;

    private String errorMessage;

    public ImportErrorItem(Integer rowNum, String username, String errorMessage) {
        this.rowNum = rowNum;
        this.username = username;
        this.errorMessage = errorMessage;
    }
}
