package com.example.usermanager.service;

public interface OnlineUserService {
    void recordUserActivity(String username);
    void removeUser(String username);
    int getOnlineUserCount();
    void cleanupExpiredUsers();
}
