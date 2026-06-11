package com.petrecovery.module.admin.dto;

import lombok.Data;

@Data
public class ReportHandleRequest {
    private String action;
    private String handleNote;
    private Boolean banReportedUser;
}
