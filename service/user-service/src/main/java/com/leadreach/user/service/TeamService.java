package com.leadreach.user.service;

import java.util.Map;

/**
 * 团队业务接口：老板管理团队、拉人、删除人、分配/修改员工负责的内容
 *
 * 团队模型（V3 起）：公司(租户)下可建多个 team，团队由销售主管带队，
 * boss_employee.team_id 记录员工归属，是各业务模块 TEAM 级数据权限的依据。
 */
public interface TeamService {

    /** 创建团队：幂等确认，确保默认团队存在并返回团队概况 */
    Map<String, Object> createDefaultTeam(Long bossId, Long tenantId);

    /** 团队成员列表：含员工手机号与归属团队名 */
    Map<String, Object> listMembers(Long bossId, Long tenantId);

    /** 团队列表：公司下所有团队，含负责人与人数 */
    Map<String, Object> listGroups(Long bossId, Long tenantId);

    /** 创建团队：指定名称与负责人（负责人必须是本公司销售主管），负责人自动归入所带团队 */
    Map<String, Object> createGroup(Long bossId, Long tenantId, String name, String leaderPhone);

    /** 拉人入队：员工必须先注册；teamId 为 null 时落入默认团队 */
    Map<String, Object> addMember(Long bossId, Long tenantId, String phone, String position, Long teamId);

    /** 开除员工：软删除团队关系（status 1→0 保留历史）并解除公司归属 */
    Map<String, Object> removeMember(Long bossId, Long tenantId, Long employeeId);

    /** 分配负责的内容 / 修改分配内容（同一员工一条分配，存在则更新） */
    Map<String, Object> assign(Long bossId, Long tenantId, Long employeeId, String contentType, String contentId);

    /** 拉人检索：按 姓名/邮箱/手机 模糊匹配员工（自由账号 + 本公司员工） */
    Map<String, Object> searchMembers(Long bossId, Long tenantId, String keyword);

    /** 为团队创建自定义岗位类型 */
    Map<String, Object> createTeamPosition(Long bossId, Long tenantId, Long teamId, String name);

    /** 团队岗位类型列表 */
    Map<String, Object> listTeamPositions(Long bossId, Long tenantId, Long teamId);

    /** 删除团队岗位类型（引用它的成员分类自动置空） */
    Map<String, Object> deleteTeamPosition(Long bossId, Long tenantId, Long teamId, Long positionId);

    /** 把成员分类到团队岗位类型 */
    Map<String, Object> assignTeamPosition(Long bossId, Long tenantId, Long employeeId, Long teamPositionId);

    /** 取公司默认团队ID，不存在则创建（幂等）；供员工加入公司等流程复用 */
    Long ensureDefaultTeamId(Long bossId, Long tenantId);

    /** 把已在本公司的员工分配/调整到指定团队（换团队时原团队的岗位分类自动清空） */
    Map<String, Object> assignTeam(Long bossId, Long tenantId, Long employeeId, Long teamId);

    /** 踢出团队：不开除，回落公司默认团队，原团队岗位分类清空 */
    Map<String, Object> removeFromTeam(Long bossId, Long tenantId, Long employeeId);
}
