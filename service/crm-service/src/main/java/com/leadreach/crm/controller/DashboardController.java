package com.leadreach.crm.controller;

import com.leadreach.crm.common.R;
import com.leadreach.crm.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 线索看板接口，网关前缀 /api/v1/crm
 */
@RestController
@RequestMapping("/api/v1/crm/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /** 我的线索概览 */
    @GetMapping("/my")
    public R<Map<String, Object>> my() {
        return R.ok(dashboardService.my());
    }

    /** 团队看板：老板看全公司，销售主管看本团队 */
    @GetMapping("/team")
    public R<Map<String, Object>> team() {
        return R.ok(dashboardService.team());
    }

    /** 公司总览：仅老板 */
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(dashboardService.overview());
    }
}