package com.petrecovery.module.message.controller;

import com.petrecovery.common.Result;
import com.petrecovery.module.message.dto.BlockUserRequest;
import com.petrecovery.module.message.dto.ReportCreateRequest;
import com.petrecovery.module.message.entity.SysImMessage;
import com.petrecovery.module.message.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/im")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    public Result<?> send(@RequestBody SysImMessage message, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        message.setSenderId(userId);
        // msgType=1 为线索消息，否则为普通消息
        if (message.getMsgType() != null && message.getMsgType() == 1) {
            return Result.success(messageService.sendClueMessage(message));
        }
        return Result.success(messageService.sendNormalMessage(message));
    }

    @GetMapping("/conversation/{otherUserId}")
    public Result<?> conversation(@PathVariable Long otherUserId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getConversation(userId, otherUserId));
    }

    @GetMapping("/unread")
    public Result<?> unread(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getUnreadMessages(userId));
    }

    @GetMapping("/conversations")
    public Result<?> conversations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getConversationList(userId));
    }

    @GetMapping("/system")
    public Result<?> systemMessages(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getSystemMessages(userId));
    }

    @GetMapping("/contact/{otherUserId}")
    public Result<?> contact(@PathVariable Long otherUserId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getClueContactInfo(userId, otherUserId));
    }

    @GetMapping("/meta/{otherUserId}")
    public Result<?> conversationMeta(@PathVariable Long otherUserId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(messageService.getConversationMeta(userId, otherUserId));
    }

    @PostMapping("/read/{messageId}")
    public Result<?> markRead(@PathVariable Long messageId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messageService.markAsRead(messageId, userId);
        return Result.success();
    }

    @PostMapping("/block")
    public Result<?> block(@RequestBody BlockUserRequest body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (body == null || body.getBlockedUserId() == null) {
            return Result.error(400, "缺少被拉黑用户");
        }
        messageService.blockUser(userId, body.getBlockedUserId(), body.getReason());
        return Result.success();
    }

    @PostMapping("/unblock/{blockedUserId}")
    public Result<?> unblock(@PathVariable Long blockedUserId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        messageService.unblockUser(userId, blockedUserId);
        return Result.success();
    }

    @PostMapping("/report")
    public Result<?> report(@RequestBody ReportCreateRequest body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (body == null || body.getReportedUserId() == null || body.getReportType() == null) {
            return Result.error(400, "举报参数不完整");
        }
        return Result.success(messageService.createReport(
                userId,
                body.getReportedUserId(),
                body.getMessageId(),
                body.getReportType(),
                body.getReason(),
                body.getDetail()
        ));
    }
}
