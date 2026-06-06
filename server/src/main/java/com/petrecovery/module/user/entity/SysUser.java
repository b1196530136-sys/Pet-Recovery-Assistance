package com.petrecovery.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String email;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String role;
    private String certCredentials;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private Integer status;

    private Integer loginFailCount;

    private LocalDateTime lockTime;
}
