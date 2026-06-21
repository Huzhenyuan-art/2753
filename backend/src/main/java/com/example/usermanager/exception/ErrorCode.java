package com.example.usermanager.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND(10001, "用户名不存在"),
    PASSWORD_ERROR(10002, "密码错误"),
    ACCOUNT_DISABLED(10003, "账号已被禁用，请联系管理员"),
    OLD_PASSWORD_ERROR(10004, "旧密码错误"),
    SAME_AS_OLD_PASSWORD(10005, "新密码不能与旧密码相同"),
    CONFIRM_PASSWORD_MISMATCH(10006, "确认密码与新密码不一致"),
    WEAK_PASSWORD(10007, "密码过于常见，请选择更复杂的密码"),
    INSUFFICIENT_COMPLEXITY(10008, "密码复杂度不足，需包含字母、数字、特殊字符中的至少两种"),

    REFRESH_TOKEN_MISSING(10010, "刷新令牌缺失"),
    REFRESH_TOKEN_INVALID(10011, "刷新令牌无效或已过期，请重新登录"),
    REFRESH_TOKEN_TYPE_ERROR(10012, "令牌类型错误，请使用刷新令牌"),
    REFRESH_USER_NOT_FOUND(10013, "用户不存在，请重新登录"),
    REFRESH_ACCOUNT_DISABLED(10014, "账号已被禁用，请联系管理员"),
    PASSWORD_CHANGED(10015, "密码已修改，请重新登录"),
    REFRESH_FAILED(10016, "刷新令牌失败，请重新登录"),

    LOGIN_FAILED(401, "登录失败，请重试");

    private final int code;
    private final String message;
}
