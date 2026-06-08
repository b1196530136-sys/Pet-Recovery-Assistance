/*
 * ===== 以下为原图像识别（感知哈希）相关代码，已注释 =====
package com.petrecovery.common.util;

import net.coobird.thumbnailator.Thumbnails;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public class ImageHashUtil {

    private static final int HASH_SIZE = 8;

    public static String computeHash(File imageFile) {
        try {
            BufferedImage img = Thumbnails.of(imageFile)
                    .size(HASH_SIZE, HASH_SIZE)
                    .keepAspectRatio(false)
                    .asBufferedImage();
            return computeHashFromImage(img);
        } catch (Exception e) {
            throw new RuntimeException("图片处理失败", e);
        }
    }

    public static String computeHash(byte[] imageBytes) {
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage img = Thumbnails.of(is)
                    .size(HASH_SIZE, HASH_SIZE)
                    .keepAspectRatio(false)
                    .asBufferedImage();
            return computeHashFromImage(img);
        } catch (Exception e) {
            throw new RuntimeException("图片处理失败", e);
        }
    }

    private static String computeHashFromImage(BufferedImage img) {
        int w = Math.min(img.getWidth(), HASH_SIZE);
        int h = Math.min(img.getHeight(), HASH_SIZE);
        int[] gray = new int[w * h];
        int idx = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                gray[idx++] = (r * 77 + g * 151 + b * 28) >> 8;
            }
        }

        long sum = 0;
        int total = w * h;
        for (int v : gray) sum += v;
        int avg = total > 0 ? (int) (sum / total) : 0;

        StringBuilder hash = new StringBuilder();
        for (int v : gray) {
            hash.append(v >= avg ? '1' : '0');
        }
        return hash.toString();
    }

    public static int hammingDistance(String hash1, String hash2) {
        int len = Math.min(hash1.length(), hash2.length());
        int dist = 0;
        for (int i = 0; i < len; i++) {
            if (hash1.charAt(i) != hash2.charAt(i)) {
                dist++;
            }
        }
        dist += Math.abs(hash1.length() - hash2.length()) * 8;
        return dist;
    }

    public static double similarity(String hash1, String hash2) {
        int maxDist = Math.max(hash1.length(), hash2.length());
        if (maxDist == 0) return 100.0;
        return (1.0 - (double) hammingDistance(hash1, hash2) / maxDist) * 100;
    }
}
* ===== 原图像识别代码注释结束 =====
*/
