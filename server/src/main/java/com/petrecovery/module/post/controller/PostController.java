package com.petrecovery.module.post.controller;

import com.petrecovery.common.Result;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/post")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
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
        return Result.success(postService.getById(id));
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
