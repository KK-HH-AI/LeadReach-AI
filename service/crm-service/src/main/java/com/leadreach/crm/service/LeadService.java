package com.leadreach.crm.service;

import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leadreach.crm.client.UserClient;
import com.leadreach.crm.client.dto.UserDTO;
import com.leadreach.crm.common.BizException;
import com.leadreach.crm.common.DataScope;
import com.leadreach.crm.common.PageResult;
import com.leadreach.crm.common.R;
import com.leadreach.crm.config.CrmProperties;
import com.leadreach.crm.context.UserContext;
import com.leadreach.crm.dto.req.LeadAssignRequest;
import com.leadreach.crm.dto.req.LeadConvertRequest;
import com.leadreach.crm.dto.req.LeadCreateRequest;
import com.leadreach.crm.dto.req.LeadStatusRequest;
import com.leadreach.crm.dto.resp.LeadDetailVO;
import com.leadreach.crm.dto.resp.LeadVO;
import com.leadreach.crm.entity.Activity;
import com.leadreach.crm.entity.AssignLog;
import com.leadreach.crm.entity.Contact;
import com.leadreach.crm.entity.Lead;
import com.leadreach.crm.entity.LeadCompany;
import com.leadreach.crm.mapper.ActivityMapper;
import com.leadreach.crm.mapper.AssignLogMapper;
import com.leadreach.crm.mapper.ContactMapper;
import com.leadreach.crm.mapper.LeadCompanyMapper;
import com.leadreach.crm.mapper.LeadMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 线索业务：入池、查询、分配、领取、状态流转、退回公海、转商机
 */
@Slf4j
@Service
public class LeadService {

    /** 公司名里的法律后缀，做标准化时剔除，避免 "ABC GmbH" 与 "ABC" 被当成两家公司 */
    private static final Set<String> LEGAL_SUFFIXES = Set.of(
            "gmbh", "ltd", "limited", "inc", "llc", "co", "corp", "corporation", "company",
            "sa", "sas", "srl", "bv", "nv", "ag", "ab", "as", "oy", "aps", "pte", "pvt",
            "plc", "kft", "sp", "spa", "zoo", "grupo", "group", "holdings", "international");

    /** 状态流转白名单：key 是当前状态，value 是允许流转到的状态 */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "NEW", Set.of("ASSIGNED", "INVALID"),
            "ASSIGNED", Set.of("CONTACTED", "INVALID"),
            "CONTACTED", Set.of("QUALIFIED", "INVALID"),
            "QUALIFIED", Set.of("CONVERTED", "INVALID"),
            "CONVERTED", Set.of(),
            "INVALID", Set.of()
    );

    @Autowired
    private LeadMapper leadMapper;

    @Autowired
    private LeadCompanyMapper leadCompanyMapper;

    @Autowired
    private ContactMapper contactMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private AssignLogMapper assignLogMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private CrmProperties props;

    // ==================================================================
    // 业务线1：线索入池
    // ==================================================================

    /** 创建线索：公司去重复用 → 联系人 → 线索 */
    @Transactional
    public Long create(LeadCreateRequest req) {
        Long tenantId = requireTenantId();
        Long userId = requireUserId();

        LeadCreateRequest.CompanyInfo companyInfo = req.getCompany();
        if (companyInfo == null || companyInfo.getName() == null || companyInfo.getName().isBlank()) {
            throw new BizException("公司名称不能为空");
        }

        String normalizedName = normalize(companyInfo.getName());
        String domain = extractDomain(companyInfo.getWebsite());
        String dedupHash = SecureUtil.md5(tenantId + "|" + (domain != null ? domain : normalizedName));
        String sourceType = blankToDefault(req.getSourceType(), "MANUAL");

        LeadCompany company = findOrCreateCompany(companyInfo, normalizedName, domain, dedupHash, sourceType, tenantId);

        Long contactId = null;
        if (hasContact(req.getContact())) {
            Contact contact = new Contact();
            contact.setTenantId(tenantId);
            contact.setLeadCompanyId(company.getId());
            contact.setName(req.getContact().getName());
            contact.setJobTitle(req.getContact().getJobTitle());
            contact.setDepartment(req.getContact().getDepartment());
            contact.setSeniority(req.getContact().getSeniority());
            contact.setEmail(req.getContact().getEmail());
            contact.setPhone(req.getContact().getPhone());
            contact.setWhatsapp(req.getContact().getWhatsapp());
            contact.setLinkedinUrl(req.getContact().getLinkedinUrl());
            contact.setCountryCode(req.getContact().getCountryCode());
            contact.setLanguage(req.getContact().getLanguage());
            contact.setIsKp(Boolean.TRUE.equals(req.getContact().getIsKp()) ? 1 : 0);
            contact.setStatus("ACTIVE");
            contactMapper.insert(contact);
            contactId = contact.getId();
        }

        Lead lead = new Lead();
        lead.setTenantId(tenantId);
        lead.setLeadCompanyId(company.getId());
        lead.setPrimaryContactId(contactId);
        lead.setTitle(blankToDefault(req.getTitle(), companyInfo.getName()));
        lead.setSourceType(sourceType);
        lead.setSourceUrl(req.getSourceUrl());
        lead.setRemark(req.getRemark());
        lead.setStatus("NEW");
        lead.setPoolType("PUBLIC");
        lead.setScore(0);
        lead.setCreatedBy(userId);
        lead.setDedupKey(dedupHash);
        leadMapper.insert(lead);
        return lead.getId();
    }

    /** 批量创建线索（后续采集模块使用）：单条失败不影响其他条目 */
    @Transactional
    public Map<String, Object> batchCreate(List<LeadCreateRequest> list) {
        if (list == null || list.isEmpty()) {
            throw new BizException("批量创建列表不能为空");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", i);
            try {
                item.put("leadId", create(list.get(i)));
                item.put("success", true);
                success++;
            } catch (Exception e) {
                item.put("success", false);
                item.put("message", e.getMessage());
            }
            items.add(item);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("successCount", success);
        result.put("failCount", list.size() - success);
        result.put("items", items);
        return result;
    }

    // ==================================================================
    // 业务线：查询（公共池 / 我的 / 列表 / 详情）
    // ==================================================================

    /** 公共线索池：本租户所有未分配的线索，不再叠加个人数据范围 */
    public PageResult<LeadVO> pool(String status, String keyword, long page, long size) {
        QueryWrapper<Lead> w = new QueryWrapper<>();
        w.eq("pool_type", "PUBLIC").isNull("owner_user_id");
        applyCommonFilters(w, status, keyword);
        w.orderByDesc("created_at");
        Page<Lead> p = leadMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p, toVOs(p.getRecords()));
    }

    /** 我的线索：不论什么岗位，都只看 owner_user_id = 自己 */
    public PageResult<LeadVO> my(String status, String keyword, long page, long size) {
        QueryWrapper<Lead> w = new QueryWrapper<>();
        w.eq("owner_user_id", requireUserId());
        applyCommonFilters(w, status, keyword);
        w.orderByDesc("updated_at");
        Page<Lead> p = leadMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p, toVOs(p.getRecords()));
    }

    /** 线索列表：按岗位叠加数据范围（BOSS全部 / 主管本团队 / 销售本人 / 运营公共池） */
    public PageResult<LeadVO> list(String status, String poolType, String keyword, Long ownerUserId, long page, long size) {
        QueryWrapper<Lead> w = new QueryWrapper<>();
        DataScope.apply(w);
        applyCommonFilters(w, status, keyword);
        if (poolType != null && !poolType.isBlank()) {
            w.eq("pool_type", poolType);
        }
        if (ownerUserId != null) {
            w.eq("owner_user_id", ownerUserId);
        }
        w.orderByDesc("updated_at");
        Page<Lead> p = leadMapper.selectPage(new Page<>(page, size), w);
        return PageResult.of(p, toVOs(p.getRecords()));
    }

    /** 线索详情：本租户内可见（不叠加数据范围，公共池线索也要能打开） */
    public LeadDetailVO detail(Long id) {
        Lead lead = requireLead(id);
        LeadDetailVO vo = new LeadDetailVO();
        fillBase(vo, lead, null, null, null, null);

        LeadCompany company = leadCompanyMapper.selectById(lead.getLeadCompanyId());
        if (company != null) {
            vo.setCompanyName(company.getName());
            vo.setCountryCode(company.getCountryCode());
            vo.setWebsite(company.getWebsite());
            vo.setIndustryName(company.getIndustryName());
            vo.setAddress(company.getAddress());
        }
        vo.setSourceType(lead.getSourceType());
        vo.setSourceUrl(lead.getSourceUrl());

        // 联系人：公司下的全部联系人，主联系人排在最前
        List<Contact> contacts = contactMapper.selectList(new QueryWrapper<Contact>()
                .eq("lead_company_id", lead.getLeadCompanyId())
                .orderByDesc("is_kp"));
        vo.setContacts(contacts.stream().map(c -> {
            LeadDetailVO.ContactVO cv = new LeadDetailVO.ContactVO();
            cv.setId(c.getId());
            cv.setName(c.getName());
            cv.setJobTitle(c.getJobTitle());
            cv.setDepartment(c.getDepartment());
            cv.setSeniority(c.getSeniority());
            cv.setEmail(c.getEmail());
            cv.setEmailStatus(c.getEmailStatus());
            cv.setPhone(c.getPhone());
            cv.setWhatsapp(c.getWhatsapp());
            cv.setLinkedinUrl(c.getLinkedinUrl());
            cv.setIsKp(c.getIsKp());
            return cv;
        }).toList());
        contacts.stream()
                .filter(c -> Objects.equals(c.getId(), lead.getPrimaryContactId()))
                .findFirst()
                .ifPresent(c -> vo.setContactName(c.getName()));
        if (vo.getContactName() == null && !contacts.isEmpty()) {
            vo.setContactName(contacts.get(0).getName());
        }

        // 跟进记录（倒序）
        List<Activity> activities = activityMapper.selectList(new QueryWrapper<Activity>()
                .eq("biz_type", "LEAD")
                .eq("biz_id", id)
                .orderByDesc("activity_at"));
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(lead.getOwnerUserId());
        activities.forEach(a -> userIds.add(a.getOwnerUserId()));

        List<AssignLog> logs = assignLogMapper.selectList(new QueryWrapper<AssignLog>()
                .eq("lead_id", id)
                .orderByDesc("created_at"));
        logs.forEach(l -> {
            userIds.add(l.getFromOwnerUserId());
            userIds.add(l.getToOwnerUserId());
            userIds.add(l.getOperatorId());
        });
        Map<Long, UserDTO> userMap = loadUsers(userIds);

        vo.setActivities(activities.stream().map(a -> {
            LeadDetailVO.ActivityVO av = new LeadDetailVO.ActivityVO();
            av.setId(a.getId());
            av.setActivityType(a.getActivityType());
            av.setContent(a.getContent());
            av.setResult(a.getResult());
            av.setOwnerUserId(a.getOwnerUserId());
            av.setOwnerName(displayName(userMap, a.getOwnerUserId()));
            av.setActivityAt(a.getActivityAt());
            av.setNextFollowAt(a.getNextFollowAt());
            av.setCreatedAt(a.getCreatedAt());
            return av;
        }).toList());
        if (!activities.isEmpty()) {
            vo.setLastFollowAt(activities.get(0).getActivityAt());
            vo.setNextFollowAt(activities.get(0).getNextFollowAt());
        }

        vo.setAssignLogs(logs.stream().map(l -> {
            LeadDetailVO.AssignLogVO lv = new LeadDetailVO.AssignLogVO();
            lv.setId(l.getId());
            lv.setAction(l.getAction());
            lv.setFromOwnerUserId(l.getFromOwnerUserId());
            lv.setFromOwnerName(displayName(userMap, l.getFromOwnerUserId()));
            lv.setToOwnerUserId(l.getToOwnerUserId());
            lv.setToOwnerName(displayName(userMap, l.getToOwnerUserId()));
            lv.setReason(l.getReason());
            lv.setOperatorId(l.getOperatorId());
            lv.setCreatedAt(l.getCreatedAt());
            return lv;
        }).toList());

        UserDTO owner = lead.getOwnerUserId() == null ? null : userMap.get(lead.getOwnerUserId());
        vo.setOwnerName(owner == null ? null : owner.getName());
        vo.setOwnerTeamName(owner == null ? null : owner.getTeamName());
        return vo;
    }

    // ==================================================================
    // 业务线2：线索分配
    // ==================================================================

    /** 可分配的销售专员：分配弹窗的下拉数据源，范围由 user-service 按岗位裁好 */
    public List<UserDTO> assignableUsers() {
        requireTenantId();
        requireUserId();
        String jobCode = UserContext.getJobCode();
        if (!"BOSS".equals(jobCode) && !"SALES_MANAGER".equals(jobCode)) {
            throw new BizException("只有老板或销售主管可以分配线索");
        }
        try {
            R<List<UserDTO>> r = userClient.listAssignableUsers();
            if (r == null || r.getData() == null) {
                return List.of();
            }
            return r.getData();
        } catch (Exception e) {
            log.warn("调用 user-service 查可分配人员失败: {}", e.getMessage());
            throw new BizException("用户服务暂不可用，请稍后重试");
        }
    }

    @Transactional
    public void assign(Long id, LeadAssignRequest req) {
        if (req == null || req.getToUserId() == null) {
            throw new BizException("目标用户不能为空");
        }
        Long tenantId = requireTenantId();
        Long operatorId = requireUserId();
        String jobCode = UserContext.getJobCode();
        if (!"BOSS".equals(jobCode) && !"SALES_MANAGER".equals(jobCode)) {
            throw new BizException("只有老板或销售主管可以分配线索");
        }

        Lead lead = requireLead(id);
        // 主管只能动公共池或本团队的线索
        if ("SALES_MANAGER".equals(jobCode) && !"PUBLIC".equals(lead.getPoolType())
                && !Objects.equals(lead.getOwnerTeamId(), UserContext.getTeamId())) {
            throw new BizException("只能分配本团队的线索");
        }

        UserDTO target = fetchUser(req.getToUserId());
        if (!Objects.equals(target.getTenantId(), tenantId)) {
            throw new BizException("目标用户不属于本公司");
        }
        if (!"SALES_REP".equals(target.getJobCode())) {
            throw new BizException("只能分配给销售专员");
        }
        if ("SALES_MANAGER".equals(jobCode) && !Objects.equals(target.getTeamId(), UserContext.getTeamId())) {
            throw new BizException("只能分配给本团队的成员");
        }

        Long fromOwner = lead.getOwnerUserId();
        Long fromTeam = lead.getOwnerTeamId();

        lead.setOwnerUserId(target.getId());
        lead.setOwnerTeamId(target.getTeamId());
        lead.setPoolType("PRIVATE");
        lead.setStatus("ASSIGNED");
        lead.setProtectedUntil(LocalDateTime.now().plusDays(props.getProtectDays()));
        leadMapper.updateById(lead);

        writeLog(lead.getId(), "ASSIGN", fromOwner, target.getId(), fromTeam, target.getTeamId(),
                req.getReason(), operatorId);
    }

    // ==================================================================
    // 业务线3：线索领取
    // ==================================================================

    @Transactional
    public void claim(Long id) {
        Long userId = requireUserId();
        requireTenantId();
        if (!"SALES_REP".equals(UserContext.getJobCode())) {
            throw new BizException("只有销售专员可以领取线索");
        }
        Lead lead = requireLead(id);
        if (!"PUBLIC".equals(lead.getPoolType()) || lead.getOwnerUserId() != null) {
            throw new BizException("线索已被领取");
        }

        Long myTeamId = UserContext.getTeamId();
        // 条件更新：依赖 affected rows 判定，防止并发下两人重复领取同一条线索
        UpdateWrapper<Lead> w = new UpdateWrapper<>();
        w.set("owner_user_id", userId)
                .set("owner_team_id", myTeamId)
                .set("pool_type", "PRIVATE")
                .set("status", "ASSIGNED")
                .set("protected_until", LocalDateTime.now().plusDays(props.getProtectDays()))
                .eq("id", id)
                .eq("pool_type", "PUBLIC")
                .isNull("owner_user_id");
        int affected = leadMapper.update(null, w);
        if (affected != 1) {
            throw new BizException("线索已被他人领取");
        }

        writeLog(id, "CLAIM", null, userId, null, myTeamId, "销售领取", userId);
    }

    // ==================================================================
    // 业务线5：状态流转
    // ==================================================================

    @Transactional
    public void updateStatus(Long id, LeadStatusRequest req) {
        if (req == null || req.getStatus() == null || req.getStatus().isBlank()) {
            throw new BizException("目标状态不能为空");
        }
        Lead lead = requireLead(id);
        Long userId = requireUserId();
        if (!Objects.equals(lead.getOwnerUserId(), userId)) {
            throw new BizException("只有线索负责人可以更新状态");
        }
        String to = req.getStatus().trim().toUpperCase();
        if ("CONVERTED".equals(to)) {
            throw new BizException("转为商机请调用 /convert 接口");
        }
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(lead.getStatus(), Set.of());
        if (!allowed.contains(to)) {
            throw new BizException("不允许从 " + lead.getStatus() + " 变更为 " + to);
        }
        lead.setStatus(to);
        if (req.getRemark() != null && !req.getRemark().isBlank()) {
            lead.setRemark(req.getRemark());
        }
        leadMapper.updateById(lead);
    }

    // ==================================================================
    // 业务线6：退回公海
    // ==================================================================

    /** 手动退回公海：老板或主管 */
    @Transactional
    public void recycle(Long id, String reason) {
        Lead lead = requireLead(id);
        String jobCode = UserContext.getJobCode();
        if (!"BOSS".equals(jobCode) && !"SALES_MANAGER".equals(jobCode)) {
            throw new BizException("只有老板或销售主管可以退回公海");
        }
        if ("SALES_MANAGER".equals(jobCode) && !Objects.equals(lead.getOwnerTeamId(), UserContext.getTeamId())) {
            throw new BizException("只能退回本团队的线索");
        }
        if ("PUBLIC".equals(lead.getPoolType())) {
            throw new BizException("线索已在公共池");
        }
        doRecycle(lead, blankToDefault(reason, "主管手动回收"), requireUserId());
    }

    /**
     * 实际回收动作：清空归属、回到公共池、保留原状态
     * 用 UpdateWrapper 显式 set null：MyBatis-Plus 默认策略会跳过实体里的 null 字段，清不掉归属
     */
    private void doRecycle(Lead lead, String reason, Long operatorId) {
        Long fromOwner = lead.getOwnerUserId();
        Long fromTeam = lead.getOwnerTeamId();

        UpdateWrapper<Lead> w = new UpdateWrapper<>();
        w.set("owner_user_id", null)
                .set("owner_team_id", null)
                .set("pool_type", "PUBLIC")
                .set("protected_until", null)
                .eq("id", lead.getId());
        leadMapper.update(null, w);

        writeLog(lead.getId(), "RECYCLE", fromOwner, null, fromTeam, null, reason, operatorId);
    }

    // ==================================================================
    // 业务线7：线索转商机（客户开发模块未就绪，先只改状态）
    // ==================================================================

    @Transactional
    public Map<String, Object> convert(Long id, LeadConvertRequest req) {
        Lead lead = requireLead(id);
        Long userId = requireUserId();
        if (!Objects.equals(lead.getOwnerUserId(), userId)) {
            throw new BizException("只有线索负责人可以转商机");
        }
        if (!"QUALIFIED".equals(lead.getStatus())) {
            throw new BizException("只有已确认需求(QUALIFIED)的线索才能转商机");
        }

        lead.setStatus("CONVERTED");
        lead.setConvertedAt(LocalDateTime.now());
        // 客户开发模块就绪后：调用其接口创建商机，再回填 convertedOpportunityId
        leadMapper.updateById(lead);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("leadId", lead.getId());
        result.put("opportunityId", lead.getConvertedOpportunityId());
        result.put("opportunityName", req == null ? null : req.getOpportunityName());
        result.put("message", "线索已标记为已转化，商机由客户开发模块创建");
        return result;
    }

    // ==================================================================
    // 供定时任务复用
    // ==================================================================

    /** 系统回收（定时任务）：不经过权限校验，按线索自带的租户精确更新 */
    @Transactional
    public void recycleBySystem(Lead lead, String reason) {
        if (leadMapper.recycleOneWithoutTenant(lead.getId(), lead.getTenantId()) == 0) {
            return;
        }
        AssignLog log = new AssignLog();
        log.setTenantId(lead.getTenantId());
        log.setLeadId(lead.getId());
        log.setAction("RECYCLE");
        log.setFromOwnerUserId(lead.getOwnerUserId());
        log.setFromTeamId(lead.getOwnerTeamId());
        log.setReason(reason);
        // 系统操作没有操作人，用原负责人占位，保证日志可追溯
        log.setOperatorId(lead.getOwnerUserId() == null ? 0L : lead.getOwnerUserId());
        assignLogMapper.insert(log);
    }

    // ==================================================================
    // 内部工具
    // ==================================================================

    private LeadCompany findOrCreateCompany(LeadCreateRequest.CompanyInfo info, String normalizedName,
                                           String domain, String dedupHash, String sourceType, Long tenantId) {
        LeadCompany company = leadCompanyMapper.selectOne(new QueryWrapper<LeadCompany>()
                .eq("dedup_hash", dedupHash)
                .last("LIMIT 1"));
        if (company == null) {
            // 兜底：先按标准化名称再找一次，覆盖「先无官网后有官网」的重复提交
            company = leadCompanyMapper.selectOne(new QueryWrapper<LeadCompany>()
                    .eq("normalized_name", normalizedName)
                    .last("LIMIT 1"));
        }
        if (company != null) {
            return company;
        }
        company = new LeadCompany();
        company.setTenantId(tenantId);
        company.setName(info.getName().trim());
        company.setNormalizedName(normalizedName);
        company.setDomain(domain);
        company.setWebsite(info.getWebsite());
        company.setCountryCode(info.getCountryCode());
        company.setRegionCode(info.getRegionCode());
        company.setIndustryCode(info.getIndustryCode());
        company.setIndustryName(info.getIndustryName());
        company.setAddress(info.getAddress());
        company.setSourceType(sourceType);
        company.setDedupHash(dedupHash);
        company.setStatus("ACTIVE");
        leadCompanyMapper.insert(company);
        return company;
    }

    /** 列表通用筛选：状态 + 关键字（标题或公司名） */
    private void applyCommonFilters(QueryWrapper<Lead> w, String status, String keyword) {
        if (status != null && !status.isBlank()) {
            w.eq("status", status.trim().toUpperCase());
        }
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            List<Long> companyIds = leadCompanyMapper.selectList(new QueryWrapper<LeadCompany>()
                            .select("id")
                            .like("name", kw))
                    .stream().map(LeadCompany::getId).toList();
            if (companyIds.isEmpty()) {
                w.like("title", kw);
            } else {
                w.and(x -> x.like("title", kw).or().in("lead_company_id", companyIds));
            }
        }
    }

    /** 批量转 VO：公司、联系人、负责人姓名一次查完，避免 N+1 */
    private List<LeadVO> toVOs(List<Lead> leads) {
        if (leads == null || leads.isEmpty()) {
            return List.of();
        }
        List<Long> leadIds = leads.stream().map(Lead::getId).toList();
        Set<Long> companyIds = leads.stream().map(Lead::getLeadCompanyId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> contactIds = leads.stream().map(Lead::getPrimaryContactId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> ownerIds = leads.stream().map(Lead::getOwnerUserId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, LeadCompany> companyMap = companyIds.isEmpty() ? Map.of()
                : leadCompanyMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(LeadCompany::getId, c -> c, (a, b) -> a));
        Map<Long, Contact> contactMap = contactIds.isEmpty() ? Map.of()
                : contactMapper.selectBatchIds(contactIds).stream()
                .collect(Collectors.toMap(Contact::getId, c -> c, (a, b) -> a));
        Map<Long, UserDTO> userMap = loadUsers(ownerIds);

        // 最近一次跟进 / 下次跟进
        Map<Long, Activity> latestActivityMap = new HashMap<>();
        List<Activity> activities = activityMapper.selectList(new QueryWrapper<Activity>()
                .eq("biz_type", "LEAD")
                .in("biz_id", leadIds)
                .orderByDesc("activity_at"));
        activities.forEach(a -> latestActivityMap.putIfAbsent(a.getBizId(), a));

        List<LeadVO> result = new ArrayList<>();
        for (Lead lead : leads) {
            LeadVO vo = new LeadVO();
            fillBase(vo, lead, companyMap.get(lead.getLeadCompanyId()), contactMap.get(lead.getPrimaryContactId()),
                    userMap, latestActivityMap.get(lead.getId()));
            result.add(vo);
        }
        return result;
    }

    /** 填充列表项公共字段 */
    private void fillBase(LeadVO vo, Lead lead, LeadCompany company, Contact contact,
                          Map<Long, UserDTO> userMap, Activity latestActivity) {
        vo.setId(lead.getId());
        vo.setTitle(lead.getTitle());
        vo.setStatus(lead.getStatus());
        vo.setPoolType(lead.getPoolType());
        vo.setScore(lead.getScore());
        vo.setGrade(lead.getGrade());
        vo.setLeadCompanyId(lead.getLeadCompanyId());
        vo.setPrimaryContactId(lead.getPrimaryContactId());
        vo.setOwnerUserId(lead.getOwnerUserId());
        vo.setOwnerTeamId(lead.getOwnerTeamId());
        vo.setProtectedUntil(lead.getProtectedUntil());
        vo.setConvertedAt(lead.getConvertedAt());
        vo.setConvertedOpportunityId(lead.getConvertedOpportunityId());
        vo.setRemark(lead.getRemark());
        vo.setCreatedAt(lead.getCreatedAt());
        vo.setUpdatedAt(lead.getUpdatedAt());
        if (company != null) {
            vo.setCompanyName(company.getName());
            vo.setCountryCode(company.getCountryCode());
        }
        if (contact != null) {
            vo.setContactName(contact.getName());
            vo.setContactEmail(contact.getEmail());
        }
        if (latestActivity != null) {
            vo.setLastFollowAt(latestActivity.getActivityAt());
            vo.setNextFollowAt(latestActivity.getNextFollowAt());
        }
        if (lead.getOwnerUserId() != null && userMap != null) {
            UserDTO owner = userMap.get(lead.getOwnerUserId());
            if (owner != null) {
                vo.setOwnerName(owner.getName());
                vo.setOwnerTeamName(owner.getTeamName());
            }
        }
    }

    /** 批量查用户信息，user-service 不可用时降级为「不带姓名」，不影响列表返回 */
    Map<Long, UserDTO> loadUsers(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        String param = ids.stream().filter(Objects::nonNull).distinct()
                .map(String::valueOf).collect(Collectors.joining(","));
        if (param.isEmpty()) {
            return Map.of();
        }
        try {
            R<List<UserDTO>> r = userClient.listUsers(param);
            if (r == null || r.getData() == null) {
                return Map.of();
            }
            return r.getData().stream().collect(Collectors.toMap(UserDTO::getId, u -> u, (a, b) -> a));
        } catch (Exception e) {
            log.warn("调用 user-service 批量查用户失败: {}", e.getMessage());
            return Map.of();
        }
    }

    /** 单个用户，查不到直接抛业务异常（分配场景必须拿到目标用户） */
    UserDTO fetchUser(Long userId) {
        try {
            R<UserDTO> r = userClient.getUser(userId);
            if (r == null || r.getCode() == null || r.getCode() != 0 || r.getData() == null) {
                throw new BizException(r == null ? "调用 user-service 失败" : r.getMsg());
            }
            return r.getData();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用 user-service 查用户失败: {}", e.getMessage());
            throw new BizException("用户服务暂不可用，请稍后重试");
        }
    }

    private String displayName(Map<Long, UserDTO> userMap, Long userId) {
        if (userId == null || userMap == null) {
            return null;
        }
        UserDTO u = userMap.get(userId);
        return u == null ? null : u.getName();
    }

    private void writeLog(Long leadId, String action, Long fromOwner, Long toOwner,
                          Long fromTeam, Long toTeam, String reason, Long operatorId) {
        AssignLog log = new AssignLog();
        log.setTenantId(UserContext.getTenantId());
        log.setLeadId(leadId);
        log.setAction(action);
        log.setFromOwnerUserId(fromOwner);
        log.setToOwnerUserId(toOwner);
        log.setFromTeamId(fromTeam);
        log.setToTeamId(toTeam);
        log.setReason(reason);
        log.setOperatorId(operatorId == null ? 0L : operatorId);
        assignLogMapper.insert(log);
    }

    Lead requireLead(Long id) {
        if (id == null) {
            throw new BizException("线索ID不能为空");
        }
        Lead lead = leadMapper.selectById(id);
        if (lead == null) {
            throw new BizException("线索不存在");
        }
        return lead;
    }

    Long requireTenantId() {
        Long tenantId = UserContext.getTenantId();
        if (tenantId == null) {
            throw new BizException("缺少租户上下文，请先创建或加入公司");
        }
        return tenantId;
    }

    Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException("缺少用户身份");
        }
        return userId;
    }

    /** 公司名标准化：去标点、去法律后缀、统一小写 */
    private String normalize(String name) {
        String[] tokens = name.toLowerCase().split("[^a-z0-9\\u4e00-\\u9fa5]+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isBlank() || LEGAL_SUFFIXES.contains(token)) {
                continue;
            }
            sb.append(token);
        }
        return sb.length() == 0 ? name.toLowerCase().trim() : sb.toString();
    }

    /** 从网址里取域名，用于公司去重 */
    private String extractDomain(String website) {
        if (website == null || website.isBlank()) {
            return null;
        }
        String s = website.trim().toLowerCase().replaceFirst("^[a-z]+://", "");
        int slash = s.indexOf('/');
        if (slash >= 0) {
            s = s.substring(0, slash);
        }
        if (s.startsWith("www.")) {
            s = s.substring(4);
        }
        return s.isBlank() ? null : s;
    }

    private String blankToDefault(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    /** 联系人至少要有一个可识别的标识，否则视为没传 */
    private boolean hasContact(LeadCreateRequest.ContactInfo contact) {
        if (contact == null) {
            return false;
        }
        return !isBlank(contact.getName()) || !isBlank(contact.getEmail())
                || !isBlank(contact.getPhone()) || !isBlank(contact.getLinkedinUrl());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}