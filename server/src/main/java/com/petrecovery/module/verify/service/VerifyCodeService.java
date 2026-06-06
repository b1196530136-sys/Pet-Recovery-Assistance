package com.petrecovery.module.verify.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.module.verify.entity.VerifyCode;
import com.petrecovery.module.verify.mapper.VerifyCodeMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class VerifyCodeService extends ServiceImpl<VerifyCodeMapper, VerifyCode> {

    private final JavaMailSender mailSender;
    private final Random random = new Random();

    public VerifyCodeService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public String sendCode(String email) {
        // 1分钟内不能重复发送
        VerifyCode last = getOne(new LambdaQueryWrapper<VerifyCode>()
                .eq(VerifyCode::getEmail, email)
                .orderByDesc(VerifyCode::getCreateTime)
                .last("LIMIT 1"));
        if (last != null && last.getCreateTime().plusMinutes(1).isAfter(LocalDateTime.now())) {
            throw new RuntimeException("请60秒后再发送");
        }

        // 生成6位随机码
        String code = String.format("%06d", random.nextInt(999999));

        // 存储到数据库
        VerifyCode vc = new VerifyCode();
        vc.setEmail(email);
        vc.setCode(code);
        vc.setExpireAt(LocalDateTime.now().plusMinutes(5));
        vc.setUsed(0);
        save(vc);

        // 发送邮件（失败则降级为控制台输出）
        boolean mailSent = true;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom("b1196530136@163.com");
            msg.setTo(email);
            msg.setSubject("寻宠互助平台 - 邮箱验证码");
            msg.setText("您的验证码是：" + code + "，有效期5分钟。如非本人操作，请忽略。");
            mailSender.send(msg);
        } catch (Exception e) {
            mailSent = false;
            System.out.println("========== 邮件发送失败（开发模式） ==========");
            System.out.println("邮箱: " + email + "，验证码: " + code);
            System.out.println("错误: " + e.getMessage());
            System.out.println("=============================================");
        }

        // 邮件未发出时返回验证码，方便开发调试
        if (mailSent) {
            return null;
        } else {
            return code;
        }
    }

    public boolean verifyCode(String email, String code) {
        VerifyCode vc = getOne(new LambdaQueryWrapper<VerifyCode>()
                .eq(VerifyCode::getEmail, email)
                .eq(VerifyCode::getCode, code)
                .eq(VerifyCode::getUsed, 0)
                .gt(VerifyCode::getExpireAt, LocalDateTime.now())
                .orderByDesc(VerifyCode::getCreateTime)
                .last("LIMIT 1"));
        if (vc != null) {
            vc.setUsed(1);
            updateById(vc);
            return true;
        }
        return false;
    }
}
