package com.example.usermanager.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserExportDTO {

    @ExcelProperty("用户ID")
    private Long id;

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("昵称")
    private String nickname;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("状态")
    private String statusText;

    @ExcelProperty("所属部门")
    private String deptNames;

    @ExcelProperty("创建时间")
    private String createTime;

    @ExcelProperty("更新时间")
    private String updateTime;
}
