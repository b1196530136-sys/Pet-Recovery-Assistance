package com.petrecovery.module.archive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.petrecovery.config.JwtConfig;
import com.petrecovery.common.Result;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;
import com.petrecovery.module.archive.service.ArchiveService;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/archive")
public class ArchiveController {

    private final ArchiveService archiveService;
    private final UserMapper userMapper;
    private final JwtConfig jwtConfig;

    public ArchiveController(ArchiveService archiveService, UserMapper userMapper, JwtConfig jwtConfig) {
        this.archiveService = archiveService;
        this.userMapper = userMapper;
        this.jwtConfig = jwtConfig;
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
    public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
        StrayAnimalArchive archive = archiveService.getById(id);
        if (archive == null) return Result.error("档案不存在");
        RequestUser viewer = resolveViewer(request);
        boolean privileged = isOwner(archive, viewer) || isAdmin(viewer);
        if (!"APPROVED".equals(archive.getStatus()) && !privileged) {
            return Result.error(403, "无权查看此档案");
        }
        Map<String, Object> result = new HashMap<>();
        result.putAll(convertToMap(archive));
        if (archive.getUserId() != null) {
            SysUser user = userMapper.selectById(archive.getUserId());
            result.put("publisherName", user != null ? user.getNickname() : "未知用户");
            result.put("publisherAvatar", user != null ? user.getAvatar() : null);
            result.put("publisherId", archive.getUserId());
        }
        if (privileged) {
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
        map.put("name", archive.getName());
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
                             @RequestParam(defaultValue = "100") int size,
                             HttpServletRequest request) {
        checkAdmin(request);
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

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            throw new SecurityException("无管理员权限");
        }
    }

    private boolean isOwner(StrayAnimalArchive archive, RequestUser viewer) {
        return viewer.userId != null && archive.getUserId() != null && archive.getUserId().equals(viewer.userId);
    }

    private boolean isAdmin(RequestUser viewer) {
        return "ADMIN".equals(viewer.role);
    }

    private RequestUser resolveViewer(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        Object role = request.getAttribute("role");
        if (userId instanceof Long id) {
            return new RequestUser(id, role instanceof String r ? r : null);
        }
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                Claims claims = jwtConfig.parseToken(token.substring(7));
                return new RequestUser(Long.parseLong(claims.getSubject()), claims.get("role", String.class));
            } catch (Exception ignored) {
            }
        }
        return new RequestUser(null, null);
    }

    private record RequestUser(Long userId, String role) {}
}
