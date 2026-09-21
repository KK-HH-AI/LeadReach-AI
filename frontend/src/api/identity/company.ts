import { http } from '../request'

/** 创建公司返回（后端 CompanyController.create） */
export interface CompanyResult {
  companyId: number
  name: string
}

export const companyApi = {
  /** 老板创建公司：创建后需重新登录，新 token 才带租户上下文 */
  create: (name: string) => http.post<CompanyResult>('/v1/companies', { name })
}
