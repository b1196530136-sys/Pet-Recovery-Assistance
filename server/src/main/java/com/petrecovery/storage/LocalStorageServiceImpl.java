package com.petrecovery.storage;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件存储实现
 * 使用 UUID+时间戳重命名文件，防止多用户并发上传同名文件导致覆写
 */
@Service
public class LocalStorageServiceImpl implements UploadStorageService {

    @Value("${upload.local.path:./upload}")
    private String storagePath;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(storagePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory", e);
        }
    }

    @Override
    public String upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetPath = Paths.get(storagePath, newFilename);
        try {
            Files.copy(file.getInputStream(), targetPath);
            return "/upload/" + newFilename;
        } catch (IOException e) {
            throw new RuntimeException("File upload failed", e);
        }
    }

    @Override
    public boolean delete(String filePath) {
        try {
            Path path = Paths.get(storagePath).resolve(filePath.replace("/upload/", ""));
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String getStoragePath() {
        return storagePath;
    }
}
