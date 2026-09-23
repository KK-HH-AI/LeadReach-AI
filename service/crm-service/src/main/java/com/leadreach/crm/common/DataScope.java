package com.leadreach.crm.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.leadreach.crm.context.UserContext;
import com.leadreach.crm.entity.Lead;

/**
 * 数据权限：按岗位给线索查询拼过滤条件
 *
 * 岗位（X-Job-Code）与范围：
 *   BOSS           → ALL   本租户全部线索
 *   SALES_MANAGER  → TEAM  本团队线索（owner_team_id）
 *   SALES_REP      → SELF  仅自己负责的线索
 *   OPERATOR       → 公共池 + 自己创建的线索（运营不参与销售跟进）
 *
 * 为什么不用全局拦截器：线索的「详情」「领取」「分配」不能带 owner 过滤——
 * 公共池线索的 owner_user_id 为空，一旦被拦掉就永远领不到，所以只在列表/统计查询上显式叠加。
 * 租户条件由 TenantLineInnerInterceptor 自动追加，这里不重复处理。
 */
public final class DataScope {

    private DataScope() {
    }

    public static void apply(QueryWrapper<Lead> wrapper) {
        Long userId = UserContext.getUserId();
        Long teamId = UserContext.getTeamId();
        String jobCode = UserContext.getJobCode();

        if (jobCode == null) {
            // 拿不到岗位信息：按最小权限处理，只能看自己的
            onlyMine(wrapper, userId);
            return;
        }

        switch (jobCode) {
            case "BOSS" -> {
                // 本租户全部，无需额外条件
            }
            case "SALES_MANAGER" -> {
                if (teamId != null) {
                    wrapper.eq("owner_team_id", teamId);
                } else {
                    // 主管还没被分配到具体团队，退化为只看自己的
                    onlyMine(wrapper, userId);
                }
            }
            case "SALES_REP" -> onlyMine(wrapper, userId);
            case "OPERATOR" -> wrapper.and(w -> w.eq("pool_type", "PUBLIC")
                    .or()
                    .eq("created_by", userId == null ? -1L : userId));
            default -> onlyMine(wrapper, userId);
        }
    }

    private static void onlyMine(QueryWrapper<Lead> wrapper, Long userId) {
        if (userId == null) {
            // 既无岗位也无用户ID：拼一个永远不成立的条件，保证查不到数据
            wrapper.eq("owner_user_id", -1L);
            return;
        }
        wrapper.eq("owner_user_id", userId);
    }
}