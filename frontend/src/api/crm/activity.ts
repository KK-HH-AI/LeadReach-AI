import { http } from '../request'
import type { ActivityCreatePayload, LeadActivityVO } from './types'

export const activityApi = {
  /** 记录一次跟进（首次跟进后端会把线索从 ASSIGNED 推进到 CONTACTED） */
  create: (payload: ActivityCreatePayload) => http.post<number>('/v1/crm/activities', payload),
  /** 查询某条线索的跟进记录 */
  list: (bizId: number, bizType = 'LEAD') =>
    http.get<LeadActivityVO[]>('/v1/crm/activities', { bizId, bizType })
}