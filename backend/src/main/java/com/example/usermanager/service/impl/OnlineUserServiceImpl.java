package com.example.usermanager.service.impl;

import com.example.usermanager.service.OnlineUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    @Value("${jwt.access-expiration:3600000}")
    private long accessExpiration;

    private final ConcurrentMap<String, Long> onlineUsers = new ConcurrentHashMap<>();

    @Override
    public void recordUserActivity(String username) {
        onlineUsers.put(username, System.currentTimeMillis());
    }

    @Override
    public void removeUser(String username) {
        onlineUsers.remove(username);
    }

    @Override
    public int getOnlineUserCount() {
        cleanupExpiredUsers();
        return onlineUsers.size();
    }

    @Override
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredUsers() {
        long expirationTime = accessExpiration;
        long now = System.currentTimeMillis();
        onlineUsers.entrySet().removeIf(entry ->
                (now - entry.getValue()) > expirationTime);
    }
}
