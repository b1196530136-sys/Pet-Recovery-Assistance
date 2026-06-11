package com.petrecovery.module.message.dto;

import lombok.Data;

@Data
public class BlockUserRequest {
    private Long blockedUserId;
    private String reason;
}
