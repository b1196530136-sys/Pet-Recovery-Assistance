package com.petrecovery.module.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.module.message.entity.SysImMessage;
import com.petrecovery.module.message.mapper.MessageMapper;
import com.petrecovery.module.message.websocket.ChatWebSocketHandler;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.mapper.PostMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, SysImMessage> implements MessageService {

    private final ChatWebSocketHandler chatWebSocketHandler;
    private final PostMapper postMapper;

    public MessageServiceImpl(ChatWebSocketHandler chatWebSocketHandler, PostMapper postMapper) {
        this.chatWebSocketHandler = chatWebSocketHandler;
        this.postMapper = postMapper;
    }

    @Override
    public SysImMessage sendClueMessage(SysImMessage message) {
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
        message.setReadStatus(0);
        message.setMsgType(1);
        save(message);
        // 尝试实时推送，用户离线则消息留存在库中待拉取
        chatWebSocketHandler.sendMessage(message.getReceiverId(), message);
        return message;
    }

    @Override
    public SysImMessage sendNormalMessage(SysImMessage message) {
        if (message.getReceiverId() == null) {
            throw new RuntimeException("接收人不能为空");
        }
        if (message.getReceiverId().equals(message.getSenderId())) {
            throw new RuntimeException("不能给自己发送消息");
        }
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
            conv.put("lastContent", latest.getMsgType() == 1 ? "[线索] " + (latest.getContent() != null ? latest.getContent() : "") : (latest.getContent() != null ? latest.getContent() : ""));
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
