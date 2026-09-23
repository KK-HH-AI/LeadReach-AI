import { http } from '../request'
import type { PageResult } from '../types'
import type {
  AssignableUser,
  LeadConvertPayload,
  LeadCreatePayload,
  LeadDetailVO,
  LeadQuery,
  LeadVO
} from './types'

export const leadApi = {
  /** 创建线索（后端自动按公司名/域名去重复用公司） */
  create: (payload: LeadCreatePayload) => http.post<number>('/v1/crm/leads', payload),
  /** 公共线索池 */
  pool: (params?: LeadQuery) => http.get<PageResult<LeadVO>>('/v1/crm/leads/pool', params),
  /** 我的线索 */
  my: (params?: LeadQuery) => http.get<PageResult<LeadVO>>('/v1/crm/leads/my', params),
  /** 线索列表（后端按岗位叠加数据范围） */
  list: (params?: LeadQuery) => http.get<PageResult<LeadVO>>('/v1/crm/leads', params),
  /** 线索详情 */
  detail: (id: number) => http.get<LeadDetailVO>(`/v1/crm/leads/${id}`),
  /** 可分配的销售专员：分配弹窗下拉，范围由后端按岗位裁好 */
  assignableUsers: () => http.get<AssignableUser[]>('/v1/crm/leads/assignable-users'),
  /** 分配：老板可全公司分，主管只能分给本团队销售 */
  assign: (id: number, payload: { toUserId: number; reason?: string }) =>
    http.put<null>(`/v1/crm/leads/${id}/assign`, payload),
  /** 领取：仅销售专员，后端条件更新防并发 */
  claim: (id: number) => http.put<null>(`/v1/crm/leads/${id}/claim`),
  /** 更新状态（转商机请用 convert） */
  updateStatus: (id: number, payload: { status: string; remark?: string }) =>
    http.put<null>(`/v1/crm/leads/${id}/status`, payload),
  /** 退回公海 */
  recycle: (id: number, reason?: string) =>
    http.put<null>(`/v1/crm/leads/${id}/recycle`, { reason }),
  /** 转商机 */
  convert: (id: number, payload?: LeadConvertPayload) =>
    http.post<{ leadId: number; opportunityId?: number; message?: string }>(
      `/v1/crm/leads/${id}/convert`,
      payload
    )
}