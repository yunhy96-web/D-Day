// API 공통 응답 타입
export interface ApiResponse<T> {
  success: boolean;
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
  tokenType: string;
  expiresIn: number;
  timezone: string;
  nickname?: string;
  userId?: number;
  role?: string;
}

export interface TokenRefreshRequest {
  refreshToken: string;
}

// 공통코드 타입
export interface CommonCode {
  code: string;
  label: string;
  labelKo: string;
  labelEn: string;
  labelTh: string;
}

// Article 관련 타입
export interface Article {
  uuid: string;
  topic: string | null;
  articleType: string;
  title: string;
  content: string;
  originalLang: string | null;
  titleKo: string | null;
  contentKo: string | null;
  titleTh: string | null;
  contentTh: string | null;
  translationStatus: string;
  authorNickname: string;
  createdBy: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface AddArticleRequest {
  topic: string;
  articleType?: string;
  title: string;
  content: string;
}

export interface UpdateArticleRequest {
  topic?: string;
  articleType?: string;
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
