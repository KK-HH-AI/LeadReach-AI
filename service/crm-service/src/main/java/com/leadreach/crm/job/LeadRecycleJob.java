package com.leadreach.crm.job;

import com.leadreach.crm.entity.Lead;
import com.leadreach.crm.mapper.LeadMapper;
import com.leadreach.crm.service.LeadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务线6：保护期到期自动退回公海
 *
 * 扫描是跨租户的（定时任务线程没有请求头，拿不到租户上下文），
 * 所以扫描 SQL 忽略租户过滤，回收时再按每行自带的 tenant_id 精确更新，
 * 保证不会误伤其他租户的数据。
 */
@Slf4j
@Component
public class LeadRecycleJob {

    /** 单批扫描条数 */
    private static final int BATCH_SIZE = 200;

    /** 单次任务最大轮数，避免异常数据导致无限循环 */
    private static final int MAX_ROUNDS = 50;

    @Autowired
    private LeadMapper leadMapper;

    @Autowired
    private LeadService leadService;

    @Scheduled(cron = "${crm.lead.recycle-cron}")
    public void recycleExpiredLeads() {
        long start = System.currentTimeMillis();
        int recycled = 0;
        int round = 0;
        while (round++ < MAX_ROUNDS) {
            List<Lead> expired = leadMapper.selectExpiredWithoutTenant(BATCH_SIZE);
            if (expired.isEmpty()) {
                break;
            }
            for (Lead lead : expired) {
                try {
                    leadService.recycleBySystem(lead, "保护期已过，系统自动回收");
                    recycled++;
                } catch (Exception e) {
                    // 单条失败不影响整批：下轮扫描还会带上它，重试即可
                    log.warn("线索 {} 自动回收失败: {}", lead.getId(), e.getMessage());
                }
            }
            if (expired.size() < BATCH_SIZE) {
                break;
            }
        }
        log.info("线索保护期回收任务结束：回收 {} 条，耗时 {} ms", recycled, System.currentTimeMillis() - start);
    }
}