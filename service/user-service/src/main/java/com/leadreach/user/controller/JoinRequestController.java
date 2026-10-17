package com.leadreach.user.controller;

import com.leadreach.user.context.TenantContext;
import com.leadreach.user.service.JoinRequestService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 员工申请加入公司接口
 * 员工侧：搜索公司 / 提交申请 / 我的申请（员工无租户上下文，身份来自 X-User-Id）
 * 老板侧：申请列表 / 同意 / 拒绝（有租户上下文，Service 层做老板身份复核）
 */
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    // 员工按公司名称搜索公司
    @GetMapping("/search")
    public Map<String, Object> searchCompanies(@RequestParam("keyword") String keyword) {
        return joinRequestService.searchCompanies(keyword);
    }

    // 员工提交加入申请
    @PostMapping("/join-requests")
    public Map<String, Object> apply(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long companyId = null;
        String companyIdStr = body.get("companyId");
        if (companyIdStr != null && !companyIdStr.isBlank()) {
            companyId = Long.parseLong(companyIdStr.trim());
        }
        return joinRequestService.apply(parseUid(request), companyId);
    }

    // 员工查看自己的申请记录及状态
    @GetMapping("/join-requests/mine")
    public Map<String, Object> myRequests(HttpServletRequest request) {
        return joinRequestService.myRequests(parseUid(request));
    }

    // 员工撤回自己的待审批申请
    @PostMapping("/join-requests/{requestId}/withdraw")
    public Map<String, Object> withdraw(HttpServletRequest request, @PathVariable Long requestId) {
        return joinRequestService.withdraw(parseUid(request), requestId);
    }

    // 老板查看本公司的申请列表
    @GetMapping("/join-requests")
    public Map<String, Object> listRequests(HttpServletRequest request) {
        return joinRequestService.listRequests(parseUid(request), TenantContext.getTenantId());
    }

    // 老板同意申请
    @PostMapping("/join-requests/{requestId}/approve")
    public Map<String, Object> approve(HttpServletRequest request, @PathVariable Long requestId) {
        return joinRequestService.approve(parseUid(request), TenantContext.getTenantId(), requestId);
    }

    // 老板拒绝申请
    @PostMapping("/join-requests/{requestId}/reject")
    public Map<String, Object> reject(HttpServletRequest request, @PathVariable Long requestId) {
        return joinRequestService.reject(parseUid(request), TenantContext.getTenantId(), requestId);
    }

    /** 从网关透传的 X-User-Id 请求头解析当前用户ID；缺失返回 null，由 Service 层校验兜底 */
    private Long parseUid(HttpServletRequest request) {
        String uid = request.getHeader("X-User-Id");
        if (uid == null || uid.isBlank()) {
            return null;
        }
        return Long.parseLong(uid.trim());
    }
}
