import type { PageQuery } from '../types'

/** el-tag 支持的语义色 */
export type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

/** 线索状态；与后端 LeadService.ALLOWED_TRANSITIONS 的状态集合保持一致 */
export const LEAD_STATUS_OPTIONS: { value: string; label: string; tag: TagType }[] = [
  { value: 'NEW', label: '待分配', tag: 'info' },
  { value: 'ASSIGNED', label: '已分配', tag: 'primary' },
  { value: 'CONTACTED', label: '已联系', tag: 'warning' },
  { value: 'QUALIFIED', label: '已确认需求', tag: 'success' },
  { value: 'CONVERTED', label: '已转化', tag: 'success' },
  { value: 'INVALID', label: '无效', tag: 'danger' }
]

/**
 * 允许的下一步状态；与后端 ALLOWED_TRANSITIONS 一致。
 * CONVERTED 不在其中：转商机必须走 /convert 接口。
 */
export const LEAD_NEXT_STATUS: Record<string, string[]> = {
  NEW: ['ASSIGNED', 'INVALID'],
  ASSIGNED: ['CONTACTED', 'INVALID'],
  CONTACTED: ['QUALIFIED', 'INVALID'],
  QUALIFIED: ['INVALID'],
  CONVERTED: [],
  INVALID: []
}

/** 跟进方式；与后端 ActivityService.ACTIVITY_TYPES 白名单保持一致 */
export const FOLLOW_TYPE_OPTIONS = [
  { value: 'CALL', label: '电话' },
  { value: 'EMAIL', label: '邮件' },
  { value: 'WHATSAPP', label: 'WhatsApp' },
  { value: 'LINKEDIN', label: 'LinkedIn' },
  { value: 'MEETING', label: '会议' },
  { value: 'NOTE', label: '备注' }
]

/** 线索来源 */
export const SOURCE_TYPE_OPTIONS = [
  { value: 'MANUAL', label: '手动录入' },
  { value: 'IMPORT', label: '批量导入' },
  { value: 'CRAWLER', label: '自动采集' }
]

/** 列表项（后端 LeadVO） */
export interface LeadVO {
  id: number
  title: string
  status: string
  poolType: string
  score?: number
  grade?: string
  leadCompanyId: number
  primaryContactId?: number
  ownerUserId?: number
  ownerTeamId?: number
  ownerName?: string
  ownerTeamName?: string
  protectedUntil?: string
  convertedAt?: string
  convertedOpportunityId?: number
  remark?: string
  createdAt?: string
  updatedAt?: string
  companyName?: string
  countryCode?: string
  contactName?: string
  contactEmail?: string
  lastFollowAt?: string
  nextFollowAt?: string
}

export interface LeadContactVO {
  id: number
  name?: string
  jobTitle?: string
  department?: string
  seniority?: string
  email?: string
  emailStatus?: string
  phone?: string
  whatsapp?: string
  linkedinUrl?: string
  isKp?: number
}

export interface LeadActivityVO {
  id: number
  activityType: string
  content?: string
  result?: string
  ownerUserId?: number
  ownerName?: string
  activityAt?: string
  nextFollowAt?: string
  createdAt?: string
}

export interface LeadAssignLogVO {
  id: number
  action: string
  fromOwnerUserId?: number
  fromOwnerName?: string
  toOwnerUserId?: number
  toOwnerName?: string
  reason?: string
  operatorId?: number
  createdAt?: string
}

/** 详情（后端 LeadDetailVO） */
export interface LeadDetailVO extends LeadVO {
  website?: string
  industryName?: string
  address?: string
  sourceType?: string
  sourceUrl?: string
  contacts?: LeadContactVO[]
  activities?: LeadActivityVO[]
  assignLogs?: LeadAssignLogVO[]
}

export interface LeadQuery extends PageQuery {
  status?: string
  /** PUBLIC / PRIVATE */
  poolType?: string
  ownerUserId?: number
}

export interface LeadCreatePayload {
  title?: string
  sourceType?: string
  sourceUrl?: string
  remark?: string
  company: {
    name: string
    countryCode?: string
    regionCode?: string
    website?: string
    industryCode?: string
    industryName?: string
    address?: string
  }
  contact?: {
    name?: string
    jobTitle?: string
    department?: string
    seniority?: string
    email?: string
    phone?: string
    whatsapp?: string
    linkedinUrl?: string
    countryCode?: string
    language?: string
    isKp?: boolean
  }
}

/** 可分配的销售专员（后端 /leads/assignable-users） */
export interface AssignableUser {
  id: number
  name?: string
  phone?: string
  teamId?: number
  teamName?: string
  jobCode?: string
}

export interface ActivityCreatePayload {
  bizType?: string
  bizId: number
  activityType: string
  content?: string
  result?: string
  activityAt?: string
  nextFollowAt?: string
}

export interface LeadConvertPayload {
  opportunityName?: string
  amount?: number
  currency?: string
  expectedCloseDate?: string
}

export interface MemberRankingItem {
  userId: number
  userName?: string
  teamName?: string
  count: number
}

export interface ExpiringLeadItem {
  leadId: number
  title: string
  ownerUserId?: number
  ownerName?: string
  protectedUntil: string
}

/** 看板指标（后端 DashboardService 返回结构） */
export interface DashboardStats {
  totalCount: number
  poolCount: number
  privateCount: number
  statusCounts: Record<string, number>
  todayNewCount: number
  todayFollowCount: number
  memberRanking?: MemberRankingItem[]
  expiringSoon?: ExpiringLeadItem[]
}