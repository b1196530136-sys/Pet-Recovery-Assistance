package com.petrecovery.module.adoption.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("adoption_record")
public class AdoptionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long requestId;
    private Long archiveId;
    private Long adopterId;
    private Long ownerId;
    private String contact;
    private String livingCondition;
    private String petExperience;
    private String followUpStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
