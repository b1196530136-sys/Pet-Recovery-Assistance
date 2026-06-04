package com.petrecovery.module.post.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;

public interface PostService extends IService<PetSearchPost> {
    PetSearchPost createPost(PetSearchPost post, Long userId);
    IPage<PetSearchPost> searchPosts(PostSearchRequest request);
    boolean submitClue(Long postId, Long userId, String clueData);
    boolean resolvePost(Long postId, Long userId);
}
