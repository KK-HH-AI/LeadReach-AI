import { http } from '../request'
import type { DashboardStats } from './types'

export const dashboardApi = {
  /** 我的线索概览 */
  my: () => http.get<DashboardStats>('/v1/crm/dashboard/my'),
  /** 团队看板：老板看全公司，销售主管看本团队 */
  team: () => http.get<DashboardStats>('/v1/crm/dashboard/team'),
  /** 公司总览：仅老板 */
  overview: () => http.get<DashboardStats>('/v1/crm/dashboard/overview')
}