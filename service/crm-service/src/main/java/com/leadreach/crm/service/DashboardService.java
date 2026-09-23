package com.leadreach.crm.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.leadreach.crm.client.UserClient;
import com.leadreach.crm.client.dto.UserDTO;
import com.leadreach.crm.common.BizException;
import com.leadreach.crm.common.R;
import com.leadreach.crm.config.CrmProperties;
import com.leadreach.crm.context.UserContext;
import com.leadreach.crm.entity.Activity;
import com.leadreach.crm.entity.Lead;
import com.leadreach.crm.mapper.ActivityMapper;
import com.leadreach.crm.mapper.LeadMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 业务线8：线索看板
 *
 * 三个视角：
 *   /my       我的线索概览（任何岗位都可看，口径 = 我是负责人）
 *   /team     团队看板（老板看全公司，销售主管看本团队）
 *   /overview 公司总览（仅老板）
 *
 * 统计口径统一由「作用域函数」决定：每次统计都新建一个 QueryWrapper 再套用作用域，
 * 避免复用同一个 wrapper 导致条件不断叠加。
 * 租户条件由多租户插件自动追加，这里不重复处理。
 */
@Slf4j
@Service
public class DashboardService {

    /** 看板状态分布固定列，缺的补 0，保证前端图表列稳定 */
    private static final List<String> STATUS_COLUMNS = List.of(
            "NEW", "ASSIGNED", "CONTACTED", "QUALIFIED", "CONVERTED", "INVALID");

    /** 成员排行与即将超期列表的返回条数上限 */
    private static final int TOP_LIMIT = 10;

    @Autowired
    private LeadMapper leadMapper;

    @Autowired
    private ActivityMapper activityMapper;

    @Autowired
    private UserClient userClient;

    @Autowired
    private CrmProperties props;

    @Autowired
    private LeadService leadService;

    /** 我的线索概览 */
    public Map<String, Object> my() {
        Long userId = leadService.requireUserId();
        return buildStats(w -> w.eq("owner_user_id", userId), List.of(userId), true);
    }

    /** 团队看板：老板看全公司，销售主管看本团队 */
    public Map<String, Object> team() {
        leadService.requireTenantId();
        String jobCode = UserContext.getJobCode();
        if ("BOSS".equals(jobCode)) {
            // 全租户：作用域为空，跟进数不加人员过滤
            return buildStats(w -> {
            }, null, true);
        }
        if ("SALES_MANAGER".equals(jobCode)) {
            Long teamId = UserContext.getTeamId();
            if (teamId == null) {
                throw new BizException("您还没有被分配到具体团队，请联系老板");
            }
            List<Long> memberIds = teamMemberIds(teamId);
            return buildStats(w -> w.eq("owner_team_id", teamId), memberIds, true);
        }
        throw new BizException("只有老板或销售主管可以查看团队看板");
    }

    /** 公司总览：仅老板 */
    public Map<String, Object> overview() {
        leadService.requireTenantId();
        if (!"BOSS".equals(UserContext.getJobCode())) {
            throw new BizException("只有老板可以查看公司总览");
        }
        return buildStats(w -> {
        }, null, false);
    }

    // ==================================================================
    // 内部实现
    // ==================================================================

    /**
     * 汇总指标
     *
     * @param scope         线索查询作用域
     * @param followUserIds 今日跟进的统计范围；null = 本租户全部（老板视角）
     * @param withRanking   是否输出成员排行与即将超期预警（总览页不需要）
     */
    private Map<String, Object> buildStats(Consumer<QueryWrapper<Lead>> scope,
                                          List<Long> followUserIds, boolean withRanking) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCount", leadMapper.selectCount(fresh(scope)));
        result.put("poolCount", leadMapper.selectCount(fresh(scope).eq("pool_type", "PUBLIC").isNull("owner_user_id")));
        result.put("privateCount", leadMapper.selectCount(fresh(scope).eq("pool_type", "PRIVATE")));

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        STATUS_COLUMNS.forEach(s -> statusCounts.put(s, 0L));
        for (Map<String, Object> row : leadMapper.countGroupByStatus(fresh(scope))) {
            Object status = row.get("status");
            if (status == null) {
                continue;
            }
            statusCounts.put(String.valueOf(status), asLong(row.get("cnt")));
        }
        result.put("statusCounts", statusCounts);

        result.put("todayNewCount", leadMapper.selectCount(fresh(scope).ge("created_at", todayStart)));
        result.put("todayFollowCount", todayFollowCount(todayStart, followUserIds));

        if (withRanking) {
            result.put("memberRanking", memberRanking(scope));
            result.put("expiringSoon", expiringSoon(scope, now));
        }
        return result;
    }

    /** 今日跟进数：按跟进人过滤，null 表示本租户全部 */
    private long todayFollowCount(LocalDateTime todayStart, List<Long> followUserIds) {
        if (followUserIds != null && followUserIds.isEmpty()) {
            // 明确的人员范围内没有任何人，直接 0，不必查库
            return 0L;
        }
        QueryWrapper<Activity> w = new QueryWrapper<Activity>()
                .eq("biz_type", "LEAD")
                .ge("activity_at", todayStart);
        if (followUserIds != null) {
            w.in("owner_user_id", followUserIds);
        }
        Long count = activityMapper.selectCount(w);
        return count == null ? 0L : count;
    }

    /** 成员线索量排行：按负责人分组，倒序取前 N，再补姓名 */
    private List<Map<String, Object>> memberRanking(Consumer<QueryWrapper<Lead>> scope) {
        List<Map<String, Object>> rows = leadMapper.countGroupByOwner(fresh(scope));
        List<Map<String, Object>> top = rows.stream()
                .filter(r -> r.get("ownerId") != null)
                .sorted(Comparator.<Map<String, Object>>comparingLong(r -> -asLong(r.get("cnt"))))
                .limit(TOP_LIMIT)
                .collect(Collectors.toList());
        if (top.isEmpty()) {
            return List.of();
        }

        List<Long> ownerIds = top.stream().map(r -> asLong(r.get("ownerId"))).toList();
        Map<Long, UserDTO> userMap = leadService.loadUsers(ownerIds);

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> row : top) {
            Long ownerId = asLong(row.get("ownerId"));
            UserDTO user = userMap.get(ownerId);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", ownerId);
            item.put("userName", user == null ? null : user.getName());
            item.put("teamName", user == null ? null : user.getTeamName());
            item.put("count", asLong(row.get("cnt")));
            ranking.add(item);
        }
        return ranking;
    }

    /** 即将超出保护期的私有线索：窗口内按截止时间升序取前 N */
    private List<Map<String, Object>> expiringSoon(Consumer<QueryWrapper<Lead>> scope, LocalDateTime now) {
        List<Lead> leads = leadMapper.selectList(fresh(scope)
                .eq("pool_type", "PRIVATE")
                .isNotNull("protected_until")
                .ge("protected_until", now)
                .le("protected_until", now.plusDays(props.getExpiringSoonDays()))
                .orderByAsc("protected_until")
                .last("LIMIT " + TOP_LIMIT));
        if (leads.isEmpty()) {
            return List.of();
        }

        List<Long> ownerIds = leads.stream().map(Lead::getOwnerUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, UserDTO> userMap = leadService.loadUsers(ownerIds);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Lead lead : leads) {
            UserDTO owner = lead.getOwnerUserId() == null ? null : userMap.get(lead.getOwnerUserId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("leadId", lead.getId());
            item.put("title", lead.getTitle());
            item.put("ownerUserId", lead.getOwnerUserId());
            item.put("ownerName", owner == null ? null : owner.getName());
            item.put("protectedUntil", lead.getProtectedUntil());
            items.add(item);
        }
        return items;
    }

    /** 团队在职成员ID，供今日跟进统计使用；user-service 不可用时降级为空列表 */
    private List<Long> teamMemberIds(Long teamId) {
        try {
            R<List<UserDTO>> r = userClient.listUsersByTeam(teamId);
            if (r == null || r.getData() == null) {
                return List.of();
            }
            return r.getData().stream().map(UserDTO::getId).filter(Objects::nonNull).distinct().toList();
        } catch (Exception e) {
            log.warn("调用 user-service 查团队成员失败: {}", e.getMessage());
            return List.of();
        }
    }

    /** 每次统计都用全新的 wrapper，防止条件在多次统计间累积 */
    private QueryWrapper<Lead> fresh(Consumer<QueryWrapper<Lead>> scope) {
        QueryWrapper<Lead> w = new QueryWrapper<>();
        scope.accept(w);
        return w;
    }

    private long asLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }
}