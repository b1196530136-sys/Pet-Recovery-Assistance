package com.petrecovery.module.stats.controller;

import com.petrecovery.common.Result;
import com.petrecovery.module.admin.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final AdminService adminService;

    public StatsController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public Result<?> dashboard() {
        return Result.success(adminService.getDashboardData());
    }
}
