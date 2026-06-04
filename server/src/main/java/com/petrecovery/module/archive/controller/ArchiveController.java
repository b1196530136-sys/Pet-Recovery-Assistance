package com.petrecovery.module.archive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.petrecovery.common.Result;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;
import com.petrecovery.module.archive.service.ArchiveService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/archive")
public class ArchiveController {

    private final ArchiveService archiveService;

    public ArchiveController(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    @PostMapping("/create")
    public Result<?> create(@RequestBody StrayAnimalArchive archive, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(archiveService.createArchive(archive, userId));
    }

    @GetMapping("/search")
    public Result<?> search(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) String animalType,
                            @RequestParam(required = false) String placementStatus) {
        LambdaQueryWrapper<StrayAnimalArchive> wrapper = new LambdaQueryWrapper<StrayAnimalArchive>()
                .eq(StrayAnimalArchive::getStatus, "APPROVED")
                .eq(animalType != null, StrayAnimalArchive::getAnimalType, animalType)
                .eq(placementStatus != null, StrayAnimalArchive::getPlacementStatus, placementStatus)
                .orderByDesc(StrayAnimalArchive::getCreateTime);
        return Result.success(archiveService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(archiveService.getById(id));
    }

    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "100") int size) {
        LambdaQueryWrapper<StrayAnimalArchive> wrapper = new LambdaQueryWrapper<StrayAnimalArchive>()
                .eq(StrayAnimalArchive::getStatus, "PENDING")
                .orderByDesc(StrayAnimalArchive::getCreateTime);
        return Result.success(archiveService.page(new Page<>(page, size), wrapper));
    }
}
