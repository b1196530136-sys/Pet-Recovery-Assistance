package com.petrecovery.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 快瞳宠物图像相似度对比接口调用工具
 * 接口地址: https://ai.inspirvision.cn/s/api/recognitionComparedFromPath
 * Token获取: https://ai.inspirvision.cn/s/api/getAccessToken (GET, accessKey + accessSecret)
 *
 * 调用流程（无需内网穿透）：
 * 本地图片文件 → 上传到免费图床(ImgBB)获取公网URL → 传给快瞳API进行比对
 */
@Slf4j
@Component
public class PetImageRecognitionUtil {

    // ==================== 快瞳API配置 ====================

    @Value("${pet-recognition.api-url}")
    private String apiUrl;

    @Value("${pet-recognition.token-url}")
    private String tokenUrl;

    @Value("${pet-recognition.access-key}")
    private String accessKey;

    @Value("${pet-recognition.access-secret}")
    private String accessSecret;

    @Value("${pet-recognition.recog-type:0}")
    private int recogType;

    @Value("${pet-recognition.default-pet-type:0}")
    private int defaultPetType;

    @Value("${pet-recognition.similarity-threshold:60}")
    private double similarityThreshold;

    /** 本地文件存储路径 */
    @Value("${upload.local.path:./upload}")
    private String storagePath;

    // ==================== 图床(ImgBB)配置 ====================

    @Value("${image-hosting.imgbb.api-url:https://api.imgbb.com/1/upload}")
    private String imgbbApiUrl;

    @Value("${image-hosting.imgbb.api-key}")
    private String imgbbApiKey;

    @Value("${image-hosting.imgbb.expiration:604800}")
    private long imgbbExpiration;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== Token 缓存管理 ====================

    private volatile String cachedToken;
    private volatile Instant tokenAcquiredAt;
    private volatile long tokenExpireSeconds = 604800;

    private final ReentrantLock tokenLock = new ReentrantLock();

    /**
     * 获取有效的Token（带自动缓存和过期刷新）
     */
    public String getEffectiveToken() {
        if (isTokenValid()) return cachedToken;
        tokenLock.lock();
        try {
            if (isTokenValid()) return cachedToken;
            log.info("快瞳Token已过期或未初始化，正在重新获取...");
            cachedToken = fetchNewToken();
            tokenAcquiredAt = Instant.now();
            log.info("快瞳Token获取成功，已缓存");
            return cachedToken;
        } finally { tokenLock.unlock(); }
    }

    private boolean isTokenValid() {
        if (cachedToken == null || cachedToken.isBlank()) return false;
        if (tokenAcquiredAt == null) return false;
        long elapsed = Instant.now().getEpochSecond() - tokenAcquiredAt.getEpochSecond();
        return elapsed < (tokenExpireSeconds - 300);
    }

    private String fetchNewToken() {
        if (accessKey == null || accessKey.isBlank() || "YOUR_ACCESS_KEY_HERE".equals(accessKey)) {
            throw new IllegalStateException("快瞳API凭证缺失：未配置 pet-recognition.access-key");
        }
        if (accessSecret == null || accessSecret.isBlank() || "YOUR_ACCESS_SECRET_HERE".equals(accessSecret)) {
            throw new IllegalStateException("快瞳API凭证缺失：未配置 pet-recognition.access-secret");
        }
        try {
            String requestUrl = String.format("%s?accessKey=%s&accessSecret=%s",
                    tokenUrl,
                    java.net.URLEncoder.encode(accessKey, "UTF-8"),
                    java.net.URLEncoder.encode(accessSecret, "UTF-8"));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(requestUrl)).timeout(Duration.ofSeconds(15)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (response.statusCode() != 200) {
                throw new RuntimeException("Token获取HTTP错误: " + response.statusCode() +
                        (body != null ? " (" + truncateBody(body, 200) + ")" : ""));
            }
            JsonNode root = objectMapper.readTree(body);
            String status = root.has("status") ? root.get("status").asText() : "";
            if (!"200".equals(status)) {
                String msg = root.has("message") ? root.get("message").asText() : "未知错误";
                throw new RuntimeException("Token获取失败(" + status + "): " + msg);
            }
            String token;
            JsonNode data = root.get("data");
            if (data != null && !data.isNull() && data.has("access_token")) {
                token = data.get("access_token").asText();
                if (data.has("expires_in")) {
                    long exp = data.get("expires_in").asLong();
                    if (exp > 0) { tokenExpireSeconds = exp; }
                }
            } else if (data != null && !data.isNull() && data.isTextual()) {
                token = data.asText();
            } else if (root.has("token")) {
                token = root.get("token").asText();
            } else {
                throw new RuntimeException("Token获取成功但无法提取token字段");
            }
            if (token == null || token.isBlank()) throw new RuntimeException("Token获取返回值为空");
            return token;
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("获取快瞳Token失败: " + e.getMessage(), e); }
    }

    public void invalidateTokenCache() {
        tokenLock.lock();
        try { cachedToken = null; tokenAcquiredAt = null; log.info("快瞳Token缓存已清除"); }
        finally { tokenLock.unlock(); }
    }

    // ==================== 图片路径解析与Base64转换 ====================

    /** 将相对路径（如 /upload/xxx.jpg）解析为本地文件绝对路径 */
    public Path resolveLocalPath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return null;
        String trimmed = relativePath.trim();
        if (trimmed.startsWith("/upload/")) {
            String fileName = trimmed.substring("/upload/".length());
            return Paths.get(storagePath, fileName);
        }
        return Paths.get(trimmed);
    }

    /** 读取本地图片文件并转换为base64编码字符串 */
    public String imageFileToBase64(String relativePath) throws IOException {
        Path filePath = resolveLocalPath(relativePath);
        if (filePath == null || !Files.exists(filePath)) {
            throw new IOException("图片文件不存在: " + filePath);
        }
        byte[] fileBytes = Files.readAllBytes(filePath);
        return Base64.getEncoder().encodeToString(fileBytes);
    }

    /** 将字节数组转换为base64编码字符串 */
    public String bytesToBase64(byte[] imageBytes) {
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    // ==================== ImgBB 图床上传 ====================

    /** 检查图床是否可用（API Key是否已配置） */
    public boolean isImageHostingAvailable() {
        return imgbbApiKey != null && !imgbbApiKey.isBlank() && !"YOUR_IMGBB_API_KEY_HERE".equals(imgbbApiKey);
    }

    /**
     * 上传图片到ImgBB图床，返回公网可访问的图片URL
     *
     * ImgBB API文档：POST https://api.imgbb.com/1/upload
     * 参数：
     *   key      - API Key
     *   image    - 图片数据（支持base64编码）
     *   expiration - 过期时间（秒），可选
     *
     * 响应格式：
     *   {"success": true, "data": {"url": "https://i.ibb.co/xxx.jpg", ...}}
     *
     * @param base64Image 图片的base64编码（不含data:image前缀）
     * @return 公网图片URL
     */
    public String uploadToImageHosting(String base64Image) {
        if (!isImageHostingAvailable()) {
            throw new IllegalStateException(
                    "图床服务不可用：未配置 image-hosting.imgbb.api-key。" +
                    "请在 application.yml 中填写ImgBB API Key（免费申请：https://imgbb.com/api）。" +
                    "以图搜宠功能需要通过图床将本地图片转为公网URL后传给快瞳API。");
        }
        if (base64Image == null || base64Image.isBlank()) {
            throw new IllegalArgumentException("图片base64数据不能为空");
        }

        try {
            // 构建multipart/form-data请求体
            // ImgBB接受标准的multipart/form-data格式上传
            String boundary = "----ImgBBUpload" + System.currentTimeMillis();

            StringBuilder sb = new StringBuilder();
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"key\"\r\n\r\n");
            sb.append(imgbbApiKey).append("\r\n");

            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"image\"\r\n\r\n");
            sb.append(base64Image).append("\r\n");

            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"expiration\"\r\n\r\n");
            sb.append(imgbbExpiration).append("\r\n");

            sb.append("--").append(boundary).append("--\r\n");

            byte[] requestBody = sb.toString().getBytes("UTF-8");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imgbbApiUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            log.debug("ImgBB上传响应: status={}, bodyLength={}", response.statusCode(),
                    body != null ? body.length() : 0);

            if (response.statusCode() != 200) {
                throw new RuntimeException("ImgBB图床上传HTTP错误: " + response.statusCode() +
                        (body != null ? " (" + truncateBody(body, 300) + ")" : ""));
            }

            JsonNode root = objectMapper.readTree(body);

            // 检查success字段
            boolean success = root.has("success") && root.get("success").asBoolean(false);
            if (!success) {
                JsonNode errorArr = root.get("error");
                String errMsg = errorArr != null ? errorArr.toString() : "未知错误";
                throw new RuntimeException("ImgBB图床上传失败: " + errMsg);
            }

            // 提取返回的图片URL
            JsonNode data = root.get("data");
            if (data == null || !data.has("url")) {
                throw new RuntimeException("ImgBB响应中缺少url字段，原始响应: " + truncateBody(body, 500));
            }

            String imageUrl = data.get("url").asText();
            log.info("图片已上传至图床: {}", maskUrl(imageUrl));
            return imageUrl;

        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("图床上传失败: " + e.getMessage(), e); }
    }

    // ==================== 核心调用方法（推荐：自动走图床）====================

    /**
     * 比较两张宠物图片的相似度（通过本地文件路径）
     *
     * 完整调用链路：
     * 1. 读取两张本地图片 → 转 base64
     * 2. 上传到 ImgBB 图床 → 获取公网 URL
     * 3. 用公网 URL 调用快瞳 API 进行相似度比对
     *
     * 无需内网穿透，只需配置好 ImgBB API Key 即可
     *
     * @param localPath0 第一张图片的本地相对路径（如 /upload/xxx.jpg）
     * @param localPath1 第二张图片的本地相对路径
     * @param petType 宠物类型：0=狗, 1=猫
     */
    public RecognitionResult compareImagesByPath(String localPath0, String localPath1, Integer petType) throws IOException {
        // 步骤1：读取两张图片并转base64
        log.info("以图搜宠 - 开始处理图片: path0={}, path1={}", localPath0, localPath1);
        String base64_0 = imageFileToBase64(localPath0);
        String base64_1 = imageFileToBase64(localPath1);
        log.info("以图搜宠 - 图片转base64完成: size0={}bytes, size1={}bytes", base64_0.length(), base64_1.length());

        // 步骤2：上传到ImgBB图床获取公网URL
        String url0 = uploadToImageHosting(base64_0);
        String url1 = uploadToImageHosting(base64_1);
        log.info("以图搜宠 - 图床URL获取完成: url0={}, url1={}", maskUrl(url0), maskUrl(url1));

        // 步骤3：用公网URL调用快瞳API
        return compareImagesByUrl(url0, url1, petType);
    }

    /**
     * 比较两张宠物图片的相似度（通过字节数组）
     * 流程：base64 → 上传图床 → 调用快瞳API
     */
    public RecognitionResult compareImagesByBytes(byte[] imageBytes0, byte[] imageBytes1, Integer petType) throws IOException {
        String base64_0 = bytesToBase64(imageBytes0);
        String base64_1 = bytesToBase64(imageBytes1);
        String url0 = uploadToImageHosting(base64_0);
        String url1 = uploadToImageHosting(base64_1);
        return compareImagesByUrl(url0, url1, petType);
    }

    // ==================== 核心调用方法（直接URL方式）====================

    /**
     * 比较两张宠物图片的相似度（直接传入公网URL调用快瞳API）
     * 此方法需要传入的URL是公网可访问的完整地址
     */
    public RecognitionResult compareImagesByUrl(String fileUrl0, String fileUrl1, Integer petType) {
        validateUrl(fileUrl0, "fileUrl0");
        validateUrl(fileUrl1, "fileUrl1");

        int actualPetType = (petType != null) ? petType : defaultPetType;
        String effectiveToken = getEffectiveToken();

        log.info("调用快瞳宠物图像识别API(URL模式): fileUrl0={}, fileUrl1={}, recogType={}, petType={}",
                maskUrl(fileUrl0), maskUrl(fileUrl1), recogType, actualPetType);

        try {
            String jsonBody = buildJsonRequestBody(effectiveToken, fileUrl0, fileUrl1, actualPetType);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return parseApiResponse(response, fileUrl0 + " / " + fileUrl1);

        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("图像识别服务调用失败: " + e.getMessage(), e); }
    }

    private void validateUrl(String url, String fieldName) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(fieldName + " 不能为空");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException(fieldName + " 必须为完整的HTTP(S) URL: " + url);
        }
    }

    // ==================== 请求体构建 ====================

    /** 构建JSON请求体（URL模式 - 快瞳API标准格式）*/
    private String buildJsonRequestBody(String token, String fileUrl0, String fileUrl1, int petType) {
        StringBuilder json = new StringBuilder("{");
        json.append("\"token\":\"").append(escapeJson(token)).append("\",");
        json.append("\"fileUrl0\":\"").append(escapeJson(fileUrl0)).append("\",");
        json.append("\"fileUrl1\":\"").append(escapeJson(fileUrl1)).append("\",");
        json.append("\"petType\":").append(petType).append(",");
        json.append("\"recogType\":").append(recogType);
        json.append("}");
        return json.toString();
    }

    // ==================== 响应解析 ====================

    /** 解析快瞳API的统一响应 */
    private RecognitionResult parseApiResponse(HttpResponse<String> response, String sourceLabel) throws Exception {
        String body = response.body();
        int httpStatus = response.statusCode();
        log.info("快瞳图像识别接口响应({}): status={}, bodyLength={}", sourceLabel, httpStatus,
                body != null ? body.length() : 0);

        if (httpStatus != 200) {
            if (httpStatus == 401) { invalidateTokenCache(); }
            throw new RuntimeException("图像识别接口HTTP错误: " + httpStatus +
                    (body != null && !body.isEmpty() ? " (" + truncateBody(body, 200) + ")" : ""));
        }

        JsonNode root = objectMapper.readTree(body);
        String status = root.has("status") ? root.get("status").asText() : "";
        if (!"200".equals(status)) {
            String msg = root.has("message") ? root.get("message").asText() :
                         (root.has("code") ? "business_code_" + root.get("code").asText() : "unknown_error");
            if (msg.contains("token") || msg.contains("Token") || msg.contains("认证")) { invalidateTokenCache(); }
            throw new RuntimeException("图像识别接口错误: " + msg);
        }

        JsonNode data = root.get("data");
        if (data == null || data.isNull() || data.isEmpty()) {
            throw new RuntimeException("图像识别接口返回数据为空");
        }

        RecognitionResult result = new RecognitionResult();
        result.setConfidence(data.has("confidence") ? data.get("confidence").asDouble() : 0.0);
        result.setPetType(data.has("petType") ? data.get("petType").asText() : "");
        result.setDescription(data.has("description") ? data.get("description").asText() : "");
        result.setQuality(data.has("quality") ? data.get("quality").asText() : "");

        log.info("图像识别结果: confidence={}, description={}",
                result.getConfidence(), result.getDescription());
        return result;
    }

    // ==================== 工具方法 ====================

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
    }

    private String maskUrl(String url) {
        if (url == null) return "null";
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            if (path != null && path.contains("/")) {
                int lastSlash = path.lastIndexOf('/');
                String masked = path.substring(0, lastSlash + 1) + "***";
                return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(),
                        uri.getPort(), masked, uri.getQuery(), uri.getFragment()).toString();
            }
        } catch (Exception ignored) {}
        return url;
    }

    private static String truncateBody(String str, int maxLen) {
        if (str == null) return "null";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...(共" + str.length() + "字符)";
    }

    public boolean isSimilarEnough(double confidence) { return confidence >= similarityThreshold; }
    public double getSimilarityThreshold() { return similarityThreshold; }

    // ==================== 结果封装类 ====================

    public static class RecognitionResult {
        private double confidence;
        private String petType;
        private String description;
        private String quality;

        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getPetType() { return petType; }
        public void setPetType(String petType) { this.petType = petType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getQuality() { return quality; }
        public void setQuality(String quality) { this.quality = quality; }
    }
}
