package com.petrecovery.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.common.constant.UserRole;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    @Override
    public SysUser login(String email, String password) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user != null && ENCODER.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public SysUser register(SysUser user) {
        user.setPassword(ENCODER.encode(user.getPassword()));
        user.setRole(UserRole.ROLE_USER);
        user.setStatus(1);
        save(user);
        return user;
    }

    @Override
    public SysUser loginByCode(String email, String code) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            user = new SysUser();
            user.setEmail(email);
            user.setNickname(email.split("@")[0]);
            user.setRole(UserRole.ROLE_USER);
            user.setStatus(1);
            save(user);
        }
        return user;
    }

    @Override
    public boolean applyCertification(Long userId, String credentials) {
        SysUser user = getById(userId);
        if (user != null && UserRole.ROLE_USER.equals(user.getRole())) {
            user.setRole(UserRole.ROLE_PENDING_CERT);
            return updateById(user);
        }
        return false;
    }
}
