package com.petrecovery.module.adoption.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("adoption_request")
public class AdoptionRequest {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long archiveId;
    private Long applicantId;
    private Long ownerId;
    private String message;
    private String contact;
    private String livingCondition;
    private String petExperience;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
