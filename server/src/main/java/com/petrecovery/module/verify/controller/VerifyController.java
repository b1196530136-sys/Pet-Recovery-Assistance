package com.petrecovery.module.verify.controller;

import com.petrecovery.common.Result;
import com.petrecovery.module.verify.service.VerifyCodeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/verify")
public class VerifyController {

    private final VerifyCodeService verifyCodeService;

    public VerifyController(VerifyCodeService verifyCodeService) {
        this.verifyCodeService = verifyCodeService;
    }

    @PostMapping("/send-code")
    public Result<?> sendCode(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = body.get("email");
        if (email == null || !email.matches("^\\S+@\\S+\\.\\S+$")) {
            return Result.error("邮箱格式不正确");
        }
        try {
            String code = verifyCodeService.sendCode(email, getClientIp(request));
            if (code != null) {
                // 邮件发送失败，返回验证码供开发调试
                return Result.success("验证码发送失败，开发模式验证码：" + code);
            }
            return Result.success("验证码已发送至邮箱");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
