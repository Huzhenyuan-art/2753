package com.example.usermanager.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserImportResult {

    private int totalCount;

    private int successCount;

    private int failCount;

    private List<ImportErrorItem> errors;

    public UserImportResult(int totalCount, int successCount, int failCount, List<ImportErrorItem> errors) {
        this.totalCount = totalCount;
        this.successCount = successCount;
        this.failCount = failCount;
        this.errors = errors;
    }
}
