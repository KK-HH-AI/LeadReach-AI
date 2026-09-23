package com.leadreach.crm.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.leadreach.crm.entity.Lead;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface LeadMapper extends BaseMapper<Lead> {

    /**
     * 按状态分组统计（看板用）
     * ${ew.customSqlSegment} 会把 Wrapper 里的条件原样拼进去，租户条件由多租户插件自动追加
     */
    @Select("SELECT status AS status, COUNT(*) AS cnt FROM lead ${ew.customSqlSegment} GROUP BY status")
    List<Map<String, Object>> countGroupByStatus(@Param(Constants.WRAPPER) Wrapper<Lead> wrapper);

    /**
     * 按负责人分组统计（成员线索量排行）
     */
    @Select("SELECT owner_user_id AS ownerId, COUNT(*) AS cnt FROM lead ${ew.customSqlSegment} GROUP BY owner_user_id")
    List<Map<String, Object>> countGroupByOwner(@Param(Constants.WRAPPER) Wrapper<Lead> wrapper);

    /**
     * 扫描已超保护期的私有线索（定时任务用，跨租户遍历）
     * 定时任务线程没有请求头，拿不到租户上下文，必须忽略租户过滤，
     * 再在回收时按每行自带的 tenant_id 精确更新。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM lead WHERE pool_type = 'PRIVATE' AND protected_until IS NOT NULL "
            + "AND protected_until < NOW() AND deleted_at IS NULL ORDER BY id LIMIT #{limit}")
    List<Lead> selectExpiredWithoutTenant(@Param("limit") int limit);

    /**
     * 回收单条线索（定时任务用）：按 id + tenant_id 精确更新，避免忽略租户后误伤其他租户
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE lead SET owner_user_id = NULL, owner_team_id = NULL, pool_type = 'PUBLIC', "
            + "protected_until = NULL, updated_at = NOW() WHERE id = #{id} AND tenant_id = #{tenantId}")
    int recycleOneWithoutTenant(@Param("id") Long id, @Param("tenantId") Long tenantId);
}