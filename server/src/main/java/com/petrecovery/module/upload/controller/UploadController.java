package com.petrecovery.module.upload.controller;

import com.petrecovery.common.Result;
import com.petrecovery.storage.UploadStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private final UploadStorageService uploadStorageService;

    public UploadController(UploadStorageService uploadStorageService) {
        this.uploadStorageService = uploadStorageService;
    }

    @PostMapping("/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("图片大小不能超过 5MB");
        }
        String extension;
        try {
            extension = detectImageExtension(file);
        } catch (IOException e) {
            return Result.error("图片读取失败");
        }
        if (extension == null) {
            return Result.error("仅支持上传 JPG/PNG/GIF/WebP/BMP 格式的真实图片文件");
        }
        String url = uploadStorageService.upload(file, extension);
        return Result.success(url);
    }

    private String detectImageExtension(MultipartFile file) throws IOException {
        byte[] header = file.getInputStream().readNBytes(12);
        String extension = detectByHeader(header);
        if (extension == null) return null;
        if (".webp".equals(extension)) {
            return extension;
        }
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            return image != null && image.getWidth() > 0 && image.getHeight() > 0 ? extension : null;
        } catch (IOException e) {
            return null;
        }
    }

    private String detectByHeader(byte[] header) {
        if (header.length >= 3
                && (header[0] & 0xff) == 0xff
                && (header[1] & 0xff) == 0xd8
                && (header[2] & 0xff) == 0xff) return ".jpg";
        if (header.length >= 8
                && (header[0] & 0xff) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4e
                && header[3] == 0x47
                && header[4] == 0x0d
                && header[5] == 0x0a
                && header[6] == 0x1a
                && header[7] == 0x0a) return ".png";
        if (header.length >= 6
                && header[0] == 0x47
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x38
                && (header[4] == 0x37 || header[4] == 0x39)
                && header[5] == 0x61) return ".gif";
        if (header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50) return ".webp";
        if (header.length >= 2 && header[0] == 0x42 && header[1] == 0x4d) return ".bmp";
        return null;
    }
}
