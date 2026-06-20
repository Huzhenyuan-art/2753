package com.example.usermanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_audit_log")
public class AuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private String nickname;

    private String operation;

    private String module;

    private String description;

    private String method;

    private String params;

    private String result;

    private String ip;

    private Integer status;

    private String errorMsg;

    private Long costTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
