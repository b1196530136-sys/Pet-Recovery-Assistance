package com.petrecovery.module.archive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.petrecovery.common.Result;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;
import com.petrecovery.module.archive.service.ArchiveService;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/archive")
public class ArchiveController {

    private final ArchiveService archiveService;
    private final UserMapper userMapper;

    public ArchiveController(ArchiveService archiveService, UserMapper userMapper) {
        this.archiveService = archiveService;
        this.userMapper = userMapper;
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
                .eq(animalType != null && !animalType.isEmpty(), StrayAnimalArchive::getAnimalType, animalType)
                .eq(placementStatus != null && !placementStatus.isEmpty(), StrayAnimalArchive::getPlacementStatus, placementStatus)
                .orderByDesc(StrayAnimalArchive::getCreateTime);
        return Result.success(archiveService.page(new Page<>(page, size), wrapper));
    }

    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        StrayAnimalArchive archive = archiveService.getById(id);
        Map<String, Object> result = new HashMap<>();
        result.putAll(archive == null ? Map.of() : convertToMap(archive));
        if (archive != null && archive.getUserId() != null) {
            SysUser user = userMapper.selectById(archive.getUserId());
            result.put("publisherName", user != null ? user.getNickname() : "未知用户");
            result.put("publisherId", archive.getUserId());
        }
        if (archive != null) {
            result.put("pendingData", archive.getPendingData());
            result.put("isUpdate", archive.getPendingData() != null && !archive.getPendingData().isBlank());
            result.put("rejectReason", archive.getRejectReason());
        }
        return Result.success(result);
    }

    private Map<String, Object> convertToMap(StrayAnimalArchive archive) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", archive.getId());
        map.put("userId", archive.getUserId());
        map.put("animalType", archive.getAnimalType());
        map.put("longitude", archive.getLongitude());
        map.put("latitude", archive.getLatitude());
        map.put("address", archive.getAddress());
        map.put("healthStatus", archive.getHealthStatus());
        map.put("neuteredStatus", archive.getNeuteredStatus());
        map.put("immuneStatus", archive.getImmuneStatus());
        map.put("placementStatus", archive.getPlacementStatus());
        map.put("photos", archive.getPhotos());
        map.put("description", archive.getDescription());
        map.put("status", archive.getStatus());
        map.put("createTime", archive.getCreateTime());
        map.put("updateTime", archive.getUpdateTime());
        return map;
    }

    @PutMapping("/update")
    public Result<?> update(@RequestBody StrayAnimalArchive archive, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        archiveService.updateArchive(archive, userId);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        archiveService.deleteArchive(id, userId);
        return Result.success();
    }

    @GetMapping("/pending")
    public Result<?> pending(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "100") int size) {
        // 待审列表：新建(PENDING状态) + 修改(有pendingData但状态为APPROVED)
        LambdaQueryWrapper<StrayAnimalArchive> wrapper = new LambdaQueryWrapper<StrayAnimalArchive>()
                .and(w -> w.eq(StrayAnimalArchive::getStatus, "PENDING")
                          .or()
                          .isNotNull(StrayAnimalArchive::getPendingData))
                .orderByDesc(StrayAnimalArchive::getUpdateTime);
        Page<StrayAnimalArchive> pageResult = archiveService.page(new Page<>(page, size), wrapper);
        // 为每条记录附加 isUpdate 标识
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords().stream().map(arch -> {
            Map<String, Object> item = convertToMap(arch);
            item.put("isUpdate", arch.getPendingData() != null && !arch.getPendingData().isBlank());
            item.put("pendingData", arch.getPendingData());
            return item;
        }).toList());
        result.put("total", pageResult.getTotal());
        result.put("size", pageResult.getSize());
        result.put("current", pageResult.getCurrent());
        return Result.success(result);
    }
}
