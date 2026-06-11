package com.petrecovery.module.post.controller;

import com.petrecovery.common.Result;
import com.petrecovery.config.JwtConfig;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.service.PostService;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/post")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    private final PostService postService;
    private final UserMapper userMapper;
    private final JwtConfig jwtConfig;

    public PostController(PostService postService, UserMapper userMapper, JwtConfig jwtConfig) {
        this.postService = postService;
        this.userMapper = userMapper;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/create")
    public Result<?> create(@RequestBody PetSearchPost post, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(postService.createPost(post, userId));
    }

    @PutMapping("/update")
    public Result<?> update(@RequestBody PetSearchPost post, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(postService.updatePost(post, userId));
    }

    @PostMapping("/search")
    public Result<?> search(@RequestBody PostSearchRequest request) {
        return Result.success(postService.searchPosts(request));
    }

    /**
     * 以图搜宠：上传宠物图片，通过阿里云API与已有帖子图片比对相似度
     */
    @PostMapping("/search-by-image")
    public Result<?> searchByImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        // 基础校验
        if (!file.getContentType().startsWith("image/")) {
            return Result.error("仅支持上传图片文件");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("图片大小不能超过5MB");
        }
        try {
            return Result.success(postService.searchByImage(file.getBytes()));
        } catch (IllegalStateException e) {
            // 配置缺失（如 server.public-url 未设置）
            log.error("以图搜宠配置错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            // 针对URL类错误给出更友好的提示
            if (msg.contains("URL") || msg.contains("url") || msg.contains("地址")) {
                log.warn("以图搜宠URL错误: {}", msg);
                return Result.error(msg);
            }
            log.error("以图搜宠接口调用异常", e);
            return Result.error("图像识别服务暂时不可用，请稍后重试。详情: " + msg);
        } catch (IOException e) {
            log.error("以图搜宠读取文件失败", e);
            return Result.error("图片读取失败");
        } catch (Exception e) {
            log.error("以图搜宠未知异常", e);
            return Result.error("服务内部错误，请稍后重试");
        }
    }

    /**
     * 以图搜宠（图床中转模式）：传入已上传到图床的图片URL进行宠物相似度对比
     * 推荐使用此方式，先通过 /api/upload/image 上传图片获取URL，再调用此接口
     */
    @PostMapping("/search-by-image-url")
    public Result<?> searchByImageUrl(@RequestBody Map<String, String> request) {
        String imageUrl = request.get("imageUrl");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return Result.error("图片URL不能为空");
        }
        // 基础校验：确保是本站图床的URL，防止SSRF攻击
        if (!isValidImageUrl(imageUrl)) {
            return Result.error("无效的图片URL地址");
        }
        try {
            return Result.success(postService.searchByImageUrl(imageUrl.trim()));
        } catch (IllegalStateException e) {
            log.error("以图搜宠配置错误: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg.contains("URL") || msg.contains("url") || msg.contains("地址")) {
                log.warn("以图搜宠URL错误: {}", msg);
                return Result.error(msg);
            }
            log.error("以图搜宠接口调用异常", e);
            return Result.error("图像识别服务暂时不可用，请稍后重试。详情: " + msg);
        } catch (Exception e) {
            log.error("以图搜宠未知异常", e);
            return Result.error("服务内部错误，请稍后重试");
        }
    }

    /**
     * 校验图片URL是否合法（防止SSRF攻击）
     */
    private boolean isValidImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        // 只允许本站图床的相对路径或绝对路径
        return url.startsWith("/upload/") ||
               (url.contains("/upload/") && (url.startsWith("http://") || url.startsWith("https://")));
    }

    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
        PetSearchPost post = postService.getById(id);
        Map<String, Object> result = new HashMap<>();
        if (post == null) return Result.error("寻宠启事不存在");
        RequestUser viewer = resolveViewer(request);
        boolean privileged = isOwner(post, viewer) || isAdmin(viewer);
        if (!("ACTIVE".equals(post.getStatus()) || "RESOLVED".equals(post.getStatus())) && !privileged) {
            return Result.error(403, "无权查看此寻宠启事");
        }
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
        if (privileged) {
            result.put("rejectReason", post.getRejectReason());
        }
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

    @GetMapping("/clue-trail/{postId}")
    public Result<?> clueTrail(@PathVariable Long postId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return Result.success(postService.getClueTrail(postId, userId, role));
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

    /**
     * 批量计算哈希（新版使用在线API，此接口保留兼容）
     */
    @PostMapping("/compute-hashes")
    public Result<?> computeHashes(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error(401, "未登录");
        postService.computeHashForExisting();
        return Result.success();
    }

    private boolean isOwner(PetSearchPost post, RequestUser viewer) {
        return viewer.userId != null && post.getUserId() != null && post.getUserId().equals(viewer.userId);
    }

    private boolean isAdmin(RequestUser viewer) {
        return "ADMIN".equals(viewer.role);
    }

    private RequestUser resolveViewer(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        Object role = request.getAttribute("role");
        if (userId instanceof Long id) {
            return new RequestUser(id, role instanceof String r ? r : null);
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                Claims claims = jwtConfig.parseToken(token.substring(7));
                return new RequestUser(Long.parseLong(claims.getSubject()), claims.get("role", String.class));
            } catch (Exception ignored) {
            }
        }
        return new RequestUser(null, null);
    }

    private record RequestUser(Long userId, String role) {}
}
