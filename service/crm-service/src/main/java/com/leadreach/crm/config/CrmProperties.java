package com.leadreach.crm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 线索策略配置，对应 application.yml 的 crm.lead 段
 */
@Data
@Component
@ConfigurationProperties(prefix = "crm.lead")
public class CrmProperties {

    /** 领取/分配后的保护天数，超过则退回公海 */
    private int protectDays = 15;

    /** 退回公海扫描任务的 cron */
    private String recycleCron = "0 0 3 * * ?";

    /** 看板「即将超保护期」预警窗口（天） */
    private int expiringSoonDays = 3;
}