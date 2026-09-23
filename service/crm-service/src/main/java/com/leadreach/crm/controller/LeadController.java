package com.leadreach.crm.controller;

import com.leadreach.crm.client.dto.UserDTO;
import com.leadreach.crm.common.PageResult;
import com.leadreach.crm.common.R;
import com.leadreach.crm.dto.req.LeadAssignRequest;
import com.leadreach.crm.dto.req.LeadConvertRequest;
import com.leadreach.crm.dto.req.LeadCreateRequest;
import com.leadreach.crm.dto.req.LeadRecycleRequest;
import com.leadreach.crm.dto.req.LeadStatusRequest;
import com.leadreach.crm.dto.resp.LeadDetailVO;
import com.leadreach.crm.dto.resp.LeadVO;
import com.leadreach.crm.service.LeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 线索接口，网关前缀 /api/v1/crm
 * 鉴权由网关统一完成，控制器只从请求头解析出来的上下文里取身份
 */
@RestController
@RequestMapping("/api/v1/crm/leads")
public class LeadController {

    @Autowired
    private LeadService leadService;

    /** 创建线索（入池） */
    @PostMapping
    public R<Long> create(@RequestBody LeadCreateRequest req) {
        return R.ok(leadService.create(req));
    }

    /** 批量创建线索，供采集模块使用 */
    @PostMapping("/batch")
    public R<Map<String, Object>> batchCreate(@RequestBody List<LeadCreateRequest> list) {
        return R.ok(leadService.batchCreate(list));
    }

    /** 公共线索池 */
    @GetMapping("/pool")
    public R<PageResult<LeadVO>> pool(@RequestParam(required = false) String status,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "20") long size) {
        return R.ok(leadService.pool(status, keyword, page, size));
    }

    /** 我的线索 */
    @GetMapping("/my")
    public R<PageResult<LeadVO>> my(@RequestParam(required = false) String status,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "20") long size) {
        return R.ok(leadService.my(status, keyword, page, size));
    }

    /** 线索列表（按岗位叠加数据范围） */
    @GetMapping
    public R<PageResult<LeadVO>> list(@RequestParam(required = false) String status,
                                      @RequestParam(required = false) String poolType,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long ownerUserId,
                                      @RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "20") long size) {
        return R.ok(leadService.list(status, poolType, keyword, ownerUserId, page, size));
    }

    /** 可分配的销售专员：老板看全公司，主管看本团队 */
    @GetMapping("/assignable-users")
    public R<List<UserDTO>> assignableUsers() {
        return R.ok(leadService.assignableUsers());
    }

    /** 线索详情（含联系人、跟进记录、流转日志） */
    @GetMapping("/{id}")
    public R<LeadDetailVO> detail(@PathVariable Long id) {
        return R.ok(leadService.detail(id));
    }

    /** 分配线索：老板可全公司分配，主管只能分给本团队 */
    @PutMapping("/{id}/assign")
    public R<Void> assign(@PathVariable Long id, @RequestBody LeadAssignRequest req) {
        leadService.assign(id, req);
        return R.ok();
    }

    /** 领取线索：仅销售专员，条件更新防并发重复领取 */
    @PutMapping("/{id}/claim")
    public R<Void> claim(@PathVariable Long id) {
        leadService.claim(id);
        return R.ok();
    }

    /** 更新线索状态 */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestBody LeadStatusRequest req) {
        leadService.updateStatus(id, req);
        return R.ok();
    }

    /** 退回公海 */
    @PutMapping("/{id}/recycle")
    public R<Void> recycle(@PathVariable Long id, @RequestBody(required = false) LeadRecycleRequest req) {
        leadService.recycle(id, req == null ? null : req.getReason());
        return R.ok();
    }

    /** 转为商机（客户开发模块未就绪前只更新状态） */
    @PostMapping("/{id}/convert")
    public R<Map<String, Object>> convert(@PathVariable Long id,
                                          @RequestBody(required = false) LeadConvertRequest req) {
        return R.ok(leadService.convert(id, req));
    }
}