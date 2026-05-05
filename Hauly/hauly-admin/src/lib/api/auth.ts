import { apiClient } from './client'
import type { LoginResponse, MeResponse } from './types'

export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await apiClient.post<LoginResponse>('/auth/login', { username, password })
  return data
}

export async function fetchMe(): Promise<MeResponse> {
  const { data } = await apiClient.get<MeResponse>('/auth/me')
  return data
}

export async function refresh(): Promise<void> {
  await apiClient.post('/auth/refresh')
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout')
}

export async function changePassword(input: {
  currentPassword: string
  newPassword: string
}): Promise<void> {
  await apiClient.post('/auth/password', input)
}

export async function updateLanguagePreference(
  language: 'ko' | 'en' | 'th'
): Promise<MeResponse> {
  const { data } = await apiClient.patch<MeResponse>('/auth/me/language', { language })
  return data
}
