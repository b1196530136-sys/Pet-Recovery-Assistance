package com.petrecovery.module.user.controller;

import com.petrecovery.common.Result;
import com.petrecovery.config.JwtConfig;
import com.petrecovery.module.user.dto.LoginRequest;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtConfig jwtConfig;

    public UserController(UserService userService, JwtConfig jwtConfig) {
        this.userService = userService;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody SysUser user) {
        userService.register(user);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody LoginRequest request) {
        SysUser user = userService.login(request.getEmail(), request.getPassword());
        if (user == null) {
            return Result.error("邮箱或密码错误");
        }
        String token = jwtConfig.generateToken(user.getId(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", toSafeMap(user));
        return Result.success(data);
    }

    @PostMapping("/login/code")
    public Result<?> loginByCode(@RequestBody LoginRequest request) {
        SysUser user = userService.loginByCode(request.getEmail(), request.getCode());
        String token = jwtConfig.generateToken(user.getId(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", toSafeMap(user));
        return Result.success(data);
    }

    @GetMapping("/profile")
    public Result<?> profile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = userService.getById(userId);
        return Result.success(toSafeMap(user));
    }

    @GetMapping("/info/{id}")
    public Result<?> info(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(toSafeMap(user));
    }

    @PostMapping("/apply-certification")
    public Result<?> applyCertification(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        userService.applyCertification(userId, body.get("credentials"));
        return Result.success();
    }

    private Map<String, Object> toSafeMap(SysUser user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        map.put("phone", user.getPhone());
        map.put("role", user.getRole());
        map.put("status", user.getStatus());
        map.put("createTime", user.getCreateTime());
        return map;
    }
}
