package com.petrecovery.module.adoption.controller;

import com.petrecovery.common.Result;
import com.petrecovery.module.adoption.service.AdoptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/adoption")
public class AdoptionController {

    private final AdoptionService adoptionService;

    public AdoptionController(AdoptionService adoptionService) {
        this.adoptionService = adoptionService;
    }

    @PostMapping("/create")
    public Result<?> create(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long archiveId = Long.valueOf(body.get("archiveId").toString());
        String message = getText(body, "message");
        String contact = getText(body, "contact");
        String livingCondition = getText(body, "livingCondition");
        String petExperience = getText(body, "petExperience");
        if (contact.isBlank() || livingCondition.isBlank() || petExperience.isBlank()) {
            return Result.error(400, "请完整填写联系方式、居住条件和养宠经验");
        }
        if (contact.length() > 64) {
            return Result.error(400, "联系方式不能超过64个字符");
        }
        return Result.success(adoptionService.createRequest(archiveId, userId, message, contact, livingCondition, petExperience));
    }

    @GetMapping("/incoming")
    public Result<?> incoming(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(adoptionService.getIncomingRequests(userId));
    }

    @GetMapping("/my")
    public Result<?> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(adoptionService.getMyRequests(userId));
    }

    @GetMapping("/incoming-from/{applicantId}")
    public Result<?> incomingFrom(@PathVariable Long applicantId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(adoptionService.getIncomingFrom(applicantId, userId));
    }

    @GetMapping("/records")
    public Result<?> records(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(adoptionService.getAdoptionRecords(userId));
    }

    @PostMapping("/review/{id}")
    public Result<?> review(@PathVariable Long id, @RequestParam String action, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        adoptionService.reviewRequest(id, userId, action);
        return Result.success();
    }

    @PostMapping("/record/{id}/follow-up")
    public Result<?> updateFollowUp(@PathVariable Long id, @RequestParam String status, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        adoptionService.updateFollowUpStatus(id, userId, status);
        return Result.success();
    }

    private String getText(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : value.toString().trim();
    }
}
