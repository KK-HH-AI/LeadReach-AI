package com.leadreach.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.leadreach.crm.client.dto.UserDTO;
import com.leadreach.crm.common.BizException;
import com.leadreach.crm.context.UserContext;
import com.leadreach.crm.dto.req.ActivityCreateRequest;
import com.leadreach.crm.dto.resp.LeadDetailVO;
import com.leadreach.crm.entity.Activity;
import com.leadreach.crm.entity.Lead;
import com.leadreach.crm.mapper.ActivityMapper;
import com.leadreach.crm.mapper.LeadMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 业务线4：线索跟进
 * 跟进记录写入 activity，首次跟进时把线索从 ASSIGNED 推进到 CONTACTED
 */
@Service
public class ActivityService {

    /** 跟进方式白名单 */
    private static final Set<String> ACTIVITY_TYPES = Set.of(
            "CALL", "EMAIL", "WHATSAPP", "LINKEDIN", "MEETING", "NOTE");

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private LeadMapper leadMapper;

    @Autowired
    private LeadService leadService;

    /** 记录一次跟进 */
    @Transactional
    public Long create(ActivityCreateRequest req) {
        if (req == null || req.getBizId() == null) {
            throw new BizException("业务ID不能为空");
        }
        String bizType = (req.getBizType() == null || req.getBizType().isBlank()) ? "LEAD" : req.getBizType().trim();
        if (!"LEAD".equals(bizType)) {
            throw new BizException("本期仅支持线索(LEAD)的跟进记录");
        }
        String activityType = req.getActivityType() == null ? "" : req.getActivityType().trim().toUpperCase();
        if (!ACTIVITY_TYPES.contains(activityType)) {
            throw new BizException("无效的跟进方式：" + req.getActivityType());
        }

        Long userId = leadService.requireUserId();
        Long tenantId = leadService.requireTenantId();
        Lead lead = leadService.requireLead(req.getBizId());
        if (!Objects.equals(lead.getOwnerUserId(), userId)) {
            throw new BizException("只有线索负责人可以记录跟进");
        }

        Activity activity = new Activity();
        activity.setTenantId(tenantId);
        activity.setBizType(bizType);
        activity.setBizId(req.getBizId());
        activity.setActivityType(activityType);
        activity.setContent(req.getContent());
        activity.setResult(req.getResult());
        activity.setOwnerUserId(userId);
        activity.setActivityAt(req.getActivityAt() == null ? LocalDateTime.now() : req.getActivityAt());
        activity.setNextFollowAt(req.getNextFollowAt());
        activityMapper.insert(activity);

        // 首次跟进：ASSIGNED → CONTACTED
        if ("ASSIGNED".equals(lead.getStatus())) {
            lead.setStatus("CONTACTED");
            leadMapper.updateById(lead);
        }
        return activity.getId();
    }

    /** 查询跟进记录：负责人本人或上级可见 */
    public List<LeadDetailVO.ActivityVO> list(String bizType, Long bizId) {
        if (bizId == null) {
            throw new BizException("业务ID不能为空");
        }
        String type = (bizType == null || bizType.isBlank()) ? "LEAD" : bizType.trim();
        Lead lead = leadService.requireLead(bizId);
        checkVisible(lead, type);

        List<Activity> activities = activityMapper.selectList(new QueryWrapper<Activity>()
                .eq("biz_type", type)
                .eq("biz_id", bizId)
                .orderByDesc("activity_at"));

        Set<Long> ownerIds = new LinkedHashSet<>();
        activities.forEach(a -> ownerIds.add(a.getOwnerUserId()));
        Map<Long, UserDTO> userMap = leadService.loadUsers(ownerIds);

        return activities.stream().map(a -> {
            LeadDetailVO.ActivityVO vo = new LeadDetailVO.ActivityVO();
            vo.setId(a.getId());
            vo.setActivityType(a.getActivityType());
            vo.setContent(a.getContent());
            vo.setResult(a.getResult());
            vo.setOwnerUserId(a.getOwnerUserId());
            UserDTO u = userMap.get(a.getOwnerUserId());
            vo.setOwnerName(u == null ? null : u.getName());
            vo.setActivityAt(a.getActivityAt());
            vo.setNextFollowAt(a.getNextFollowAt());
            vo.setCreatedAt(a.getCreatedAt());
            return vo;
        }).toList();
    }

    /** 可见性：负责人本人、老板、同团队主管 */
    private void checkVisible(Lead lead, String bizType) {
        if (!"LEAD".equals(bizType)) {
            // 非线索业务暂不做归属校验，租户条件已由插件保证
            return;
        }
        String jobCode = UserContext.getJobCode();
        if ("BOSS".equals(jobCode)) {
            return;
        }
        if (Objects.equals(lead.getOwnerUserId(), UserContext.getUserId())) {
            return;
        }
        if ("SALES_MANAGER".equals(jobCode) && Objects.equals(lead.getOwnerTeamId(), UserContext.getTeamId())) {
            return;
        }
        throw new BizException("无权查看该线索的跟进记录");
    }
}