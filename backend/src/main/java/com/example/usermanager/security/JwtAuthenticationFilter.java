package com.example.usermanager.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.usermanager.entity.User;
import com.example.usermanager.service.OnlineUserService;
import com.example.usermanager.service.UserService;
import com.example.usermanager.util.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    @Lazy
    private UserService userService;

    @Autowired
    @Lazy
    private OnlineUserService onlineUserService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("/actuator/health".equals(path) || "/api/health".equals(path) || "/api/user/login".equals(path) || "/api/user/refresh".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtUtils.extractTokenFromRequest(request);

        if (token != null && jwtUtils.validateToken(token)) {
            if (!jwtUtils.isAccessToken(token)) {
                writeAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "令牌类型错误，请使用访问令牌");
                return;
            }

            String username = jwtUtils.getUsernameFromToken(token);
            User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (user == null) {
                writeAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "用户不存在，请重新登录");
                return;
            }
            if (user.getStatus() != null && user.getStatus() == 0) {
                writeAuthError(response, HttpServletResponse.SC_FORBIDDEN, 403, "账号已被禁用，请联系管理员");
                return;
            }
            if (user.getPasswordChangedAt() != null) {
                Claims claims = jwtUtils.parseClaims(token);
                if (claims.getIssuedAt() != null) {
                    LocalDateTime tokenIssuedAt = claims.getIssuedAt().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime();
                    if (tokenIssuedAt.isBefore(user.getPasswordChangedAt())) {
                        writeAuthError(response, HttpServletResponse.SC_UNAUTHORIZED, 401, "密码已修改，请重新登录");
                        return;
                    }
                }
            }

            List<String> roleCodes = jwtUtils.getRolesFromToken(token);
            List<String> permissionCodes = jwtUtils.getPermissionsFromToken(token);

            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.addAll(roleCodes.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList()));
            authorities.addAll(permissionCodes.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()));

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            onlineUserService.recordUserActivity(username);
        }

        filterChain.doFilter(request, response);
    }

    private void writeAuthError(HttpServletResponse response, int httpStatus, int code, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
