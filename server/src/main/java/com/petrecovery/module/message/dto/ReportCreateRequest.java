package com.petrecovery.module.message.dto;

import lombok.Data;

@Data
public class ReportCreateRequest {
    private Long reportedUserId;
    private Long messageId;
    private String reportType;
    private String reason;
    private String detail;
}
