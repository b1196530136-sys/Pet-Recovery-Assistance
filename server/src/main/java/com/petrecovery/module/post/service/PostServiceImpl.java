package com.petrecovery.module.post.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.module.message.entity.SysImMessage;
import com.petrecovery.module.message.mapper.MessageMapper;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.mapper.PostMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, PetSearchPost> implements PostService {

    private final MessageMapper messageMapper;

    public PostServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public PetSearchPost createPost(PetSearchPost post, Long userId) {
        post.setUserId(userId);
        post.setStatus("PENDING");
        save(post);
        return post;
    }

    @Override
    public IPage<PetSearchPost> searchPosts(PostSearchRequest request) {
        LambdaQueryWrapper<PetSearchPost> wrapper = new LambdaQueryWrapper<>();
        if (request.getPetType() != null && !request.getPetType().isEmpty()) {
            wrapper.eq(PetSearchPost::getPetType, request.getPetType());
        }
        if (request.getProvince() != null && !request.getProvince().isEmpty()) {
            wrapper.like(PetSearchPost::getAddress, request.getProvince());
        }
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            wrapper.eq(PetSearchPost::getStatus, request.getStatus());
        } else {
            wrapper.in(PetSearchPost::getStatus, "ACTIVE", "RESOLVED");
        }
        wrapper.orderByDesc(PetSearchPost::getCreateTime);
        return page(new Page<>(request.getPage(), request.getSize()), wrapper);
    }

    @Override
    public boolean submitClue(Long postId, Long userId, String clueData) {
        // 线索提交 - 实际由 Message 模块处理
        return true;
    }

    @Override
    public boolean resolvePost(Long postId, Long userId) {
        PetSearchPost post = getById(postId);
        if (post != null && post.getUserId().equals(userId)) {
            post.setStatus("RESOLVED");
            return updateById(post);
        }
        return false;
    }

    @Override
    public List<PetSearchPost> getMyPosts(Long userId) {
        return list(new LambdaQueryWrapper<PetSearchPost>()
                .eq(PetSearchPost::getUserId, userId)
                .orderByDesc(PetSearchPost::getCreateTime));
    }

    @Override
    public List<PetSearchPost> getCluedPosts(Long userId) {
        // 查出当前用户提交过的所有线索消息中的 postId
        List<SysImMessage> msgs = messageMapper.selectList(
                new LambdaQueryWrapper<SysImMessage>()
                        .eq(SysImMessage::getSenderId, userId)
                        .eq(SysImMessage::getMsgType, 1));
        if (msgs.isEmpty()) return Collections.emptyList();
        List<Long> postIds = msgs.stream()
                .map(SysImMessage::getPostId)
                .distinct()
                .collect(Collectors.toList());
        return list(new LambdaQueryWrapper<PetSearchPost>()
                .in(PetSearchPost::getId, postIds)
                .orderByDesc(PetSearchPost::getCreateTime));
    }
}
