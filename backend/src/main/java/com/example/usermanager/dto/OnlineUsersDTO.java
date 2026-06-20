package com.example.usermanager.dto;

import lombok.Data;

@Data
public class OnlineUsersDTO {
    private int count;
    private int totalUsers;
    private int activeUsersToday;
}
