package com.petrecovery.module.user.controller;

import com.petrecovery.common.Result;
import com.petrecovery.common.util.PhoneCryptoUtil;
import com.petrecovery.config.JwtConfig;
import com.petrecovery.module.user.dto.LoginRequest;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.service.UserService;
import com.petrecovery.module.verify.service.VerifyCodeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtConfig jwtConfig;
    private final VerifyCodeService verifyCodeService;
    private final PhoneCryptoUtil phoneCryptoUtil;

    public UserController(UserService userService, JwtConfig jwtConfig, VerifyCodeService verifyCodeService,
                          PhoneCryptoUtil phoneCryptoUtil) {
        this.userService = userService;
        this.jwtConfig = jwtConfig;
        this.verifyCodeService = verifyCodeService;
        this.phoneCryptoUtil = phoneCryptoUtil;
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, Object> body) {
        String code = (String) body.get("code");
        String email = (String) body.get("email");
        if (code == null || !verifyCodeService.verifyCode(email, code)) {
            return Result.error("验证码错误或已过期");
        }
        SysUser user = new SysUser();
        user.setEmail(email);
        user.setNickname((String) body.get("nickname"));
        user.setPassword((String) body.get("password"));
        userService.register(user);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        try {
            SysUser user = userService.login(request.getEmail(), request.getPassword());
            String token = jwtConfig.generateToken(user.getId(), user.getRole());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", toPrivateMap(user));
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login/code")
    public Result<?> loginByCode(@RequestBody LoginRequest request) {
        if (request.getEmail() == null || request.getCode() == null) {
            return Result.error("参数不完整");
        }
        if (!userService.isEmailRegistered(request.getEmail())) {
            return Result.error("邮箱未注册");
        }
        if (!verifyCodeService.verifyCode(request.getEmail(), request.getCode())) {
            return Result.error("验证码错误或已过期");
        }
        try {
            SysUser user = userService.loginByCode(request.getEmail());
            String token = jwtConfig.generateToken(user.getId(), user.getRole());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", toPrivateMap(user));
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/profile")
    public Result<?> profile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = userService.getById(userId);
        return Result.success(toPrivateMap(user));
    }

    @GetMapping("/info/{id}")
    public Result<?> info(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(toPublicMap(user));
    }

    @PostMapping("/reset-password")
    public Result<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        String newPassword = body.get("newPassword");
        if (email == null || code == null || newPassword == null) {
            return Result.error("参数不完整");
        }
        if (!verifyCodeService.verifyCode(email, code)) {
            return Result.error("验证码错误或已过期");
        }
        userService.resetPassword(email, newPassword);
        return Result.success();
    }

    @PostMapping("/apply-certification")
    public Result<?> applyCertification(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        userService.applyCertification(userId, body.get("credentials"));
        return Result.success();
    }

    @PostMapping("/update-avatar")
    public Result<?> updateAvatar(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateAvatar(userId, body.get("avatar"));
        return Result.success();
    }

    @PostMapping("/update-phone")
    public Result<?> updatePhone(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updatePhone(userId, body.get("phone"));
        SysUser user = userService.getById(userId);
        return Result.success(toPrivateMap(user));
    }

    private Map<String, Object> toPublicMap(SysUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        map.put("role", user.getRole());
        map.put("createTime", user.getCreateTime());
        return map;
    }

    private Map<String, Object> toPrivateMap(SysUser user) {
        Map<String, Object> map = toPublicMap(user);
        map.put("email", user.getEmail());
        map.put("phone", phoneCryptoUtil.mask(user.getPhone()));
        map.put("hasPhone", phoneCryptoUtil.hasPhone(user.getPhone()));
        map.put("certCredentials", user.getCertCredentials());
        map.put("status", user.getStatus());
        return map;
    }
}
