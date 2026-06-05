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
        String message = (String) body.getOrDefault("message", "");
        return Result.success(adoptionService.createRequest(archiveId, userId, message));
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

    @PostMapping("/review/{id}")
    public Result<?> review(@PathVariable Long id, @RequestParam String action, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        adoptionService.reviewRequest(id, userId, action);
        return Result.success();
    }
}
