package com.leadreach.user.service;

import java.util.Map;

/**
 * 员工申请加入公司业务接口
 * 流程：员工搜索公司 → 提交申请 → 老板审批（同意：员工入默认团队并回填租户；拒绝：仅标记）
 */
public interface JoinRequestService {

    /** 员工按公司名称搜索公司 */
    Map<String, Object> searchCompanies(String keyword);

    /** 员工提交加入申请（必须是未入公司的在职员工，同公司不可重复待审） */
    Map<String, Object> apply(Long employeeId, Long companyId);

    /** 员工撤回自己的待审批申请（仅 PENDING 可撤回，置 WITHDRAWN） */
    Map<String, Object> withdraw(Long employeeId, Long requestId);

    /** 员工查看自己的申请记录及状态 */
    Map<String, Object> myRequests(Long employeeId);

    /** 老板查看本公司的申请列表（含申请人信息与状态） */
    Map<String, Object> listRequests(Long bossId, Long tenantId);

    /** 老板同意申请：员工入默认团队（岗位默认销售专员）并回填租户，申请置 APPROVED */
    Map<String, Object> approve(Long bossId, Long tenantId, Long requestId);

    /** 老板拒绝申请：仅置 REJECTED，员工可另寻公司 */
    Map<String, Object> reject(Long bossId, Long tenantId, Long requestId);
}
