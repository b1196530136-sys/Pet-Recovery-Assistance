package com.petrecovery.module.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.petrecovery.common.constant.UserRole;
import com.petrecovery.module.admin.dto.ReportHandleRequest;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;
import com.petrecovery.module.archive.mapper.ArchiveMapper;
import com.petrecovery.module.archive.service.ArchiveService;
import com.petrecovery.module.post.entity.PetSearchPost;
import com.petrecovery.module.post.mapper.PostMapper;
import com.petrecovery.module.message.service.MessageService;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final PostMapper postMapper;
    private final ArchiveMapper archiveMapper;
    private final UserMapper userMapper;
    private final ArchiveService archiveService;
    private final MessageService messageService;

    public AdminService(PostMapper postMapper, ArchiveMapper archiveMapper, UserMapper userManager,
                        ArchiveService archiveService, MessageService messageService) {
        this.postMapper = postMapper;
        this.archiveMapper = archiveMapper;
        this.userMapper = userManager;
        this.archiveService = archiveService;
        this.messageService = messageService;
    }

    public void reviewPost(Long postId, String action, String reason) {
        PetSearchPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new RuntimeException("寻宠启事不存在");
        }
        if (!"PENDING".equals(post.getStatus())) {
            throw new RuntimeException("该寻宠启事已处理，不能重复审核");
        }
        if (!"APPROVED".equals(action) && !"REJECTED".equals(action)) {
            throw new IllegalArgumentException("无效的审核操作");
        }
        post.setStatus("APPROVED".equals(action) ? "ACTIVE" : "REJECTED");
        if ("REJECTED".equals(action)) {
            post.setRejectReason(reason);
        } else {
            post.setRejectReason(null);
        }
        postMapper.updateById(post);
        if ("APPROVED".equals(action)) {
            messageService.sendSystemMessage(post.getUserId(), "寻宠启事审核通过",
                    "你的寻宠启事「" + displayPostName(post) + "」已通过审核，现已在寻宠大厅展示。",
                    "post", postId, "APPROVED");
        } else {
            messageService.sendSystemMessage(post.getUserId(), "寻宠启事审核未通过",
                    "你的寻宠启事「" + displayPostName(post) + "」未通过审核。原因：" + defaultReason(reason),
                    "post", postId, "REJECTED");
        }
    }

    public IPage<PetSearchPost> getPendingPosts(long page, long size) {
        return postMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<PetSearchPost>()
                        .eq(PetSearchPost::getStatus, "PENDING")
                        .orderByDesc(PetSearchPost::getCreateTime));
    }

    public void reviewArchive(Long archiveId, String action, String reason) {
        StrayAnimalArchive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new RuntimeException("档案不存在");
        }
        boolean isUpdate = archive.getPendingData() != null && !archive.getPendingData().isBlank()
                && "APPROVED".equals(archive.getStatus());
        if ("APPROVED".equals(action)) {
            archiveService.approveArchive(archiveId);
            messageService.sendSystemMessage(archive.getUserId(), isUpdate ? "档案修改审核通过" : "流浪动物档案审核通过",
                    "你的流浪动物档案「" + displayArchiveName(archive) + "」" + (isUpdate ? "修改已通过审核。" : "已通过审核，现已公开展示。"),
                    "archive", archiveId, "APPROVED");
        } else if ("REJECTED".equals(action)) {
            archiveService.rejectArchive(archiveId, reason);
            messageService.sendSystemMessage(archive.getUserId(), isUpdate ? "档案修改审核未通过" : "流浪动物档案审核未通过",
                    "你的流浪动物档案「" + displayArchiveName(archive) + "」" + (isUpdate ? "修改" : "") + "未通过审核。原因：" + defaultReason(reason),
                    "archive", archiveId, "REJECTED");
        }
    }

    public List<SysUser> getPendingCertUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, UserRole.ROLE_PENDING_CERT));
    }

    public void reviewCertification(Long userId, String action) {
        SysUser user = userMapper.selectById(userId);
        if (user == null || !UserRole.ROLE_PENDING_CERT.equals(user.getRole())) {
            return;
        }
        if ("APPROVED".equals(action)) {
            user.setRole(UserRole.ROLE_CERTIFIED);
            userMapper.updateById(user);
            messageService.sendSystemMessage(userId, "认证审核通过",
                    "你的认证申请已通过，现在可以登记和维护流浪动物档案。",
                    "certification", userId, "APPROVED");
        } else if ("REJECTED".equals(action)) {
            user.setRole(UserRole.ROLE_USER);
            userMapper.updateById(user);
            messageService.sendSystemMessage(userId, "认证审核未通过",
                    "你的认证申请未通过审核，请补充或更换认证凭证后重新提交。",
                    "certification", userId, "REJECTED");
        }
    }

    private String displayPostName(PetSearchPost post) {
        return post.getPetName() != null && !post.getPetName().isBlank() ? post.getPetName() : "未命名宠物";
    }

    private String displayArchiveName(StrayAnimalArchive archive) {
        if (archive.getName() != null && !archive.getName().isBlank()) {
            return archive.getName();
        }
        return archive.getAnimalType() != null ? archive.getAnimalType() : "未命名动物";
    }

    private String defaultReason(String reason) {
        return reason != null && !reason.isBlank() ? reason : "暂未填写具体原因，请根据平台规范补充信息后重新提交。";
    }

    // ===== 用户权限管理 =====
    public List<SysUser> getAllUsers() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .ne(SysUser::getRole, UserRole.ROLE_ADMIN)
                .orderByAsc(SysUser::getId));
    }

    public void updateUserRole(Long userId, String newRole) {
        if (!Arrays.asList(UserRole.ROLE_USER, UserRole.ROLE_CERTIFIED, UserRole.ROLE_PENDING_CERT).contains(newRole)) {
            throw new IllegalArgumentException("无效的角色: " + newRole);
        }
        SysUser user = userMapper.selectById(userId);
        if (user != null && !UserRole.ROLE_ADMIN.equals(user.getRole())) {
            user.setRole(newRole);
            userMapper.updateById(user);
        }
    }

    public void banUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        if (UserRole.ROLE_ADMIN.equals(user.getRole())) throw new RuntimeException("不能封禁管理员");
        user.setStatus(0);
        userMapper.updateById(user);
    }

    public void unbanUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setStatus(1);
        userMapper.updateById(user);
    }

    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        Long pendingPosts = postMapper.selectCount(new LambdaQueryWrapper<PetSearchPost>()
                .eq(PetSearchPost::getStatus, "PENDING"));
        Long pendingArchives = archiveMapper.selectCount(new LambdaQueryWrapper<StrayAnimalArchive>()
                .and(w -> w.eq(StrayAnimalArchive::getStatus, "PENDING")
                        .or()
                        .isNotNull(StrayAnimalArchive::getPendingData)));
        Long pendingCertifications = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRole, UserRole.ROLE_PENDING_CERT));
        Long pendingReports = (long) messageService.getPendingReports().size();
        data.put("totalUsers", userMapper.selectCount(null));
        data.put("activePosts", postMapper.selectCount(new LambdaQueryWrapper<PetSearchPost>()
                .eq(PetSearchPost::getStatus, "ACTIVE")));
        data.put("resolvedPosts", postMapper.selectCount(new LambdaQueryWrapper<PetSearchPost>()
                .eq(PetSearchPost::getStatus, "RESOLVED")));
        data.put("totalArchives", archiveMapper.selectCount(new LambdaQueryWrapper<StrayAnimalArchive>()
                .eq(StrayAnimalArchive::getStatus, "APPROVED")));
        data.put("pendingPosts", pendingPosts);
        data.put("pendingArchives", pendingArchives);
        data.put("pendingCertifications", pendingCertifications);
        data.put("pendingReports", pendingReports);
        data.put("pendingTotal", pendingPosts + pendingArchives + pendingCertifications + pendingReports);
        return data;
    }

    public Object getPendingReports() {
        return messageService.getPendingReports();
    }

    public Object getAllReports() {
        return messageService.getAllReports();
    }

    public void handleReport(Long reportId, Long adminUserId, ReportHandleRequest request) {
        if (request == null || request.getAction() == null) {
            throw new IllegalArgumentException("处理参数不完整");
        }
        messageService.handleReport(
                reportId,
                adminUserId,
                request.getAction(),
                request.getHandleNote(),
                Boolean.TRUE.equals(request.getBanReportedUser())
        );
    }

    public void exportArchiveExcel(String startDate, String endDate, String region,
                                   HttpServletResponse response) {
        LambdaQueryWrapper<StrayAnimalArchive> wrapper = new LambdaQueryWrapper<StrayAnimalArchive>()
                .eq(StrayAnimalArchive::getStatus, "APPROVED")
                .orderByDesc(StrayAnimalArchive::getCreateTime);
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(StrayAnimalArchive::getCreateTime, parseStartDate(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(StrayAnimalArchive::getCreateTime, parseEndDate(endDate));
        }
        if (region != null && !region.isBlank()) {
            wrapper.like(StrayAnimalArchive::getAddress, region);
        }
        List<StrayAnimalArchive> list = archiveMapper.selectList(wrapper);
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

    private LocalDateTime parseStartDate(String value) {
        return LocalDate.parse(value.trim()).atStartOfDay();
    }

    private LocalDateTime parseEndDate(String value) {
        return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay().minusNanos(1);
    }
}
