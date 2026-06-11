package com.petrecovery.module.adoption.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.adoption.entity.AdoptionRequest;

import java.util.List;
import java.util.Map;

public interface AdoptionService extends IService<AdoptionRequest> {
    AdoptionRequest createRequest(Long archiveId, Long applicantId, String message, String contact, String livingCondition, String petExperience);
    List<Map<String, Object>> getIncomingRequests(Long ownerId);
    List<Map<String, Object>> getMyRequests(Long applicantId);
    Map<String, Object> getIncomingFrom(Long applicantId, Long ownerId);
    List<Map<String, Object>> getAdoptionRecords(Long userId);
    void reviewRequest(Long requestId, Long ownerId, String action);
    void updateFollowUpStatus(Long recordId, Long userId, String status);
}
