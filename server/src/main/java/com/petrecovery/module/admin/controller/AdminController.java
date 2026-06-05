package com.petrecovery.module.admin.controller;

import com.petrecovery.common.Result;
import com.petrecovery.module.admin.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("无管理员权限");
        }
    }

    // ===== 寻宠启事审核 =====
    @PostMapping("/post/review/{postId}")
    public Result<?> reviewPost(@PathVariable Long postId, @RequestParam String action,
                                @RequestParam(required = false) String reason,
                                HttpServletRequest request) {
        checkAdmin(request);
        adminService.reviewPost(postId, action, reason);
        return Result.success();
    }

    // ===== 档案审核 =====
    @PostMapping("/archive/review/{archiveId}")
    public Result<?> reviewArchive(@PathVariable Long archiveId, @RequestParam String action,
                                   @RequestParam(required = false) String reason,
                                   HttpServletRequest request) {
        checkAdmin(request);
        adminService.reviewArchive(archiveId, action, reason);
        return Result.success();
    }

    // ===== 认证审批 =====
    @GetMapping("/certification/pending")
    public Result<?> pendingCertifications(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(adminService.getPendingCertUsers());
    }

    @PostMapping("/certification/review/{userId}")
    public Result<?> reviewCertification(@PathVariable Long userId, @RequestParam String action,
                                         HttpServletRequest request) {
        checkAdmin(request);
        adminService.reviewCertification(userId, action);
        return Result.success();
    }

    // ===== 用户权限管理 =====
    @GetMapping("/users")
    public Result<?> users(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(adminService.getAllUsers());
    }

    @PostMapping("/user/role/{userId}")
    public Result<?> updateUserRole(@PathVariable Long userId, @RequestParam String role,
                                     HttpServletRequest request) {
        checkAdmin(request);
        adminService.updateUserRole(userId, role);
        return Result.success();
    }

    @PostMapping("/user/ban/{userId}")
    public Result<?> banUser(@PathVariable Long userId, HttpServletRequest request) {
        checkAdmin(request);
        adminService.banUser(userId);
        return Result.success();
    }

    @PostMapping("/user/unban/{userId}")
    public Result<?> unbanUser(@PathVariable Long userId, HttpServletRequest request) {
        checkAdmin(request);
        adminService.unbanUser(userId);
        return Result.success();
    }

    // ===== 数据大盘 =====
    @GetMapping("/dashboard")
    public Result<?> dashboard(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(adminService.getDashboardData());
    }

    // ===== 台账导出 =====
    @GetMapping("/export/archive")
    public void exportArchive(@RequestParam(required = false) String startDate,
                              @RequestParam(required = false) String endDate,
                              @RequestParam(required = false) String region,
                              HttpServletResponse response,
                              HttpServletRequest request) {
        checkAdmin(request);
        adminService.exportArchiveExcel(startDate, endDate, region, response);
    }

    // ===== 寻宠案例导出 =====
    @GetMapping("/export/resolved-posts")
    public void exportResolvedPosts(HttpServletResponse response,
                                    HttpServletRequest request) {
        checkAdmin(request);
        adminService.exportResolvedPostsExcel(response);
    }
}
