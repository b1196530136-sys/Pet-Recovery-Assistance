package com.petrecovery.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.user.entity.SysUser;

public interface UserService extends IService<SysUser> {
    SysUser login(String email, String password);

    SysUser register(SysUser user);

    SysUser loginByCode(String email);

    boolean applyCertification(Long userId, String credentials);

    void updateAvatar(Long userId, String avatar);
    void resetPassword(String email, String newPassword);
}
