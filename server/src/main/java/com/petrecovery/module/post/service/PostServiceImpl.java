package com.petrecovery.module.post.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.common.util.ImageHashUtil;
import com.petrecovery.module.message.entity.SysImMessage;
import com.petrecovery.module.message.mapper.MessageMapper;
import com.petrecovery.module.post.dto.PostSearchRequest;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.mapper.PostMapper;
import com.petrecovery.storage.UploadStorageService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, PetSearchPost> implements PostService {

    private static final double SIMILARITY_THRESHOLD = 60.0;

    private final MessageMapper messageMapper;
    private final UploadStorageService uploadStorageService;

    public PostServiceImpl(MessageMapper messageMapper, UploadStorageService uploadStorageService) {
        this.messageMapper = messageMapper;
        this.uploadStorageService = uploadStorageService;
    }

    @Override
    public PetSearchPost createPost(PetSearchPost post, Long userId) {
        post.setUserId(userId);
        post.setStatus("PENDING");
        // 如果包含照片，计算感知哈希
        if (post.getPhotos() != null && !post.getPhotos().isEmpty()) {
            String[] photoUrls = post.getPhotos().split(",");
            String[] hashes = new String[photoUrls.length];
            String storagePath = uploadStorageService.getStoragePath();
            for (int i = 0; i < photoUrls.length; i++) {
                String url = photoUrls[i].trim();
                String filename = url.replace("/upload/", "");
                File imgFile = Paths.get(storagePath, filename).toFile();
                if (imgFile.exists()) {
                    try {
                        hashes[i] = ImageHashUtil.computeHash(imgFile);
                    } catch (Exception e) {
                        hashes[i] = "";
                    }
                }
            }
            post.setPhotoHash(String.join(",", hashes));
        }
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
    public List<Map<String, Object>> getClueTrail(Long postId) {
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

    @Override
    public List<Map<String, Object>> searchByImage(byte[] imageBytes) {
        // 计算上传图片的哈希
        String uploadedHash = ImageHashUtil.computeHash(imageBytes);

        // 获取所有已审核通过的帖子
        List<PetSearchPost> allPosts = list(new LambdaQueryWrapper<PetSearchPost>()
                .in(PetSearchPost::getStatus, "ACTIVE", "RESOLVED")
                .isNotNull(PetSearchPost::getPhotoHash)
                .ne(PetSearchPost::getPhotoHash, ""));

        // 逐一比对哈希值，找出相似度 >= 60% 的
        List<Map<String, Object>> results = new ArrayList<>();
        for (PetSearchPost post : allPosts) {
            String[] postHashes = post.getPhotoHash().split(",");
            double bestSim = 0;
            for (String hash : postHashes) {
                if (hash == null || hash.isEmpty()) continue;
                double sim = ImageHashUtil.similarity(uploadedHash, hash);
                if (sim > bestSim) bestSim = sim;
            }
            if (bestSim >= SIMILARITY_THRESHOLD) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", post.getId());
                item.put("petName", post.getPetName());
                item.put("petType", post.getPetType());
                item.put("photos", post.getPhotos());
                item.put("address", post.getAddress());
                item.put("lostTime", post.getLostTime());
                item.put("description", post.getDescription());
                item.put("status", post.getStatus());
                item.put("similarity", Math.round(bestSim * 10.0) / 10.0);
                results.add(item);
            }
        }

        // 按相似度降序排列
        results.sort((a, b) -> Double.compare((double) b.get("similarity"), (double) a.get("similarity")));
        return results;
    }

    @Override
    public void computeHashForExisting() {
        String storagePath = uploadStorageService.getStoragePath();
        List<PetSearchPost> allPosts = list(new LambdaQueryWrapper<PetSearchPost>()
                .isNotNull(PetSearchPost::getPhotos)
                .ne(PetSearchPost::getPhotos, ""));
        int updated = 0;
        for (PetSearchPost post : allPosts) {
            if (post.getPhotoHash() != null && !post.getPhotoHash().isEmpty()) continue;
            String[] photoUrls = post.getPhotos().split(",");
            List<String> hashes = new ArrayList<>();
            for (String url : photoUrls) {
                String filename = url.trim().replace("/upload/", "");
                File imgFile = Paths.get(storagePath, filename).toFile();
                if (imgFile.exists()) {
                    try {
                        hashes.add(ImageHashUtil.computeHash(imgFile));
                    } catch (Exception e) {
                        hashes.add("");
                    }
                }
            }
            String hashStr = String.join(",", hashes);
            if (!hashStr.isEmpty()) {
                post.setPhotoHash(hashStr);
                updateById(post);
                updated++;
            }
        }
    }
}
