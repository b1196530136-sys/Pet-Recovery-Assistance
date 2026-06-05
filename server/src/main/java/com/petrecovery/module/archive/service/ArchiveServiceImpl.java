package com.petrecovery.module.archive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;
import com.petrecovery.module.archive.mapper.ArchiveMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ArchiveServiceImpl extends ServiceImpl<ArchiveMapper, StrayAnimalArchive> implements ArchiveService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public StrayAnimalArchive createArchive(StrayAnimalArchive archive, Long userId) {
        archive.setUserId(userId);
        archive.setStatus("PENDING");
        save(archive);
        return archive;
    }

    @Override
    public void updateArchive(StrayAnimalArchive archive, Long userId) {
        StrayAnimalArchive existing = getById(archive.getId());
        if (existing == null) throw new RuntimeException("档案不存在");
        if (!existing.getUserId().equals(userId)) throw new RuntimeException("无权修改此档案");

        // 将可编辑字段序列化为JSON存入pendingData，原数据不变
        try {
            Map<String, Object> pending = Map.of(
                "animalType", archive.getAnimalType() != null ? archive.getAnimalType() : existing.getAnimalType(),
                "longitude", archive.getLongitude(),
                "latitude", archive.getLatitude(),
                "address", archive.getAddress() != null ? archive.getAddress() : existing.getAddress(),
                "healthStatus", archive.getHealthStatus(),
                "neuteredStatus", archive.getNeuteredStatus(),
                "immuneStatus", archive.getImmuneStatus(),
                "placementStatus", archive.getPlacementStatus(),
                "photos", archive.getPhotos() != null ? archive.getPhotos() : existing.getPhotos(),
                "description", archive.getDescription()
            );
            existing.setPendingData(objectMapper.writeValueAsString(pending));
            // 修改时保持原状态(APPROVED)，不改为PENDING，确保档案大厅仍显示原数据
            existing.setRejectReason(null);
            updateById(existing);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("数据序列化失败", e);
        }
    }

    @Override
    public void deleteArchive(Long id, Long userId) {
        StrayAnimalArchive existing = getById(id);
        if (existing == null) throw new RuntimeException("档案不存在");
        if (!existing.getUserId().equals(userId)) throw new RuntimeException("无权删除此档案");
        removeById(id);
    }

    @Override
    public void approveArchive(Long id) {
        StrayAnimalArchive archive = getById(id);
        if (archive == null) throw new RuntimeException("档案不存在");

        // 如果有待审的修改内容，合并到主字段
        if (archive.getPendingData() != null && !archive.getPendingData().isBlank()) {
            try {
                Map<String, Object> pending = objectMapper.readValue(archive.getPendingData(), Map.class);
                if (pending.get("animalType") != null) archive.setAnimalType((String) pending.get("animalType"));
                if (pending.get("longitude") != null) archive.setLongitude(new java.math.BigDecimal(pending.get("longitude").toString()));
                if (pending.get("latitude") != null) archive.setLatitude(new java.math.BigDecimal(pending.get("latitude").toString()));
                if (pending.get("address") != null) archive.setAddress((String) pending.get("address"));
                if (pending.get("healthStatus") != null) archive.setHealthStatus((String) pending.get("healthStatus"));
                if (pending.get("neuteredStatus") != null) archive.setNeuteredStatus((String) pending.get("neuteredStatus"));
                if (pending.get("immuneStatus") != null) archive.setImmuneStatus((String) pending.get("immuneStatus"));
                if (pending.get("placementStatus") != null) archive.setPlacementStatus((String) pending.get("placementStatus"));
                if (pending.get("photos") != null) archive.setPhotos((String) pending.get("photos"));
                if (pending.get("description") != null) archive.setDescription((String) pending.get("description"));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("数据解析失败", e);
            }
            archive.setPendingData(null);
        }

        archive.setStatus("APPROVED");
        archive.setRejectReason(null);
        // 显式更新：确保 pendingData 的 null 值能写入数据库
        LambdaUpdateWrapper<StrayAnimalArchive> uw = new LambdaUpdateWrapper<>();
        uw.eq(StrayAnimalArchive::getId, id)
          .set(StrayAnimalArchive::getStatus, archive.getStatus())
          .set(StrayAnimalArchive::getRejectReason, archive.getRejectReason())
          .set(StrayAnimalArchive::getPendingData, archive.getPendingData());
        if (archive.getPendingData() == null && archive.getAnimalType() != null) {
            uw.set(StrayAnimalArchive::getAnimalType, archive.getAnimalType())
              .set(StrayAnimalArchive::getLongitude, archive.getLongitude())
              .set(StrayAnimalArchive::getLatitude, archive.getLatitude())
              .set(StrayAnimalArchive::getAddress, archive.getAddress())
              .set(StrayAnimalArchive::getHealthStatus, archive.getHealthStatus())
              .set(StrayAnimalArchive::getNeuteredStatus, archive.getNeuteredStatus())
              .set(StrayAnimalArchive::getImmuneStatus, archive.getImmuneStatus())
              .set(StrayAnimalArchive::getPlacementStatus, archive.getPlacementStatus())
              .set(StrayAnimalArchive::getPhotos, archive.getPhotos())
              .set(StrayAnimalArchive::getDescription, archive.getDescription());
        }
        update(uw);
    }

    @Override
    public void rejectArchive(Long id, String reason) {
        StrayAnimalArchive archive = getById(id);
        if (archive == null) throw new RuntimeException("档案不存在");

        if (archive.getPendingData() != null && !archive.getPendingData().isBlank()) {
            // 有待审修改内容：仅驳回修改，不清除档案，恢复为已通过状态
            LambdaUpdateWrapper<StrayAnimalArchive> uw = new LambdaUpdateWrapper<>();
            uw.eq(StrayAnimalArchive::getId, id)
              .set(StrayAnimalArchive::getPendingData, null)
              .set(StrayAnimalArchive::getStatus, "APPROVED")
              .set(StrayAnimalArchive::getRejectReason, reason);
            update(uw);
        } else {
            // 新建档案被驳回
            archive.setStatus("REJECTED");
            archive.setRejectReason(reason);
            updateById(archive);
        }
    }
}
