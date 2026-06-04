package com.petrecovery.module.post.controller;

import com.petrecovery.common.Result;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

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
}
