package com.petrecovery.module.post.dto;

import lombok.Data;

@Data
public class PostSearchRequest {
    private String petType;
    private String province;
    private String city;
    private String district;
    private String status;
    private String keyword;
    private int page = 1;
    private int size = 20;
}
