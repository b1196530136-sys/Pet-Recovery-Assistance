package com.petrecovery.module.verify.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_verify_code")
public class VerifyCode {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;
    private String code;
    private LocalDateTime expireAt;
    private Integer used;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
