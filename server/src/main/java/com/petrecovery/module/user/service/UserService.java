package com.petrecovery.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.user.entity.SysUser;

public interface UserService extends IService<SysUser> {
    SysUser login(String email, String password);
    SysUser register(SysUser user);
    SysUser loginByCode(String email, String code);
    boolean applyCertification(Long userId, String credentials);
}
