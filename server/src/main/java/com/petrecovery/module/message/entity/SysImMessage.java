package com.petrecovery.module.message.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_im_message")
public class SysImMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long senderId;
    private Long receiverId;
    private Long postId;
    private Integer msgType;
    private String content;
    private String clueTime;
    private BigDecimal clueLongitude;
    private BigDecimal clueLatitude;
    private String clueAddress;
    private String cluePhotos;
    private Integer readStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
