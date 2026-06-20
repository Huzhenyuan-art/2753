package com.example.usermanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_user_dept")
public class UserDept {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long deptId;
}
