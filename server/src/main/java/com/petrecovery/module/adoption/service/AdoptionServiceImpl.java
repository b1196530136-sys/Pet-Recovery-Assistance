package com.petrecovery.module.adoption.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petrecovery.module.adoption.entity.AdoptionRecord;
import com.petrecovery.module.adoption.entity.AdoptionRequest;
import com.petrecovery.module.adoption.mapper.AdoptionRecordMapper;
import com.petrecovery.module.adoption.mapper.AdoptionRequestMapper;
import com.petrecovery.module.archive.entity.StrayAnimalArchive;
import com.petrecovery.module.archive.mapper.ArchiveMapper;
import com.petrecovery.module.message.entity.SysImMessage;
import com.petrecovery.module.message.service.MessageService;
import com.petrecovery.module.user.entity.SysUser;
import com.petrecovery.module.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdoptionServiceImpl extends ServiceImpl<AdoptionRequestMapper, AdoptionRequest> implements AdoptionService {

    private final ArchiveMapper archiveMapper;
    private final UserMapper userMapper;
    private final MessageService messageService;
    private final AdoptionRecordMapper adoptionRecordMapper;

    public AdoptionServiceImpl(ArchiveMapper archiveMapper, UserMapper userMapper, MessageService messageService, AdoptionRecordMapper adoptionRecordMapper) {
        this.archiveMapper = archiveMapper;
        this.userMapper = userMapper;
        this.messageService = messageService;
        this.adoptionRecordMapper = adoptionRecordMapper;
    }

    @Override
    public AdoptionRequest createRequest(Long archiveId, Long applicantId, String message, String contact, String livingCondition, String petExperience) {
        StrayAnimalArchive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new RuntimeException("档案不存在");
        }
        if (!"APPROVED".equals(archive.getStatus()) || !"adoptable".equals(archive.getPlacementStatus())) {
            throw new RuntimeException("该档案暂未开放领养");
        }
        if (archive.getUserId().equals(applicantId)) {
            throw new RuntimeException("不能申请领养自己的档案");
        }
        long pendingCount = count(new LambdaQueryWrapper<AdoptionRequest>()
                .eq(AdoptionRequest::getArchiveId, archiveId)
                .eq(AdoptionRequest::getApplicantId, applicantId)
                .eq(AdoptionRequest::getStatus, "PENDING"));
        if (pendingCount > 0) {
            throw new RuntimeException("您已提交过该档案的领养申请，请等待处理");
        }

        AdoptionRequest request = new AdoptionRequest();
        request.setArchiveId(archiveId);
        request.setApplicantId(applicantId);
        request.setOwnerId(archive.getUserId());
        request.setMessage(message);
        request.setContact(contact);
        request.setLivingCondition(livingCondition);
        request.setPetExperience(petExperience);
        request.setStatus("PENDING");
        save(request);

        // 向档案发布人发送私信通知
        SysUser applicant = userMapper.selectById(applicantId);
        String applicantName = applicant != null ? applicant.getNickname() : "用户";
        String notice = "用户 " + applicantName + " 想要领养您的档案（" + archive.getAnimalType() + "），留言："
                + (message != null && !message.isEmpty() ? message : "无留言")
                + "；联系方式：" + (contact != null && !contact.isEmpty() ? contact : "未填写")
                + "；居住条件：" + (livingCondition != null && !livingCondition.isEmpty() ? livingCondition : "未填写")
                + "；养宠经验：" + (petExperience != null && !petExperience.isEmpty() ? petExperience : "未填写");
        SysImMessage noticeMsg = new SysImMessage();
        noticeMsg.setSenderId(applicantId);
        noticeMsg.setReceiverId(archive.getUserId());
        noticeMsg.setContent(notice);
        messageService.sendNormalMessage(noticeMsg);

        return request;
    }

    @Override
    public List<Map<String, Object>> getIncomingRequests(Long ownerId) {
        List<AdoptionRequest> requests = list(new LambdaQueryWrapper<AdoptionRequest>()
                .eq(AdoptionRequest::getOwnerId, ownerId)
                .orderByDesc(AdoptionRequest::getCreateTime));

        if (requests.isEmpty()) return Collections.emptyList();

        Set<Long> archiveIds = requests.stream().map(AdoptionRequest::getArchiveId).collect(Collectors.toSet());
        Set<Long> userIds = requests.stream().map(AdoptionRequest::getApplicantId).collect(Collectors.toSet());

        Map<Long, StrayAnimalArchive> archiveMap = archiveMapper.selectBatchIds(archiveIds)
                .stream().collect(Collectors.toMap(StrayAnimalArchive::getId, a -> a));
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return requests.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("archiveId", r.getArchiveId());
            map.put("applicantId", r.getApplicantId());
            map.put("message", r.getMessage());
            map.put("contact", r.getContact());
            map.put("livingCondition", r.getLivingCondition());
            map.put("petExperience", r.getPetExperience());
            map.put("status", r.getStatus());
            map.put("createTime", r.getCreateTime());

            StrayAnimalArchive a = archiveMap.get(r.getArchiveId());
            map.put("animalType", a != null ? a.getAnimalType() : null);

            SysUser u = userMap.get(r.getApplicantId());
            map.put("applicantName", u != null ? u.getNickname() : "未知用户");
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getMyRequests(Long applicantId) {
        List<AdoptionRequest> requests = list(new LambdaQueryWrapper<AdoptionRequest>()
                .eq(AdoptionRequest::getApplicantId, applicantId)
                .orderByDesc(AdoptionRequest::getCreateTime));

        if (requests.isEmpty()) return Collections.emptyList();

        Set<Long> archiveIds = requests.stream().map(AdoptionRequest::getArchiveId).collect(Collectors.toSet());
        Set<Long> userIds = requests.stream().map(AdoptionRequest::getOwnerId).collect(Collectors.toSet());

        Map<Long, StrayAnimalArchive> archiveMap = archiveMapper.selectBatchIds(archiveIds)
                .stream().collect(Collectors.toMap(StrayAnimalArchive::getId, a -> a));
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return requests.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("archiveId", r.getArchiveId());
            map.put("ownerId", r.getOwnerId());
            map.put("message", r.getMessage());
            map.put("contact", r.getContact());
            map.put("livingCondition", r.getLivingCondition());
            map.put("petExperience", r.getPetExperience());
            map.put("status", r.getStatus());
            map.put("createTime", r.getCreateTime());

            StrayAnimalArchive a = archiveMap.get(r.getArchiveId());
            map.put("animalType", a != null ? a.getAnimalType() : null);

            SysUser u = userMap.get(r.getOwnerId());
            map.put("ownerName", u != null ? u.getNickname() : "未知用户");
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getIncomingFrom(Long applicantId, Long ownerId) {
        List<AdoptionRequest> requests = list(new LambdaQueryWrapper<AdoptionRequest>()
                .eq(AdoptionRequest::getApplicantId, applicantId)
                .eq(AdoptionRequest::getOwnerId, ownerId)
                .eq(AdoptionRequest::getStatus, "PENDING")
                .orderByDesc(AdoptionRequest::getCreateTime));
        if (requests.isEmpty()) return null;

        AdoptionRequest r = requests.get(0);
        StrayAnimalArchive archive = archiveMapper.selectById(r.getArchiveId());
        SysUser applicant = userMapper.selectById(r.getApplicantId());

        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("archiveId", r.getArchiveId());
        map.put("applicantId", r.getApplicantId());
        map.put("message", r.getMessage());
        map.put("contact", r.getContact());
        map.put("livingCondition", r.getLivingCondition());
        map.put("petExperience", r.getPetExperience());
        map.put("status", r.getStatus());
        map.put("createTime", r.getCreateTime());
        map.put("animalType", archive != null ? archive.getAnimalType() : null);
        map.put("applicantName", applicant != null ? applicant.getNickname() : "未知用户");
        return map;
    }

    @Override
    public List<Map<String, Object>> getAdoptionRecords(Long userId) {
        List<AdoptionRecord> records = adoptionRecordMapper.selectList(new LambdaQueryWrapper<AdoptionRecord>()
                .eq(AdoptionRecord::getOwnerId, userId)
                .or()
                .eq(AdoptionRecord::getAdopterId, userId)
                .orderByDesc(AdoptionRecord::getCreateTime));
        if (records.isEmpty()) return Collections.emptyList();

        Set<Long> archiveIds = records.stream().map(AdoptionRecord::getArchiveId).collect(Collectors.toSet());
        Set<Long> userIds = records.stream()
                .flatMap(r -> Arrays.asList(r.getOwnerId(), r.getAdopterId()).stream())
                .collect(Collectors.toSet());

        Map<Long, StrayAnimalArchive> archiveMap = archiveMapper.selectBatchIds(archiveIds)
                .stream().collect(Collectors.toMap(StrayAnimalArchive::getId, a -> a));
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return records.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("requestId", r.getRequestId());
            map.put("archiveId", r.getArchiveId());
            map.put("ownerId", r.getOwnerId());
            map.put("adopterId", r.getAdopterId());
            map.put("contact", r.getContact());
            map.put("livingCondition", r.getLivingCondition());
            map.put("petExperience", r.getPetExperience());
            map.put("followUpStatus", r.getFollowUpStatus());
            map.put("createTime", r.getCreateTime());
            map.put("updateTime", r.getUpdateTime());

            StrayAnimalArchive archive = archiveMap.get(r.getArchiveId());
            map.put("animalType", archive != null ? archive.getAnimalType() : null);
            map.put("archiveName", archive != null ? archive.getName() : null);

            SysUser owner = userMap.get(r.getOwnerId());
            SysUser adopter = userMap.get(r.getAdopterId());
            map.put("ownerName", owner != null ? owner.getNickname() : "未知用户");
            map.put("adopterName", adopter != null ? adopter.getNickname() : "未知用户");
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reviewRequest(Long requestId, Long ownerId, String action) {
        AdoptionRequest request = getById(requestId);
        if (request == null) {
            throw new RuntimeException("申请不存在");
        }
        if (!request.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("无权操作该申请");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("该申请已被处理");
        }
        if (!"APPROVED".equals(action) && !"REJECTED".equals(action)) {
            throw new RuntimeException("无效的操作");
        }

        StrayAnimalArchive archive = archiveMapper.selectById(request.getArchiveId());
        String animalType = archive != null ? archive.getAnimalType() : "";
        if ("APPROVED".equals(action)) {
            if (archive == null) {
                throw new RuntimeException("档案不存在");
            }
            if (!"adoptable".equals(archive.getPlacementStatus())) {
                throw new RuntimeException("该档案当前不可领养");
            }
        }

        request.setStatus(action);
        updateById(request);

        // 同意后更新档案安置状态为已被领养，并生成回访记录
        if ("APPROVED".equals(action) && archive != null) {
            archive.setPlacementStatus("adopted");
            archiveMapper.updateById(archive);
            update(new LambdaUpdateWrapper<AdoptionRequest>()
                    .eq(AdoptionRequest::getArchiveId, request.getArchiveId())
                    .eq(AdoptionRequest::getStatus, "PENDING")
                    .ne(AdoptionRequest::getId, request.getId())
                    .set(AdoptionRequest::getStatus, "REJECTED"));

            long recordCount = adoptionRecordMapper.selectCount(new LambdaQueryWrapper<AdoptionRecord>()
                    .eq(AdoptionRecord::getRequestId, request.getId()));
            if (recordCount == 0) {
                AdoptionRecord record = new AdoptionRecord();
                record.setRequestId(request.getId());
                record.setArchiveId(request.getArchiveId());
                record.setAdopterId(request.getApplicantId());
                record.setOwnerId(request.getOwnerId());
                record.setContact(request.getContact());
                record.setLivingCondition(request.getLivingCondition());
                record.setPetExperience(request.getPetExperience());
                record.setFollowUpStatus("PENDING");
                adoptionRecordMapper.insert(record);
            }
        }

        // 向申请人发送结果通知
        String msg = "APPROVED".equals(action) ? "同意了" : "拒绝了";
        SysUser owner = userMapper.selectById(ownerId);
        String ownerName = owner != null ? owner.getNickname() : "发布人";
        String notice = "发布人 " + ownerName + " " + msg + "您的领养申请（" + animalType + "）";
        SysImMessage noticeMsg = new SysImMessage();
        noticeMsg.setSenderId(ownerId);
        noticeMsg.setReceiverId(request.getApplicantId());
        noticeMsg.setContent(notice);
        messageService.sendNormalMessage(noticeMsg);
    }

    @Override
    public void updateFollowUpStatus(Long recordId, Long userId, String status) {
        AdoptionRecord record = adoptionRecordMapper.selectById(recordId);
        if (record == null) {
            throw new RuntimeException("领养记录不存在");
        }
        if (!record.getOwnerId().equals(userId)) {
            throw new RuntimeException("无权更新该领养记录");
        }
        if (!Arrays.asList("PENDING", "VISITED", "ABNORMAL").contains(status)) {
            throw new RuntimeException("无效的回访状态");
        }
        record.setFollowUpStatus(status);
        adoptionRecordMapper.updateById(record);
    }
}
