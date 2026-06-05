package com.petrecovery.module.adoption.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.petrecovery.module.adoption.entity.AdoptionRequest;

import java.util.List;
import java.util.Map;

public interface AdoptionService extends IService<AdoptionRequest> {
    AdoptionRequest createRequest(Long archiveId, Long applicantId, String message);
    List<Map<String, Object>> getIncomingRequests(Long ownerId);
    List<Map<String, Object>> getMyRequests(Long applicantId);
    Map<String, Object> getIncomingFrom(Long applicantId, Long ownerId);
    void reviewRequest(Long requestId, Long ownerId, String action);
}
