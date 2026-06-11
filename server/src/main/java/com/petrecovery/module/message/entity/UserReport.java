package com.petrecovery.module.message.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_report")
public class UserReport {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reporterId;
    private Long reportedUserId;
    private Long messageId;
    private String reportType;
    private String reason;
    private String detail;
    private String messageSnapshot;
    private String status;
    private String handleNote;
    private Long handledBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
