package com.petrecovery.module.post.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.mapper.PostMapper;
import org.springframework.stereotype.Service;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, PetSearchPost> implements PostService {

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
        if (request.getPetType() != null) {
            wrapper.eq(PetSearchPost::getPetType, request.getPetType());
        }
        if (request.getProvince() != null) {
            wrapper.like(PetSearchPost::getAddress, request.getProvince());
        }
        if (request.getStatus() != null) {
            wrapper.eq(PetSearchPost::getStatus, request.getStatus());
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
}
