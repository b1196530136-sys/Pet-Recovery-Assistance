package com.petrecovery.module.upload.controller;

import com.petrecovery.common.Result;
import com.petrecovery.storage.UploadStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private final UploadStorageService uploadStorageService;

    public UploadController(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @PostMapping("/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            return Result.error("仅支持上传 JPG/PNG/GIF/WebP/BMP 格式的图片文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("图片大小不能超过 5MB");
        }
        String url = uploadStorageService.upload(file);
        return Result.success(url);
    }
}
