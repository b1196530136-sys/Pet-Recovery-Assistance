package com.petrecovery.module.post.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pet_search_post")
public class PetSearchPost {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String petType;
    private String breed;
    private String petName;
    private String photos;
    private LocalDateTime lostTime;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private String reward;
    private String description;
    private String status;
    private String photoHash;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String rejectReason;
}
