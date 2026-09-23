package com.leadreach.crm.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 线索详情：在列表项基础上补公司明细、联系人、跟进记录与归属变更日志
 */
@Data
public class LeadDetailVO extends LeadVO {

    private String website;
    private String industryName;
    private String address;
    private String sourceType;
    private String sourceUrl;

    /** 联系人（列表，含主联系人与其他KP） */
    private List<ContactVO> contacts;
    /** 跟进记录（按时间倒序） */
    private List<ActivityVO> activities;
    /** 归属变更日志（按时间倒序） */
    private List<AssignLogVO> assignLogs;

    @Data
    public static class ContactVO {
        private Long id;
        private String name;
        private String jobTitle;
        private String department;
        private String seniority;
        private String email;
        private String emailStatus;
        private String phone;
        private String whatsapp;
        private String linkedinUrl;
        private Integer isKp;
    }

    @Data
    public static class ActivityVO {
        private Long id;
        private String activityType;
        private String content;
        private String result;
        private Long ownerUserId;
        private String ownerName;
        private LocalDateTime activityAt;
        private LocalDateTime nextFollowAt;
        private LocalDateTime createdAt;
    }

    @Data
    public static class AssignLogVO {
        private Long id;
        private String action;
        private Long fromOwnerUserId;
        private String fromOwnerName;
        private Long toOwnerUserId;
        private String toOwnerName;
        private String reason;
        private Long operatorId;
        private LocalDateTime createdAt;
    }
}