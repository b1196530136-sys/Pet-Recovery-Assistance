package com.petrecovery.module.user.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private String code;
}
