package com.example.usermanager.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanager.annotation.AuditLog;
import com.example.usermanager.common.Result;
import com.example.usermanager.entity.User;
import com.example.usermanager.service.AuditLogService;
import com.example.usermanager.service.UserService;
import com.example.usermanager.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
public class AuditLogAspect {

    @Autowired
    @Lazy
    private AuditLogService auditLogService;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    @Lazy
    private JwtUtils jwtUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(auditLogAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLogAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();

        com.example.usermanager.entity.AuditLog auditLog = new com.example.usermanager.entity.AuditLog();
        auditLog.setOperation(auditLogAnnotation.operation());
        auditLog.setModule(auditLogAnnotation.module());
        auditLog.setDescription(auditLogAnnotation.description());
        auditLog.setMethod(joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName());

        HttpServletRequest request = getRequest();
        if (request != null) {
            auditLog.setIp(getClientIp(request));

            String token = jwtUtils.extractTokenFromRequest(request);
            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                String username = jwtUtils.getUsernameFromToken(token);
                Long userId = jwtUtils.getUserIdFromToken(token);
                User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
                auditLog.setUserId(userId);
                auditLog.setUsername(username);
                if (user != null) {
                    auditLog.setNickname(user.getNickname());
                }
            }
        }

        if (auditLog.getUsername() == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
                String username = authentication.getName();
                auditLog.setUsername(username);
                User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
                if (user != null) {
                    auditLog.setUserId(user.getId());
                    auditLog.setNickname(user.getNickname());
                }
            }
        }

        if ("LOGIN".equals(auditLogAnnotation.operation()) && auditLog.getUsername() == null) {
            try {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                String[] paramNames = signature.getParameterNames();
                Object[] args = joinPoint.getArgs();
                if (paramNames != null && args != null) {
                    for (int i = 0; i < paramNames.length; i++) {
                        if ("loginData".equals(paramNames[i]) && args[i] instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, String> map = (java.util.Map<String, String>) args[i];
                            String username = map.get("username");
                            if (StringUtils.hasText(username)) {
                                auditLog.setUsername(username);
                            }
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (auditLogAnnotation.recordParams()) {
            try {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                String[] paramNames = signature.getParameterNames();
                Object[] args = joinPoint.getArgs();
                if (paramNames != null && args != null) {
                    StringBuilder paramsBuilder = new StringBuilder();
                    for (int i = 0; i < paramNames.length; i++) {
                        if (args[i] instanceof HttpServletRequest) continue;
                        paramsBuilder.append(paramNames[i]).append("=");
                        try {
                            paramsBuilder.append(objectMapper.writeValueAsString(args[i]));
                        } catch (Exception e) {
                            paramsBuilder.append(String.valueOf(args[i]));
                        }
                        if (i < paramNames.length - 1) {
                            paramsBuilder.append(", ");
                        }
                    }
                    String params = paramsBuilder.toString();
                    if (params.length() > 2000) {
                        params = params.substring(0, 2000) + "...";
                    }
                    auditLog.setParams(params);
                }
            } catch (Exception e) {
                auditLog.setParams("参数解析失败");
            }
        }

        Object result = null;
        try {
            result = joinPoint.proceed();

            if (result instanceof Result<?> res) {
                auditLog.setStatus(res.getCode() != null && res.getCode() == 200 ? 1 : 0);
                if (res.getCode() == null || res.getCode() != 200) {
                    String errorMsg = res.getMessage();
                    if (errorMsg != null && errorMsg.length() > 1000) {
                        errorMsg = errorMsg.substring(0, 1000);
                    }
                    auditLog.setErrorMsg(errorMsg);
                }
            } else {
                auditLog.setStatus(1);
            }

            if (auditLogAnnotation.recordResult() && result != null) {
                try {
                    String resultStr = objectMapper.writeValueAsString(result);
                    if (resultStr.length() > 2000) {
                        resultStr = resultStr.substring(0, 2000) + "...";
                    }
                    auditLog.setResult(resultStr);
                } catch (Exception e) {
                    auditLog.setResult("结果解析失败");
                }
            }

            if ("LOGIN".equals(auditLogAnnotation.operation()) && result != null) {
                try {
                    Method getMethod = result.getClass().getMethod("getData");
                    Object data = getMethod.invoke(result);
                    if (data != null) {
                        Method getUsername = data.getClass().getMethod("getUsername");
                        Method getUserId = data.getClass().getMethod("getUserId");
                        Method getNickname = data.getClass().getMethod("getNickname");
                        Object username = getUsername.invoke(data);
                        Object userId = getUserId.invoke(data);
                        Object nickname = getNickname.invoke(data);
                        if (username != null) auditLog.setUsername(username.toString());
                        if (userId != null) auditLog.setUserId(Long.valueOf(userId.toString()));
                        if (nickname != null) auditLog.setNickname(nickname.toString());
                    }
                } catch (Exception e) {
                }
            }

            return result;
        } catch (Throwable e) {
            auditLog.setStatus(0);
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 1000) {
                errorMsg = errorMsg.substring(0, 1000);
            }
            auditLog.setErrorMsg(errorMsg);
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            auditLog.setCostTime(costTime);
            auditLog.setCreateTime(LocalDateTime.now());
            try {
                auditLogService.saveLog(auditLog);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}
