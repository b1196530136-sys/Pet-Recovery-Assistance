package com.petrecovery.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传存储抽象接口
 * 遵循面向接口编程原则，支持本地存储与云存储无缝切换
 */
public interface UploadStorageService {

    /**
     * 上传文件并返回可访问路径
     */
    String upload(MultipartFile file);

    /**
     * 删除文件
     */
    boolean delete(String filePath);

    /**
     * 获取存储根路径
     */
    String getStoragePath();
}
