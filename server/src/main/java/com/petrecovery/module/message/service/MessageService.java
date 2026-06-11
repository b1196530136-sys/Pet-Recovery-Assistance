package com.petrecovery.module.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.message.entity.SysImMessage;
import java.util.List;
import java.util.Map;

public interface MessageService extends IService<SysImMessage> {
    SysImMessage sendClueMessage(SysImMessage message);
    SysImMessage sendNormalMessage(SysImMessage message);
    List<SysImMessage> getConversation(Long userId, Long otherUserId);
    List<SysImMessage> getUnreadMessages(Long userId);
    List<Map<String, Object>> getConversationList(Long userId);
    Map<String, Object> getClueContactInfo(Long userId, Long otherUserId);
    SysImMessage sendSystemMessage(Long receiverId, String title, String body, String targetType, Long targetId, String resultType);
    List<Map<String, Object>> getSystemMessages(Long userId);
    boolean markAsRead(Long messageId, Long userId);
    Map<String, Object> getConversationMeta(Long userId, Long otherUserId);
    void blockUser(Long userId, Long blockedUserId, String reason);
    void unblockUser(Long userId, Long blockedUserId);
    Map<String, Object> createReport(Long userId, Long reportedUserId, Long messageId, String reportType, String reason, String detail);
    List<Map<String, Object>> getPendingReports();
    List<Map<String, Object>> getAllReports();
    void handleReport(Long reportId, Long adminUserId, String action, String handleNote, boolean banReportedUser);
}
