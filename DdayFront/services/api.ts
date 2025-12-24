import axios, { AxiosError, InternalAxiosRequestConfig } from 'axios';
import { config } from '../config';
import { storage } from '../utils/storage';
import { ApiResponse, TokenResponse } from '../types/api';

// Axios 인스턴스 생성
export const api = axios.create({
  baseURL: config.apiUrl,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 토큰 갱신 중인지 체크
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: Error) => void;
}> = [];

const processQueue = (error: Error | null, token: string | null = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else {
      promise.resolve(token!);
    }
  });
  failedQueue = [];
};

// Request Interceptor - 토큰 자동 첨부
api.interceptors.request.use(
  async (config: InternalAxiosRequestConfig) => {
    // 로그인, 회원가입, 토큰 갱신은 토큰 불필요
    const publicEndpoints = ['/api/auth/login', '/api/auth/signup', '/api/auth/refresh'];
    const isPublicEndpoint = publicEndpoints.some((endpoint) =>
      config.url?.includes(endpoint)
    );

    if (!isPublicEndpoint) {
      const token = await storage.getAccessToken();
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }

    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor - 토큰 만료 시 자동 갱신
api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
    };

    // 401 or 403 에러이고 재시도하지 않은 경우 (expired token)
    const isAuthError = error.response?.status === 401 || error.response?.status === 403;
    if (isAuthError && !originalRequest._retry) {
      // 토큰 갱신 요청 자체가 실패한 경우 로그아웃
      if (originalRequest.url?.includes('/api/auth/refresh')) {
        await storage.clearTokens(true); // emit event to redirect to login
        return Promise.reject(error);
      }

      if (isRefreshing) {
        // 이미 갱신 중이면 큐에 추가
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`;
              resolve(api(originalRequest));
            },
            reject: (err: Error) => {
              reject(err);
            },
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = await storage.getRefreshToken();
        if (!refreshToken) {
          // refresh token이 없으면 조용히 로그인 화면으로 리다이렉트
          console.log('No refresh token found, redirecting to login...');
          processQueue(new Error('No refresh token'), null);
          await storage.clearTokens(true); // emit event to redirect to login
          isRefreshing = false;
          return Promise.reject(new Error('Session expired'));
        }

        const response = await api.post<ApiResponse<TokenResponse>>(
          config.endpoints.auth.refresh,
          { refreshToken }
        );

        const { accessToken, refreshToken: newRefreshToken } = response.data.data!;
        await storage.saveTokens(accessToken, newRefreshToken);

        processQueue(null, accessToken);

        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError as Error, null);
        await storage.clearTokens(true); // emit event to redirect to login
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
