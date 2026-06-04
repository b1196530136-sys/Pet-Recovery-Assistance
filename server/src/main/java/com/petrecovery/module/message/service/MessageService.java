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
    boolean markAsRead(Long messageId, Long userId);
}
