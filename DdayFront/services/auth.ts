import { api } from './api';
import { config } from '../config';
import { storage } from '../utils/storage';
import {
  ApiResponse,
  SignUpRequest,
  LoginRequest,
  TokenResponse,
} from '../types/api';

export const authService = {
  // 회원가입
  async signUp(data: SignUpRequest): Promise<ApiResponse<number>> {
    const response = await api.post<ApiResponse<number>>(
      config.endpoints.auth.signup,
      data
    );
    return response.data;
  },

  // 로그인
  async login(data: LoginRequest): Promise<ApiResponse<TokenResponse>> {
    const response = await api.post<ApiResponse<TokenResponse>>(
      config.endpoints.auth.login,
      data
    );

    // 로그인 성공 시 토큰 저장
    if (response.data.data) {
      const { accessToken, refreshToken } = response.data.data;
      await storage.saveTokens(accessToken, refreshToken);
    }

    return response.data;
  },

  // 로그아웃
  async logout(): Promise<void> {
    try {
      await api.post(config.endpoints.auth.logout);
    } catch (error) {
      // 서버 로그아웃 실패해도 로컬 토큰은 삭제
      console.error('Logout error:', error);
    } finally {
      await storage.clearTokens();
    }
  },

  // 로그인 상태 확인
  async isLoggedIn(): Promise<boolean> {
    const token = await storage.getAccessToken();
    return !!token;
  },
};
