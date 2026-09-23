package com.leadreach.crm.client;

import com.leadreach.crm.client.dto.TeamDTO;
import com.leadreach.crm.client.dto.UserDTO;
import com.leadreach.crm.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * user-service 内部接口客户端
 *
 * 身份头由 FeignConfig 统一注入（X-Internal-Call / X-Tenant-Id），
 * 这里只声明接口，不关心鉴权细节。
 */
@FeignClient(name = "user-service", path = "/internal")
public interface UserClient {

    /** 查单个用户：分配线索时校验目标用户 */
    @GetMapping("/users/{id}")
    R<UserDTO> getUser(@PathVariable("id") Long id);

    /** 批量查用户：列表页补负责人展示名 */
    @GetMapping("/users")
    R<List<UserDTO>> listUsers(@RequestParam("ids") String ids);

    /** 查团队下所有用户（含负责人）：团队看板 */
    @GetMapping("/users/by-team")
    R<List<UserDTO>> listUsersByTeam(@RequestParam("teamId") Long teamId);

    /** 可分配的销售专员：分配线索的下拉数据源，范围由 user-service 按岗位裁好 */
    @GetMapping("/users/assignable")
    R<List<UserDTO>> listAssignableUsers();

    /** 查团队 */
    @GetMapping("/teams/{id}")
    R<TeamDTO> getTeam(@PathVariable("id") Long id);

    /** 查团队成员列表 */
    @GetMapping("/teams/{id}/members")
    R<List<UserDTO>> listTeamMembers(@PathVariable("id") Long id);
}