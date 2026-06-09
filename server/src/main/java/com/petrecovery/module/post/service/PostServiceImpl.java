package com.petrecovery.module.post.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.common.util.PetImageRecognitionUtil;
import com.petrecovery.module.message.entity.SysImMessage;
import com.petrecovery.module.message.mapper.MessageMapper;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.mapper.PostMapper;
import com.petrecovery.storage.UploadStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, PetSearchPost> implements PostService {

    private final MessageMapper messageMapper;
    private final UploadStorageService uploadStorageService;
    private final PetImageRecognitionUtil recognitionUtil;

    public PostServiceImpl(MessageMapper messageMapper, UploadStorageService uploadStorageService,
                           PetImageRecognitionUtil recognitionUtil) {
        this.messageMapper = messageMapper;
        this.uploadStorageService = uploadStorageService;
        this.recognitionUtil = recognitionUtil;
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
        wrapper.in(PetSearchPost::getStatus, "ACTIVE", "RESOLVED");
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
    public List<Map<String, Object>> getClueTrail(Long postId, Long viewerId, String viewerRole) {
        PetSearchPost post = getById(postId);
        if (post == null) {
            throw new RuntimeException("寻宠启事不存在");
        }
        boolean privileged = post.getUserId().equals(viewerId) || "ADMIN".equals(viewerRole);
        if (!privileged) {
            throw new SecurityException("无权查看线索轨迹");
        }
        List<SysImMessage> clues = messageMapper.selectList(
                new LambdaQueryWrapper<SysImMessage>()
                        .eq(SysImMessage::getPostId, postId)
                        .eq(SysImMessage::getMsgType, 1)
                        .isNotNull(SysImMessage::getClueLongitude)
                        .isNotNull(SysImMessage::getClueLatitude)
                        .orderByAsc(SysImMessage::getClueTime));
        return clues.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("senderId", c.getSenderId());
            m.put("content", c.getContent());
            m.put("clueTime", c.getClueTime());
            m.put("clueLongitude", c.getClueLongitude());
            m.put("clueLatitude", c.getClueLatitude());
            m.put("clueAddress", c.getClueAddress());
            return m;
        }).collect(Collectors.toList());
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

    @Override
    public boolean deletePost(Long postId, Long userId) {
        PetSearchPost post = getById(postId);
        if (post != null && post.getUserId().equals(userId)) {
            return removeById(postId);
        }
        return false;
    }

    /**
     * 以图搜宠：将用户上传的图片与所有已审核帖子的图片逐一通过阿里云API比对相似度
     */
    @Override
    public List<Map<String, Object>> searchByImage(byte[] imageBytes) {
        // 1. 先将上传的图片保存到本地，获取路径
        String uploadedRelativeUrl = saveTempImage(imageBytes);
        if (uploadedRelativeUrl == null) {
            throw new RuntimeException("上传的图片保存失败，无法进行识别");
        }
        log.info("以图搜宠 - 上传图片已保存: {}", uploadedRelativeUrl);

        // 2. 获取所有已审核通过的、有照片的帖子
        List<PetSearchPost> allPosts = list(new LambdaQueryWrapper<PetSearchPost>()
                .in(PetSearchPost::getStatus, "ACTIVE", "RESOLVED")
                .isNotNull(PetSearchPost::getPhotos)
                .ne(PetSearchPost::getPhotos, ""));

        if (allPosts.isEmpty()) {
            log.info("以图搜宠 - 没有找到可匹配的帖子数据");
            return new ArrayList<>();
        }

        log.info("以图搜宠 - 共有 {} 个帖子待比对", allPosts.size());

        List<Map<String, Object>> results = new ArrayList<>();
        double threshold = recognitionUtil.getSimilarityThreshold();

        // Base64模式无需公网URL，直接使用本地文件路径进行比对

        int successCount = 0;
        int failCount = 0;

        // 3. 逐一调用快瞳API进行相似度对比
        for (PetSearchPost post : allPosts) {
            if (post.getPhotos() == null || post.getPhotos().isEmpty()) continue;

            String[] photoUrls = post.getPhotos().split(",");
            double bestScore = -1;
            String bestDescription = "";

            // 取每张照片与上传图片做对比，取最高分
            for (String photoUrl : photoUrls) {
                String url = photoUrl.trim();
                if (url.isEmpty()) continue;
                try {
                    Integer petType = resolvePetType(post.getPetType());
                    PetImageRecognitionUtil.RecognitionResult result =
                            recognitionUtil.compareImagesByPath(uploadedRelativeUrl, url, petType);
                    if (result.getConfidence() > bestScore) {
                        bestScore = result.getConfidence();
                        bestDescription = result.getDescription();
                    }
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.warn("图片比对失败: postId={}, url={}, error={}", post.getId(), maskLog(url), e.getMessage());
                }
            }

            // 相似度达到阈值则加入结果（confidence为0~1，threshold为百分比如60）
            if (bestScore >= threshold / 100.0) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", post.getId());
                item.put("petName", post.getPetName());
                item.put("petType", post.getPetType());
                item.put("photos", post.getPhotos());
                item.put("address", post.getAddress());
                item.put("lostTime", post.getLostTime());
                item.put("description", post.getDescription());
                item.put("status", post.getStatus());
                item.put("similarity", Math.round(bestScore * 1000.0) / 10.0); // 转为百分比显示
                item.put("recognitionDesc", bestDescription);
                results.add(item);
            }
        }

        log.info("以图搜宠完成: 成功{}次, 失败{}次, 匹配到{}条结果",
                successCount, failCount, results.size());

        // 按相似度降序排列
        results.sort((a, b) -> Double.compare((double) b.get("similarity"), (double) a.get("similarity")));
        return results;
    }

    /**
     * 以图搜宠（图床中转模式）：使用已上传到图床的图片URL进行宠物相似度对比
     * 相比 searchByImage，此方法无需保存临时文件，直接使用URL进行比对
     */
    @Override
    public List<Map<String, Object>> searchByImageUrl(String imageUrl) {
        log.info("以图搜宠（图床模式） - 使用图床URL: {}", maskLog(imageUrl));

        // 1. 获取所有已审核通过的、有照片的帖子
        List<PetSearchPost> allPosts = list(new LambdaQueryWrapper<PetSearchPost>()
                .in(PetSearchPost::getStatus, "ACTIVE", "RESOLVED")
                .isNotNull(PetSearchPost::getPhotos)
                .ne(PetSearchPost::getPhotos, ""));

        if (allPosts.isEmpty()) {
            log.info("以图搜宠（图床模式） - 没有找到可匹配的帖子数据");
            return new ArrayList<>();
        }

        log.info("以图搜宠（图床模式） - 共有 {} 个帖子待比对", allPosts.size());

        List<Map<String, Object>> results = new ArrayList<>();
        double threshold = recognitionUtil.getSimilarityThreshold();

        int successCount = 0;
        int failCount = 0;

        // 2. 逐一调用快瞳API进行相似度对比
        for (PetSearchPost post : allPosts) {
            if (post.getPhotos() == null || post.getPhotos().isEmpty()) continue;

            String[] photoUrls = post.getPhotos().split(",");
            double bestScore = -1;
            String bestDescription = "";

            // 取每张照片与上传图片做对比，取最高分
            for (String photoUrl : photoUrls) {
                String url = photoUrl.trim();
                if (url.isEmpty()) continue;
                try {
                    Integer petType = resolvePetType(post.getPetType());
                    PetImageRecognitionUtil.RecognitionResult result =
                            recognitionUtil.compareImagesByPath(imageUrl, url, petType);
                    if (result.getConfidence() > bestScore) {
                        bestScore = result.getConfidence();
                        bestDescription = result.getDescription();
                    }
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.warn("图片比对失败: postId={}, url={}, error={}", post.getId(), maskLog(url), e.getMessage());
                }
            }

            // 相似度达到阈值则加入结果（confidence为0~1，threshold为百分比如60）
            if (bestScore >= threshold / 100.0) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", post.getId());
                item.put("petName", post.getPetName());
                item.put("petType", post.getPetType());
                item.put("photos", post.getPhotos());
                item.put("address", post.getAddress());
                item.put("lostTime", post.getLostTime());
                item.put("description", post.getDescription());
                item.put("status", post.getStatus());
                item.put("similarity", Math.round(bestScore * 1000.0) / 10.0); // 转为百分比显示
                item.put("recognitionDesc", bestDescription);
                results.add(item);
            }
        }

        log.info("以图搜宠（图床模式）完成: 成功{}次, 失败{}次, 匹配到{}条结果",
                successCount, failCount, results.size());

        // 按相似度降序排列
        results.sort((a, b) -> Double.compare((double) b.get("similarity"), (double) a.get("similarity")));
        return results;
    }

    /**
     * 将上传的字节数组临时保存为文件并返回相对路径
     * 图片将通过base64方式传给快瞳API，无需公网URL
     */
    private String saveTempImage(byte[] imageBytes) {
        try {
            String storagePath = uploadStorageService.getStoragePath();
            String fileName = System.currentTimeMillis() + "_search_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + ".jpg";
            java.nio.file.Path filePath = java.nio.file.Paths.get(storagePath, fileName);
            java.nio.file.Files.write(filePath, imageBytes);
            return "/upload/" + fileName;
        } catch (Exception e) {
            log.error("临时保存搜索图片失败", e);
            return null;
        }
    }

    /**
     * 日志脱敏：隐藏URL中的文件名
     */
    private String maskLog(String url) {
        if (url == null) return "null";
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash > 0 && lastSlash < url.length() - 1) {
            return url.substring(0, lastSlash + 1) + "***";
        }
        return "***";
    }

    /**
     * 将前端宠物类型映射为API所需类型: dog->0, cat->1, other->0(默认)
     */
    private Integer resolvePetType(String petTypeStr) {
        if ("cat".equals(petTypeStr)) return 1;
        return 0; // dog 或 other 默认为0
    }

    @Override
    public void computeHashForExisting() {
        // 新版使用在线API实时比对，无需预计算哈希，此方法保留为空实现以兼容接口
    }
}
