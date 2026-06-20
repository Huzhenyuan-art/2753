package com.example.usermanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少为6位")
    private String password;
    
    @NotBlank(message = "昵称不能为空")
    private String nickname;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private String avatar;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime passwordChangedAt;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private List<com.example.usermanager.entity.Dept> depts;

    @TableField(exist = false)
    private List<Long> deptIds;
}
