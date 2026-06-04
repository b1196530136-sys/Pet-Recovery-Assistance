package com.petrecovery.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.petrecovery.common.constant.UserRole;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;
import com.petrecovery.module.archive.mapper.ArchiveMapper;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.mapper.PostMapper;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final PostMapper postMapper;
    private final ArchiveMapper archiveMapper;
    private final UserMapper userMapper;

    public AdminService(PostMapper postMapper, ArchiveMapper archiveMapper, UserMapper userMapper) {
        this.postMapper = postMapper;
        this.archiveMapper = archiveMapper;
        this.userMapper = userMapper;
    }

    public void reviewPost(Long postId, String action, String reason) {
        PetSearchPost post = postMapper.selectById(postId);
        if (post != null) {
            post.setStatus("APPROVED".equals(action) ? "ACTIVE" : "REJECTED");
            if ("REJECTED".equals(action)) {
                post.setRejectReason(reason);
            }
            postMapper.updateById(post);
        }
    }

    public void reviewArchive(Long archiveId, String action, String reason) {
        StrayAnimalArchive archive = archiveMapper.selectById(archiveId);
        if (archive != null) {
            archive.setStatus("APPROVED".equals(action) ? "APPROVED" : "REJECTED");
            if ("REJECTED".equals(action)) {
                archive.setRejectReason(reason);
            }
            archiveMapper.updateById(archive);
        }
    }

    public List<SysUser> getPendingCertUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, UserRole.ROLE_PENDING_CERT));
    }

    public void reviewCertification(Long userId, String action) {
        SysUser user = userMapper.selectById(userId);
        if (user != null && "APPROVED".equals(action)
                && UserRole.ROLE_PENDING_CERT.equals(user.getRole())) {
            user.setRole(UserRole.ROLE_CERTIFIED);
            userMapper.updateById(user);
        }
    }

    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("activePosts", postMapper.selectCount(new LambdaQueryWrapper<PetSearchPost>()
                .eq(PetSearchPost::getStatus, "ACTIVE")));
        data.put("resolvedPosts", postMapper.selectCount(new LambdaQueryWrapper<PetSearchPost>()
                .eq(PetSearchPost::getStatus, "RESOLVED")));
        data.put("totalArchives", archiveMapper.selectCount(new LambdaQueryWrapper<StrayAnimalArchive>()
                .eq(StrayAnimalArchive::getStatus, "APPROVED")));
        return data;
    }

    public void exportArchiveExcel(String startDate, String endDate, String region,
                                   HttpServletResponse response) {
        List<StrayAnimalArchive> list = archiveMapper.selectList(null);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("流浪动物登记台账");
            Row header = sheet.createRow(0);
            String[] columns = {"ID", "动物类型", "健康状况", "安置状态", "经度", "纬度", "建档时间"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            int rowNum = 1;
            for (StrayAnimalArchive a : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getId());
                row.createCell(1).setCellValue(a.getAnimalType());
                row.createCell(2).setCellValue(a.getHealthStatus());
                row.createCell(3).setCellValue(a.getPlacementStatus());
                row.createCell(4).setCellValue(a.getLongitude() != null ? a.getLongitude().doubleValue() : 0);
                row.createCell(5).setCellValue(a.getLatitude() != null ? a.getLatitude().doubleValue() : 0);
                row.createCell(6).setCellValue(a.getCreateTime() != null ? a.getCreateTime().toString() : "");
            }
            writeExcelResponse(response, workbook, "流浪动物登记台账.xlsx");
        } catch (IOException e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    public void exportResolvedPostsExcel(HttpServletResponse response) {
        List<PetSearchPost> list = postMapper.selectList(
                new LambdaQueryWrapper<PetSearchPost>().eq(PetSearchPost::getStatus, "RESOLVED"));
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("寻宠成功案例统计报表");
            Row header = sheet.createRow(0);
            String[] columns = {"ID", "宠物名称", "类型", "丢失时间", "发布时间"};
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            int rowNum = 1;
            for (PetSearchPost p : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getPetName());
                row.createCell(2).setCellValue(p.getPetType());
                row.createCell(3).setCellValue(p.getLostTime() != null ? p.getLostTime().toString() : "");
                row.createCell(4).setCellValue(p.getCreateTime() != null ? p.getCreateTime().toString() : "");
            }
            writeExcelResponse(response, workbook, "寻宠成功案例统计报表.xlsx");
        } catch (IOException e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    private void writeExcelResponse(HttpServletResponse response, Workbook workbook, String filename)
            throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
        workbook.write(response.getOutputStream());
        response.getOutputStream().flush();
    }
}
