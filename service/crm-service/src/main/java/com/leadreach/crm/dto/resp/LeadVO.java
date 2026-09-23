package com.leadreach.crm.dto.resp;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线索列表项
 */
@Data
public class LeadVO {
    private Long id;
    private String title;
    /** NEW/ASSIGNED/CONTACTED/QUALIFIED/CONVERTED/INVALID */
    private String status;
    /** PUBLIC/PRIVATE/PROTECTED */
    private String poolType;
    private Integer score;
    private String grade;

    private Long leadCompanyId;
    private String companyName;
    private String countryCode;

    private Long primaryContactId;
    private String contactName;
    private String contactEmail;

    private Long ownerUserId;
    /** 负责人展示名（当前为手机号，来自 user-service） */
    private String ownerName;
    private Long ownerTeamId;
    private String ownerTeamName;

    private LocalDateTime protectedUntil;
    private LocalDateTime convertedAt;
    private Long convertedOpportunityId;

    /** 最近一次跟进时间 */
    private LocalDateTime lastFollowAt;
    /** 下次跟进时间 */
    private LocalDateTime nextFollowAt;

    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}