package com.petrecovery.module.upload.controller;

import com.petrecovery.common.Result;
import com.petrecovery.storage.UploadStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadStorageService uploadStorageService;

    public UploadController(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @PostMapping("/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        String url = uploadStorageService.upload(file);
        return Result.success(url);
    }
}
