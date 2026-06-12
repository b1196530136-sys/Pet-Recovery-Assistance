package com.petrecovery.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.user.entity.SysUser;

import java.util.Map;

public interface UserService extends IService<SysUser> {
    SysUser login(String email, String password);

    SysUser register(SysUser user);

    SysUser loginByCode(String email);

    boolean isEmailRegistered(String email);

    boolean applyCertification(Long userId, String credentials);

    void updateAvatar(Long userId, String avatar);

    void updatePhone(Long userId, String phone);

    void resetPassword(String email, String newPassword);

    Map<String, Object> getPublicProfile(Long targetUserId, Long viewerUserId);
}
