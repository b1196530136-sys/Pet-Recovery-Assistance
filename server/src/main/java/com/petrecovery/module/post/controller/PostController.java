package com.petrecovery.module.post.controller;

import com.petrecovery.common.Result;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.service.PostService;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/post")
public class PostController {

    private final PostService postService;
    private final UserMapper userMapper;

    public PostController(PostService postService, UserMapper userMapper) {
        this.postService = postService;
        this.userMapper = userMapper;
    }

    @PostMapping("/create")
    public Result<?> create(@RequestBody PetSearchPost post, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(postService.createPost(post, userId));
    }

    @PostMapping("/search")
    public Result<?> search(@RequestBody PostSearchRequest request) {
        return Result.success(postService.searchPosts(request));
    }

    @PostMapping("/search-by-image")
    public Result<?> searchByImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        try {
            return Result.success(postService.searchByImage(file.getBytes()));
        } catch (IOException e) {
            return Result.error("图片处理失败");
        }
    }

    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        PetSearchPost post = postService.getById(id);
        Map<String, Object> result = new HashMap<>();
        if (post == null) return Result.error("寻宠启事不存在");
        result.put("id", post.getId());
        result.put("userId", post.getUserId());
        result.put("petType", post.getPetType());
        result.put("breed", post.getBreed());
        result.put("petName", post.getPetName());
        result.put("photos", post.getPhotos());
        result.put("lostTime", post.getLostTime());
        result.put("longitude", post.getLongitude());
        result.put("latitude", post.getLatitude());
        result.put("address", post.getAddress());
        result.put("reward", post.getReward());
        result.put("description", post.getDescription());
        result.put("status", post.getStatus());
        result.put("createTime", post.getCreateTime());
        SysUser user = userMapper.selectById(post.getUserId());
        result.put("publisherName", user != null ? user.getNickname() : "未知用户");
        result.put("publisherAvatar", user != null ? user.getAvatar() : null);
        return Result.success(result);
    }

    @PostMapping("/resolve/{id}")
    public Result<?> resolve(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        postService.resolvePost(id, userId);
        return Result.success();
    }

    @GetMapping("/my")
    public Result<?> myPosts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(postService.getMyPosts(userId));
    }

    @GetMapping("/clued")
    public Result<?> cluedPosts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(postService.getCluedPosts(userId));
    }

    @PostMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        postService.deletePost(id, userId);
        return Result.success();
    }

    @PostMapping("/compute-hashes")
    public Result<?> computeHashes(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        postService.computeHashForExisting();
        return Result.success();
    }
}
