package com.example.usermanager.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserExcelDTO {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("密码")
    private String password;

    @ExcelProperty("昵称")
    private String nickname;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("状态(0=禁用,1=启用)")
    private String statusText;
}
