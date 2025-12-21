// API 공통 응답 타입
export interface ApiResponse<T> {
  status: 'SUCCESS' | 'ERROR';
  message?: string;
  data?: T;
}

// 인증 관련 타입
export interface SignUpRequest {
  email: string;
  password: string;
  nickname: string;
  timezone?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface TokenRefreshRequest {
  refreshToken: string;
}

// Article 관련 타입 (백엔드 원본)
export interface Article {
  uuid: string;
  title: string;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface AddArticleRequest {
  title: string;
  content: string;
}

export interface UpdateArticleRequest {
  title?: string;
  content?: string;
}

// D-Day 관련 타입 (프론트엔드용)
export interface DDay {
  uuid: string;
  title: string;
  targetDate: string; // ISO date string (YYYY-MM-DD)
  createdAt: string;
  updatedAt: string;
}

export interface AddDDayRequest {
  title: string;
  targetDate: string;
}

export interface UpdateDDayRequest {
  title?: string;
  targetDate?: string;
}
