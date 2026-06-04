package com.petrecovery.module.message.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.message.entity.SysImMessage;
import java.util.List;

public interface MessageService extends IService<SysImMessage> {
    SysImMessage sendClueMessage(SysImMessage message);
    List<SysImMessage> getConversation(Long userId, Long otherUserId);
    List<SysImMessage> getUnreadMessages(Long userId);
    boolean markAsRead(Long messageId, Long userId);
}
