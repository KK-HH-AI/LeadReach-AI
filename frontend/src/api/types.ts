/**
 * 统一响应结构，与后端网关约定一致：{ code, message, data, traceId }
 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  traceId: string
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
