package com.petrecovery.module.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.module.message.entity.SysImMessage;
import com.petrecovery.module.message.mapper.MessageMapper;
import com.petrecovery.module.message.websocket.ChatWebSocketHandler;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, SysImMessage> implements MessageService {

    private final ChatWebSocketHandler chatWebSocketHandler;

    public MessageServiceImpl(ChatWebSocketHandler chatWebSocketHandler) {
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @Override
    public SysImMessage sendClueMessage(SysImMessage message) {
        message.setReadStatus(0);
        message.setMsgType(1);
        save(message);
        // 尝试实时推送，用户离线则消息留存在库中待拉取
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
}
