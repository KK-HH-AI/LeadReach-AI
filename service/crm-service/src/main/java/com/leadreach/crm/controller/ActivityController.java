package com.leadreach.crm.controller;

import com.leadreach.crm.common.R;
import com.leadreach.crm.dto.req.ActivityCreateRequest;
import com.leadreach.crm.dto.resp.LeadDetailVO;
import com.leadreach.crm.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 线索跟进接口，网关前缀 /api/v1/crm
 */
@RestController
@RequestMapping("/api/v1/crm/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    /** 记录一次跟进（首次跟进会把线索推进到 CONTACTED） */
    @PostMapping
    public R<Long> create(@RequestBody ActivityCreateRequest req) {
        return R.ok(activityService.create(req));
    }

    /** 查询某条线索的跟进记录 */
    @GetMapping
    public R<List<LeadDetailVO.ActivityVO>> list(@RequestParam(required = false) String bizType,
                                                 @RequestParam Long bizId) {
        return R.ok(activityService.list(bizType, bizId));
    }
}