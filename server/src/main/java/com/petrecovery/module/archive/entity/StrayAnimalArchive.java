package com.petrecovery.module.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("stray_animal_archive")
public class StrayAnimalArchive {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String animalType;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String address;
    private String healthStatus;
    private String neuteredStatus;
    private String placementStatus;
    private String photos;
    private String description;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String rejectReason;
}
