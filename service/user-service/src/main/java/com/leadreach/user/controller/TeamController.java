package com.leadreach.user.controller;

import com.leadreach.user.context.TenantContext;
import com.leadreach.user.service.TeamService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 团队接口：只做身份解析（X-User-Id + TenantContext）与参数接收，
 * 业务逻辑在 TeamService（service 包），老板身份复核在 Service 层完成
 */
@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    // 创建团队：幂等确认，返回团队概况
    @PostMapping
    public Map<String, Object> create(HttpServletRequest request) {
        return teamService.createDefaultTeam(parseUid(request), TenantContext.getTenantId());
    }

    // 团队成员列表（兼容 /teams 与 /teams/ 两种路径）
    @GetMapping({"", "/"})
    public Map<String, Object> list(HttpServletRequest request) {
        return teamService.listMembers(parseUid(request), TenantContext.getTenantId());
    }

    // 团队列表：公司下的所有团队，含负责人与人数
    @GetMapping("/groups")
    public Map<String, Object> listGroups(HttpServletRequest request) {
        return teamService.listGroups(parseUid(request), TenantContext.getTenantId());
    }

    // 创建团队：指定名称与负责人（负责人必须是本公司销售主管）
    @PostMapping("/groups")
    public Map<String, Object> createGroup(HttpServletRequest request, @RequestBody Map<String, String> body) {
        return teamService.createGroup(parseUid(request), TenantContext.getTenantId(),
                body.get("name"), body.get("leaderPhone"));
    }

    // 拉人入队：员工必须先注册；teamId 可省略，缺省落在默认团队
    @PostMapping("/members")
    public Map<String, Object> addMember(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long teamId = null;
        String teamIdStr = body.get("teamId");
        if (teamIdStr != null && !teamIdStr.isBlank()) {
            teamId = Long.parseLong(teamIdStr.trim());
        }
        return teamService.addMember(parseUid(request), TenantContext.getTenantId(),
                body.get("phone"), body.get("position"), teamId);
    }

    // 删除人：移出团队并解除员工的公司归属
    @DeleteMapping("/members/{employeeId}")
    public Map<String, Object> removeMember(HttpServletRequest request, @PathVariable Long employeeId) {
        return teamService.removeMember(parseUid(request), TenantContext.getTenantId(), employeeId);
    }

    // 分配负责的内容 / 修改分配内容（同一员工一条分配，存在则更新）
    @PutMapping("/members/{employeeId}/assignment")
    public Map<String, Object> assign(HttpServletRequest request, @PathVariable Long employeeId,
                                      @RequestBody Map<String, String> body) {
        return teamService.assign(parseUid(request), TenantContext.getTenantId(), employeeId,
                body.get("contentType"), body.get("contentId"));
    }

    // 拉人检索：按 姓名/邮箱/手机 模糊匹配员工
    @GetMapping("/members/search")
    public Map<String, Object> searchMembers(HttpServletRequest request,
                                             @RequestParam("keyword") String keyword) {
        return teamService.searchMembers(parseUid(request), TenantContext.getTenantId(), keyword);
    }

    // 为团队创建自定义岗位类型
    @PostMapping("/{teamId}/positions")
    public Map<String, Object> createTeamPosition(HttpServletRequest request, @PathVariable Long teamId,
                                                  @RequestBody Map<String, String> body) {
        return teamService.createTeamPosition(parseUid(request), TenantContext.getTenantId(),
                teamId, body.get("name"));
    }

    // 团队岗位类型列表
    @GetMapping("/{teamId}/positions")
    public Map<String, Object> listTeamPositions(HttpServletRequest request, @PathVariable Long teamId) {
        return teamService.listTeamPositions(parseUid(request), TenantContext.getTenantId(), teamId);
    }

    // 删除团队岗位类型（引用它的成员分类自动置空）
    @DeleteMapping("/{teamId}/positions/{positionId}")
    public Map<String, Object> deleteTeamPosition(HttpServletRequest request, @PathVariable Long teamId,
                                                  @PathVariable Long positionId) {
        return teamService.deleteTeamPosition(parseUid(request), TenantContext.getTenantId(), teamId, positionId);
    }

    // 把成员分类到团队岗位类型
    @PutMapping("/members/{employeeId}/team-position")
    public Map<String, Object> assignTeamPosition(HttpServletRequest request, @PathVariable Long employeeId,
                                                  @RequestBody Map<String, String> body) {
        Long teamPositionId = null;
        String idStr = body.get("teamPositionId");
        if (idStr != null && !idStr.isBlank()) {
            teamPositionId = Long.parseLong(idStr.trim());
        }
        return teamService.assignTeamPosition(parseUid(request), TenantContext.getTenantId(),
                employeeId, teamPositionId);
    }

    // 把成员分配/调整到指定团队（前提：员工已在本公司）
    @PutMapping("/members/{employeeId}/team")
    public Map<String, Object> assignTeam(HttpServletRequest request, @PathVariable Long employeeId,
                                          @RequestBody Map<String, String> body) {
        Long teamId = null;
        String teamIdStr = body.get("teamId");
        if (teamIdStr != null && !teamIdStr.isBlank()) {
            teamId = Long.parseLong(teamIdStr.trim());
        }
        return teamService.assignTeam(parseUid(request), TenantContext.getTenantId(), employeeId, teamId);
    }

    // 踢出团队：不开除，回落默认团队
    @DeleteMapping("/members/{employeeId}/team")
    public Map<String, Object> removeFromTeam(HttpServletRequest request, @PathVariable Long employeeId) {
        return teamService.removeFromTeam(parseUid(request), TenantContext.getTenantId(), employeeId);
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
