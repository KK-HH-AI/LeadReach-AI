package com.leadreach.crm.dto.req;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 记录跟进请求
 */
@Data
public class ActivityCreateRequest {
    /** 业务类型，本期只用 LEAD */
    private String bizType;
    /** 业务ID（线索ID） */
    private Long bizId;
    /** CALL/EMAIL/WHATSAPP/LINKEDIN/MEETING/NOTE */
    private String activityType;
    private String content;
    private String result;
    /** 跟进时间，缺省取当前时间 */
    private LocalDateTime activityAt;
    /** 下次跟进时间 */
    private LocalDateTime nextFollowAt;
}