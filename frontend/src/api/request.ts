import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'
import { useTenantStore } from '@/stores/tenant'
import type { ApiResponse } from './types'

/** 生成 trace_id，与后端 OTel 链路对齐 */
export function genTraceId(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

/** 约定：业务码 0 表示成功 */
const SUCCESS_CODE = 0

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 15000
})

// 请求拦截：注入 JWT / 租户 / traceId / 语言
service.interceptors.request.use((config) => {
  const userStore = useUserStore()
  const tenantStore = useTenantStore()

  const token = userStore.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (tenantStore.currentTenantId) {
    config.headers['X-Tenant-Id'] = tenantStore.currentTenantId
  }
  config.headers['X-Trace-Id'] = genTraceId()
  config.headers['Accept-Language'] = userStore.language || 'zh-CN'
  return config
})

// 响应拦截：统一处理 code / HTTP 状态
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    // 文件/二进制流直接返回
    if (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer') {
      return response as unknown as Promise<any>
    }
    if (res.code === SUCCESS_CODE) {
      return res.data as unknown as AxiosResponse
    }
    // 业务错误：真实后端返回 msg，mock 返回 message，两者都兼容
    const message = res.msg || res.message || res.traceId || '请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  async (error) => {
    const { response, config } = error
    const userStore = useUserStore()
    if (!response) {
      ElMessage.error(error.traceId || '网络异常')
      return Promise.reject(error)
    }
    const status: number = response.status
    if (status === 401) {
      // 尝试刷新一次；失败则登出跳登录
      const refreshed = await userStore.tryRefresh()
      if (refreshed && config) {
        return service(config)
      }
      userStore.logout()
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      ElMessage.error('登录已过期，请重新登录')
    } else if (status === 403) {
      router.push('/403')
    } else if (status === 429) {
      ElMessage.error('请求过于频繁，请稍后再试')
    } else if (status >= 500) {
      ElMessage.error(`服务异常 (${response.data?.traceId ?? ''})`)
    } else {
      ElMessage.error(response.data?.message || `请求失败 (${status})`)
    }
    return Promise.reject(error)
  }
)

/** 供 mock 挂载拦截的原始 axios 实例 */
export { service as axiosInstance }

/** 泛型封装：直接返回 data 段（响应拦截器已解包） */
export async function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  const res = (await service.request(config)) as T
  return res
}

export const http = {
  get: <T = unknown>(url: string, params?: object, config?: AxiosRequestConfig) =>
    request<T>({ method: 'GET', url, params, ...config }),
  post: <T = unknown>(url: string, data?: object, config?: AxiosRequestConfig) =>
    request<T>({ method: 'POST', url, data, ...config }),
  put: <T = unknown>(url: string, data?: object, config?: AxiosRequestConfig) =>
    request<T>({ method: 'PUT', url, data, ...config }),
  patch: <T = unknown>(url: string, data?: object, config?: AxiosRequestConfig) =>
    request<T>({ method: 'PATCH', url, data, ...config }),
  delete: <T = unknown>(url: string, config?: AxiosRequestConfig) =>
    request<T>({ method: 'DELETE', url, ...config })
}
