import { http } from '../request'

/** 团队成员（后端 TeamController.list 返回结构） */
export interface TeamMember {
  employeeId: number
  phone: string
  position: string
  /** 所属团队ID，空 = 未分团队 */
  teamId?: number
  teamName?: string
  createdAt: string
}

/** 团队概况（后端 TeamController.create 返回结构） */
export interface TeamResult {
  teamId: number
  bossId: number
  memberCount: number
}

/** 岗位选项：与后端 POSITIONS 白名单保持一致 */
export const POSITION_OPTIONS = [
  { value: 'SALES_REP', label: '销售专员' },
  { value: 'SALES_MANAGER', label: '销售主管' },
  { value: 'OPERATOR', label: '运营' }
]

export const teamApi = {
  /** 创建/确认团队（幂等） */
  create: () => http.post<TeamResult>('/v1/teams'),
  /** 团队成员列表（尾斜杠：网关 catch-all 与后端双路径都兼容） */
  list: () => http.get<TeamMember[]>('/v1/teams/'),
  /** 拉人入队：员工必须先注册 */
  addMember: (payload: { phone: string; position: string }) =>
    http.post<null>('/v1/teams/members', payload),
  /** 删除人：移出团队并解除公司归属 */
  removeMember: (employeeId: number) =>
    http.delete<null>(`/v1/teams/members/${employeeId}`),
  /** 分配/修改员工负责的内容 */
  assign: (employeeId: number, payload: { contentType: string; contentId: string }) =>
    http.put<null>(`/v1/teams/members/${employeeId}/assignment`, payload)
}
