package com.petrecovery.module.post.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;

import java.util.List;
import java.util.Map;

public interface PostService extends IService<PetSearchPost> {
    PetSearchPost createPost(PetSearchPost post, Long userId);
    IPage<PetSearchPost> searchPosts(PostSearchRequest request);
    boolean submitClue(Long postId, Long userId, String clueData);
    boolean resolvePost(Long postId, Long userId);
    List<PetSearchPost> getMyPosts(Long userId);
    List<PetSearchPost> getCluedPosts(Long userId);
    boolean deletePost(Long postId, Long userId);
    List<Map<String, Object>> searchByImage(byte[] imageBytes);
    void computeHashForExisting();
}
