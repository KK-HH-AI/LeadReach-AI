/**
 * 统一响应结构，与后端约定一致：{ code, msg, data }
 * mock 层历史上用的是 message，故两个字段都声明为可选
 */
export interface ApiResponse<T = unknown> {
  code: number
  /** 后端（user-service）统一用 msg */
  msg?: string
  /** mock 层沿用 message */
  message?: string
  data: T
  traceId?: string
}

export interface PageResult<T = unknown> {
  list: T[]
  total: number
  page: number
  size: number
}

export interface PageQuery {
  page?: number
  size?: number
  keyword?: string
}
