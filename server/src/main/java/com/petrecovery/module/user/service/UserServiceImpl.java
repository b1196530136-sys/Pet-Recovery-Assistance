package com.petrecovery.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.common.util.PhoneCryptoUtil;
import com.petrecovery.common.constant.UserRole;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final int MAX_FAIL_COUNT = 5;
    private static final int LOCK_MINUTES = 30;
    private static final String PHONE_PATTERN = "^\\+?\\d{7,20}$";

    private final PhoneCryptoUtil phoneCryptoUtil;

    public UserServiceImpl(PhoneCryptoUtil phoneCryptoUtil) {
        this.phoneCryptoUtil = phoneCryptoUtil;
    }

    @Override
    public SysUser login(String email, String password) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            throw new RuntimeException("邮箱或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被封禁");
        }
        // 检查是否被锁定
        if (user.getLockTime() != null && user.getLockTime().isAfter(LocalDateTime.now())) {
            long remainMinutes = java.time.Duration.between(LocalDateTime.now(), user.getLockTime()).toMinutes() + 1;
            throw new RuntimeException("账号已锁定，请" + remainMinutes + "分钟后再试");
        }
        // 锁定已过期，重置失败次数
        if (user.getLockTime() != null && user.getLockTime().isBefore(LocalDateTime.now())) {
            user.setLoginFailCount(0);
            user.setLockTime(null);
            updateById(user);
        }
        if (ENCODER.matches(password, user.getPassword())) {
            // 登录成功，重置失败次数
            if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
                user.setLoginFailCount(0);
                user.setLockTime(null);
                updateById(user);
            }
            return user;
        } else {
            // 登录失败，增加失败次数
            int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            user.setLoginFailCount(failCount);
            if (failCount >= MAX_FAIL_COUNT) {
                user.setLockTime(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                updateById(user);
                throw new RuntimeException("密码错误已达" + MAX_FAIL_COUNT + "次，账号锁定" + LOCK_MINUTES + "分钟");
            } else {
                updateById(user);
                throw new RuntimeException("邮箱或密码错误，还剩" + (MAX_FAIL_COUNT - failCount) + "次机会");
            }
        }
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
    public SysUser loginByCode(String email) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            throw new RuntimeException("邮箱未注册");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被封禁");
        }
        return user;
    }

    @Override
    public boolean isEmailRegistered(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return exists(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email.trim()));
    }

    @Override
    public boolean applyCertification(Long userId, String credentials) {
        SysUser user = getById(userId);
        if (user != null && UserRole.ROLE_USER.equals(user.getRole())) {
            user.setRole(UserRole.ROLE_PENDING_CERT);
            user.setCertCredentials(credentials);
            return updateById(user);
        }
        return false;
    }

    @Override
    public void updateAvatar(Long userId, String avatar) {
        SysUser user = getById(userId);
        if (user != null) {
            user.setAvatar(avatar);
            updateById(user);
        }
    }

    @Override
    public void updatePhone(Long userId, String phone) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String normalized = phoneCryptoUtil.normalize(phone);
        if (normalized != null && !normalized.matches(PHONE_PATTERN)) {
            throw new RuntimeException("联系电话格式不正确");
        }
        user.setPhone(normalized == null ? null : phoneCryptoUtil.encrypt(normalized));
        updateById(user);
    }

    @Override
    public void resetPassword(String email, String newPassword) {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            throw new RuntimeException("该邮箱未注册");
        }
        user.setPassword(ENCODER.encode(newPassword));
        updateById(user);
    }
}
