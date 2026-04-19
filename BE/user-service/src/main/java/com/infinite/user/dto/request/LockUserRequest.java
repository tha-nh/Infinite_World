package com.infinite.user.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LockUserRequest {
    private Long userId;
    private LocalDateTime lockTime; // null = khóa vĩnh viễn
    private String nguoithuchien;
}