package com.petrecovery.module.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petrecovery.common.util.PhoneCryptoUtil;
import com.petrecovery.module.message.entity.SysImMessage;
import com.petrecovery.module.message.entity.UserBlock;
import com.petrecovery.module.message.entity.UserReport;
import com.petrecovery.module.message.mapper.MessageMapper;
import com.petrecovery.module.message.mapper.UserBlockMapper;
import com.petrecovery.module.message.mapper.UserReportMapper;
import com.petrecovery.module.message.websocket.ChatWebSocketHandler;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.mapper.PostMapper;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, SysImMessage> implements MessageService {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final JavaMailSender mailSender;
    private final PhoneCryptoUtil phoneCryptoUtil;
    private final ObjectMapper objectMapper;
    private final UserBlockMapper userBlockMapper;
    private final UserReportMapper userReportMapper;

    private static final Set<String> REPORT_TYPES = Set.of("SPAM", "HARASSMENT", "FALSE_CLUE", "OTHER");

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public MessageServiceImpl(ChatWebSocketHandler chatWebSocketHandler, PostMapper postMapper,
                              UserMapper userMapper, JavaMailSender mailSender,
                              PhoneCryptoUtil phoneCryptoUtil, ObjectMapper objectMapper,
                              UserBlockMapper userBlockMapper, UserReportMapper userReportMapper) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.postMapper = postMapper;
        this.userMapper = userMapper;
        this.mailSender = mailSender;
        this.phoneCryptoUtil = phoneCryptoUtil;
        this.objectMapper = objectMapper;
        this.userBlockMapper = userBlockMapper;
        this.userReportMapper = userReportMapper;
    }

    @Override
    public SysImMessage sendClueMessage(SysImMessage message) {
        validateSendPermission(message.getSenderId(), message.getReceiverId(), message.getContent(), 1);
        if (message.getPostId() == null) {
            throw new RuntimeException("线索必须关联寻宠启事");
        }
        PetSearchPost post = postMapper.selectById(message.getPostId());
        if (post == null) {
            throw new RuntimeException("寻宠启事不存在");
        }
        if (!"ACTIVE".equals(post.getStatus())) {
            throw new RuntimeException("只能对寻找中的启事提交线索");
        }
        if (post.getUserId().equals(message.getSenderId())) {
            throw new RuntimeException("不能给自己的寻宠启事提交线索");
        }
        message.setReceiverId(post.getUserId());
        validateSendPermission(message.getSenderId(), message.getReceiverId(), message.getContent(), 1);
        message.setReadStatus(0);
        message.setMsgType(1);
        save(message);
        // 尝试实时推送，用户离线则消息留存在库中待拉取
        chatWebSocketHandler.sendMessage(message.getReceiverId(), message);
        sendClueNotificationEmail(post.getUserId());
        return message;
    }

    private void sendClueNotificationEmail(Long ownerId) {
        SysUser owner = userMapper.selectById(ownerId);
        if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) {
            log.warn("Skip clue notification email, owner email is empty. ownerId={}", ownerId);
            return;
        }
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                mailMessage.setFrom(mailFrom);
            }
            mailMessage.setTo(owner.getEmail());
            mailMessage.setSubject("寻宠互助平台 - 新线索提醒");
            mailMessage.setText("有新的线索了！请及时查看。");
            mailSender.send(mailMessage);
        } catch (Exception e) {
            log.error("Failed to send clue notification email. ownerId={}, email={}", ownerId, owner.getEmail(), e);
        }
    }

    @Override
    public SysImMessage sendNormalMessage(SysImMessage message) {
        if (message.getReceiverId() == null) {
            throw new RuntimeException("接收人不能为空");
        }
        if (message.getReceiverId().equals(message.getSenderId())) {
            throw new RuntimeException("不能给自己发送消息");
        }
        validateSendPermission(message.getSenderId(), message.getReceiverId(), message.getContent(), 0);
        message.setReadStatus(0);
        message.setMsgType(0);
        save(message);
        chatWebSocketHandler.sendMessage(message.getReceiverId(), message);
        return message;
    }

    @Override
    public List<SysImMessage> getConversation(Long userId, Long otherUserId) {
        LambdaQueryWrapper<SysImMessage> wrapper = new LambdaQueryWrapper<SysImMessage>()
                .and(w -> w.eq(SysImMessage::getSenderId, userId)
                        .eq(SysImMessage::getReceiverId, otherUserId))
                .or(w -> w.eq(SysImMessage::getSenderId, otherUserId)
                        .eq(SysImMessage::getReceiverId, userId))
                .orderByAsc(SysImMessage::getCreateTime);
        return list(wrapper);
    }

    @Override
    public List<Map<String, Object>> getConversationList(Long userId) {
        // 查出用户参与的所有消息
        List<SysImMessage> all = list(new LambdaQueryWrapper<SysImMessage>()
                .and(w -> w.eq(SysImMessage::getSenderId, userId)
                        .or(w2 -> w2.eq(SysImMessage::getReceiverId, userId)))
                .ne(SysImMessage::getMsgType, 2)
                .orderByDesc(SysImMessage::getCreateTime));

        // 按对方用户分组
        Map<Long, List<SysImMessage>> grouped = all.stream()
                .collect(Collectors.groupingBy(msg -> msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId()));

        List<Map<String, Object>> result = new ArrayList<>();
        grouped.forEach((otherId, msgs) -> {
            SysImMessage latest = msgs.get(0);
            long unread = msgs.stream().filter(m -> m.getReceiverId().equals(userId) && m.getReadStatus() == 0).count();
            Map<String, Object> conv = new HashMap<>();
            conv.put("otherId", otherId);
            conv.put("lastContent", formatLastContent(latest));
            conv.put("lastTime", latest.getCreateTime());
            conv.put("unread", unread);
            result.add(conv);
        });

        result.sort((a, b) -> {
            LocalDateTime ta = (LocalDateTime) a.get("lastTime");
            LocalDateTime tb = (LocalDateTime) b.get("lastTime");
            return tb.compareTo(ta);
        });
        return result;
    }

    @Override
    public SysImMessage sendSystemMessage(Long receiverId, String title, String body, String targetType, Long targetId, String resultType) {
        if (receiverId == null) {
            throw new RuntimeException("接收人不能为空");
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("body", body);
        payload.put("targetType", targetType);
        payload.put("targetId", targetId);
        payload.put("resultType", resultType);

        SysImMessage message = new SysImMessage();
        message.setSenderId(0L);
        message.setReceiverId(receiverId);
        message.setMsgType(2);
        message.setReadStatus(0);
        try {
            message.setContent(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            message.setContent(title + "\n" + body);
        }
        save(message);
        chatWebSocketHandler.sendMessage(receiverId, message);
        return message;
    }

    @Override
    public List<Map<String, Object>> getSystemMessages(Long userId) {
        List<SysImMessage> messages = list(new LambdaQueryWrapper<SysImMessage>()
                .eq(SysImMessage::getReceiverId, userId)
                .eq(SysImMessage::getMsgType, 2)
                .orderByDesc(SysImMessage::getCreateTime));
        return messages.stream().map(this::toSystemMessageMap).collect(Collectors.toList());
    }

    private Map<String, Object> toSystemMessageMap(SysImMessage message) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", message.getId());
        map.put("readStatus", message.getReadStatus());
        map.put("createTime", message.getCreateTime());
        map.put("title", "系统消息");
        map.put("body", message.getContent());
        map.put("targetType", null);
        map.put("targetId", null);
        map.put("resultType", null);
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getContent(), new TypeReference<>() {});
            map.putAll(payload);
        } catch (Exception ignored) {
        }
        return map;
    }

    private String formatLastContent(SysImMessage latest) {
        if (latest.getMsgType() != null && latest.getMsgType() == 1) {
            return "[线索] " + (latest.getContent() != null ? latest.getContent() : "");
        }
        if (latest.getMsgType() != null && latest.getMsgType() == 2) {
            try {
                Map<String, Object> payload = objectMapper.readValue(latest.getContent(), new TypeReference<>() {});
                Object title = payload.get("title");
                return "[系统] " + (title != null ? title : "系统消息");
            } catch (Exception ignored) {
                return "[系统] 系统消息";
            }
        }
        return latest.getContent() != null ? latest.getContent() : "";
    }

    @Override
    public Map<String, Object> getClueContactInfo(Long userId, Long otherUserId) {
        if (userId == null || otherUserId == null || userId.equals(otherUserId)) {
            throw new RuntimeException("无效的联系人");
        }
        boolean hasClueRelation = count(new LambdaQueryWrapper<SysImMessage>()
                .eq(SysImMessage::getMsgType, 1)
                .and(w -> w.and(a -> a.eq(SysImMessage::getSenderId, userId)
                                .eq(SysImMessage::getReceiverId, otherUserId))
                        .or(a -> a.eq(SysImMessage::getSenderId, otherUserId)
                                .eq(SysImMessage::getReceiverId, userId)))) > 0;

        SysUser me = userMapper.selectById(userId);
        SysUser other = userMapper.selectById(otherUserId);
        Map<String, Object> result = new HashMap<>();
        result.put("hasClueRelation", hasClueRelation);
        result.put("myHasPhone", me != null && phoneCryptoUtil.hasPhone(me.getPhone()));
        result.put("otherHasPhone", hasClueRelation && other != null && phoneCryptoUtil.hasPhone(other.getPhone()));
        boolean blocked = isBlockedEitherWay(userId, otherUserId);
        result.put("isBlocked", blocked);
        result.put("otherPhoneMasked", hasClueRelation && other != null && !blocked ? phoneCryptoUtil.mask(other.getPhone()) : null);
        result.put("otherPhone", hasClueRelation && other != null && !blocked ? phoneCryptoUtil.decrypt(other.getPhone()) : null);
        return result;
    }

    @Override
    public List<SysImMessage> getUnreadMessages(Long userId) {
        return list(new LambdaQueryWrapper<SysImMessage>()
                .eq(SysImMessage::getReceiverId, userId)
                .eq(SysImMessage::getReadStatus, 0)
                .orderByDesc(SysImMessage::getCreateTime));
    }

    @Override
    public boolean markAsRead(Long messageId, Long userId) {
        SysImMessage message = getById(messageId);
        if (message != null && message.getReceiverId().equals(userId)) {
            message.setReadStatus(1);
            return updateById(message);
        }
        return false;
    }

    @Override
    public Map<String, Object> getConversationMeta(Long userId, Long otherUserId) {
        Map<String, Object> result = new HashMap<>();
        boolean iBlocked = isBlocked(userId, otherUserId);
        boolean blockedMe = isBlocked(otherUserId, userId);
        long pendingReports = userReportMapper.selectCount(new LambdaQueryWrapper<UserReport>()
                .eq(UserReport::getReporterId, userId)
                .eq(UserReport::getReportedUserId, otherUserId)
                .eq(UserReport::getStatus, "PENDING"));
        result.put("iBlocked", iBlocked);
        result.put("blockedMe", blockedMe);
        result.put("isBlocked", iBlocked || blockedMe);
        result.put("pendingReportCount", pendingReports);
        return result;
    }

    @Override
    public void blockUser(Long userId, Long blockedUserId, String reason) {
        if (userId == null || blockedUserId == null || userId.equals(blockedUserId)) {
            throw new IllegalArgumentException("无效的拉黑对象");
        }
        SysUser blockedUser = userMapper.selectById(blockedUserId);
        if (blockedUser == null) {
            throw new RuntimeException("被拉黑用户不存在");
        }
        if (isBlocked(userId, blockedUserId)) {
            return;
        }
        UserBlock block = new UserBlock();
        block.setUserId(userId);
        block.setBlockedUserId(blockedUserId);
        block.setReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        userBlockMapper.insert(block);
    }

    @Override
    public void unblockUser(Long userId, Long blockedUserId) {
        userBlockMapper.delete(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, userId)
                .eq(UserBlock::getBlockedUserId, blockedUserId));
    }

    @Override
    public Map<String, Object> createReport(Long userId, Long reportedUserId, Long messageId, String reportType, String reason, String detail) {
        if (userId == null || reportedUserId == null || userId.equals(reportedUserId)) {
            throw new IllegalArgumentException("无效的举报对象");
        }
        if (reportType == null || !REPORT_TYPES.contains(reportType)) {
            throw new IllegalArgumentException("无效的举报类型");
        }
        SysUser reportedUser = userMapper.selectById(reportedUserId);
        if (reportedUser == null) {
            throw new RuntimeException("被举报用户不存在");
        }
        SysImMessage targetMessage = null;
        if (messageId != null) {
            targetMessage = getById(messageId);
            if (targetMessage == null) {
                throw new RuntimeException("举报消息不存在");
            }
            if (!Objects.equals(targetMessage.getSenderId(), reportedUserId) && !Objects.equals(targetMessage.getReceiverId(), reportedUserId)) {
                throw new RuntimeException("举报对象与消息不匹配");
            }
            boolean related = Objects.equals(targetMessage.getSenderId(), userId) || Objects.equals(targetMessage.getReceiverId(), userId);
            if (!related) {
                throw new SecurityException("无权举报该消息");
            }
        }

        long duplicatePending = userReportMapper.selectCount(new LambdaQueryWrapper<UserReport>()
                .eq(UserReport::getReporterId, userId)
                .eq(UserReport::getReportedUserId, reportedUserId)
                .eq(messageId != null, UserReport::getMessageId, messageId)
                .eq(UserReport::getStatus, "PENDING"));
        if (duplicatePending > 0) {
            throw new RuntimeException("你已提交过相同对象的待处理举报");
        }

        UserReport report = new UserReport();
        report.setReporterId(userId);
        report.setReportedUserId(reportedUserId);
        report.setMessageId(messageId);
        report.setReportType(reportType);
        report.setReason(trimToNull(reason, 128));
        report.setDetail(trimToNull(detail, 1000));
        report.setStatus("PENDING");
        report.setMessageSnapshot(buildMessageSnapshot(targetMessage));
        userReportMapper.insert(report);

        Map<String, Object> result = new HashMap<>();
        result.put("id", report.getId());
        result.put("status", report.getStatus());
        result.put("createTime", report.getCreateTime());
        return result;
    }

    @Override
    public List<Map<String, Object>> getPendingReports() {
        return toReportMaps(userReportMapper.selectList(new LambdaQueryWrapper<UserReport>()
                .eq(UserReport::getStatus, "PENDING")
                .orderByAsc(UserReport::getCreateTime)));
    }

    @Override
    public List<Map<String, Object>> getAllReports() {
        return toReportMaps(userReportMapper.selectList(new LambdaQueryWrapper<UserReport>()
                .orderByDesc(UserReport::getCreateTime)));
    }

    @Override
    public void handleReport(Long reportId, Long adminUserId, String action, String handleNote, boolean banReportedUser) {
        UserReport report = userReportMapper.selectById(reportId);
        if (report == null) {
            throw new RuntimeException("举报不存在");
        }
        if (!"PENDING".equals(report.getStatus())) {
            throw new RuntimeException("该举报已处理");
        }
        if (!"RESOLVED".equals(action) && !"REJECTED".equals(action)) {
            throw new IllegalArgumentException("无效的处理动作");
        }
        report.setStatus(action);
        report.setHandledBy(adminUserId);
        report.setHandleNote(trimToNull(handleNote, 512));
        userReportMapper.updateById(report);

        if (banReportedUser) {
            SysUser user = userMapper.selectById(report.getReportedUserId());
            if (user != null && !"ADMIN".equals(user.getRole())) {
                user.setStatus(0);
                userMapper.updateById(user);
            }
        }

        sendSystemMessage(report.getReporterId(),
                "举报处理结果通知",
                "你提交的举报已" + ("RESOLVED".equals(action) ? "处理" : "驳回") + "。" +
                        (report.getHandleNote() != null ? "说明：" + report.getHandleNote() : ""),
                "report", report.getId(), action);
    }

    private void validateSendPermission(Long senderId, Long receiverId, String content, int msgType) {
        if (senderId == null || receiverId == null) {
            return;
        }
        if (isBlocked(senderId, receiverId)) {
            throw new RuntimeException("你已拉黑对方，无法继续发送消息");
        }
        if (isBlocked(receiverId, senderId)) {
            throw new RuntimeException("对方已拒收你的消息");
        }
        checkSendFrequency(senderId, receiverId, content, msgType);
    }

    private void checkSendFrequency(Long senderId, Long receiverId, String content, int msgType) {
        LocalDateTime now = LocalDateTime.now();
        long recentCount = count(new LambdaQueryWrapper<SysImMessage>()
                .eq(SysImMessage::getSenderId, senderId)
                .eq(SysImMessage::getReceiverId, receiverId)
                .ge(SysImMessage::getCreateTime, now.minusMinutes(10)));
        if (recentCount >= 8) {
            throw new RuntimeException("发送过于频繁，请稍后再试");
        }

        String normalizedContent = normalizeContent(content);
        if (!normalizedContent.isEmpty()) {
            SysImMessage recentSame = getOne(new LambdaQueryWrapper<SysImMessage>()
                    .eq(SysImMessage::getSenderId, senderId)
                    .eq(SysImMessage::getReceiverId, receiverId)
                    .eq(SysImMessage::getMsgType, msgType)
                    .orderByDesc(SysImMessage::getCreateTime)
                    .last("LIMIT 5"), false);
            if (recentSame != null) {
                String latest = normalizeContent(recentSame.getContent());
                if (normalizedContent.equals(latest)
                        && recentSame.getCreateTime() != null
                        && recentSame.getCreateTime().isAfter(now.minusMinutes(3))) {
                    throw new RuntimeException("请勿短时间重复发送相同内容");
                }
            }
        }
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.replaceAll("\\s+", "").trim();
    }

    private boolean isBlockedEitherWay(Long userId, Long otherUserId) {
        return isBlocked(userId, otherUserId) || isBlocked(otherUserId, userId);
    }

    private boolean isBlocked(Long userId, Long otherUserId) {
        if (userId == null || otherUserId == null) {
            return false;
        }
        return userBlockMapper.selectCount(new LambdaQueryWrapper<UserBlock>()
                .eq(UserBlock::getUserId, userId)
                .eq(UserBlock::getBlockedUserId, otherUserId)) > 0;
    }

    private String buildMessageSnapshot(SysImMessage message) {
        if (message == null) {
            return null;
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", message.getId());
        snapshot.put("msgType", message.getMsgType());
        snapshot.put("content", message.getContent());
        snapshot.put("clueTime", message.getClueTime());
        snapshot.put("clueAddress", message.getClueAddress());
        snapshot.put("cluePhotos", message.getCluePhotos());
        snapshot.put("createTime", message.getCreateTime());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return message.getContent();
        }
    }

    private List<Map<String, Object>> toReportMaps(List<UserReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = new HashSet<>();
        Set<Long> messageIds = new HashSet<>();
        for (UserReport report : reports) {
            if (report.getReporterId() != null) userIds.add(report.getReporterId());
            if (report.getReportedUserId() != null) userIds.add(report.getReportedUserId());
            if (report.getHandledBy() != null) userIds.add(report.getHandledBy());
            if (report.getMessageId() != null) messageIds.add(report.getMessageId());
        }
        Map<Long, SysUser> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));
        Map<Long, SysImMessage> messageMap = messageIds.isEmpty()
                ? Collections.emptyMap()
                : listByIds(messageIds).stream().collect(Collectors.toMap(SysImMessage::getId, m -> m));

        return reports.stream().map(report -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", report.getId());
            map.put("reportType", report.getReportType());
            map.put("reason", report.getReason());
            map.put("detail", report.getDetail());
            map.put("status", report.getStatus());
            map.put("handleNote", report.getHandleNote());
            map.put("createTime", report.getCreateTime());
            map.put("updateTime", report.getUpdateTime());
            map.put("reporterId", report.getReporterId());
            map.put("reportedUserId", report.getReportedUserId());
            map.put("messageId", report.getMessageId());
            map.put("messageSnapshot", parseJsonSafely(report.getMessageSnapshot()));
            SysUser reporter = userMap.get(report.getReporterId());
            SysUser reported = userMap.get(report.getReportedUserId());
            SysUser handledBy = userMap.get(report.getHandledBy());
            map.put("reporterName", reporter != null ? defaultDisplayName(reporter) : "未知用户");
            map.put("reporterEmail", reporter != null ? reporter.getEmail() : null);
            map.put("reportedName", reported != null ? defaultDisplayName(reported) : "未知用户");
            map.put("reportedEmail", reported != null ? reported.getEmail() : null);
            map.put("handledByName", handledBy != null ? defaultDisplayName(handledBy) : null);
            SysImMessage message = messageMap.get(report.getMessageId());
            if (message != null) {
                map.put("message", message);
            }
            return map;
        }).collect(Collectors.toList());
    }

    private Object parseJsonSafely(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (Exception e) {
            return raw;
        }
    }

    private String defaultDisplayName(SysUser user) {
        if (user == null) {
            return "未知用户";
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getEmail();
    }

    private String trimToNull(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
