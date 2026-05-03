import { apiClient } from './client'

export interface CommonCodeView {
  groupCode: string
  code: string
  name: string
  sortOrder: number
  attributes: Record<string, unknown> | null
  active: boolean
}

export async function fetchCommonCodes(groupCode: string): Promise<CommonCodeView[]> {
  const { data } = await apiClient.get<CommonCodeView[]>(`/intake/common-codes/${groupCode}`)
  return data
}
