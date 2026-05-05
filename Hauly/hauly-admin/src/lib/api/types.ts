export interface UserInfo {
  id: number
  username: string
  role: 'INTAKE' | 'BUYER' | 'ADMIN' | 'VIEWER'
  displayName: string | null
  preferredLanguage: 'ko' | 'en' | 'th' | null
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  user: UserInfo
}

/** Shape returned by GET /api/auth/me */
export type MeResponse = UserInfo

export interface ApiErrorBody {
  error: {
    code: string
    message: string
  }
}

export class ApiError extends Error {
  code: string
  status: number

  constructor(code: string, message: string, status: number) {
    super(message)
    this.code = code
    this.status = status
    this.name = 'ApiError'
  }
}
