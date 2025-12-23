import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';
const TIMEZONE_KEY = 'timezone';
const NICKNAME_KEY = 'nickname';
const USER_ID_KEY = 'userId';
const ROLE_KEY = 'role';
const EMAIL_KEY = 'email';

// 웹에서는 SecureStore 대신 localStorage 사용
const isWeb = Platform.OS === 'web';

// Auth event listeners for token expiry
type AuthEventListener = () => void;
const authEventListeners: Set<AuthEventListener> = new Set();

export const authEvents = {
  subscribe(listener: AuthEventListener): () => void {
    authEventListeners.add(listener);
    return () => {
      authEventListeners.delete(listener);
    };
  },
  emit() {
    authEventListeners.forEach((listener) => listener());
  },
};

export const storage = {
  // Access Token
  async getAccessToken(): Promise<string | null> {
    if (isWeb) {
      return localStorage.getItem(ACCESS_TOKEN_KEY);
    }
    return await SecureStore.getItemAsync(ACCESS_TOKEN_KEY);
  },

  async setAccessToken(token: string): Promise<void> {
    if (isWeb) {
      localStorage.setItem(ACCESS_TOKEN_KEY, token);
      return;
    }
    await SecureStore.setItemAsync(ACCESS_TOKEN_KEY, token);
  },

  async removeAccessToken(): Promise<void> {
    if (isWeb) {
      localStorage.removeItem(ACCESS_TOKEN_KEY);
      return;
    }
    await SecureStore.deleteItemAsync(ACCESS_TOKEN_KEY);
  },

  // Refresh Token
  async getRefreshToken(): Promise<string | null> {
    if (isWeb) {
      return localStorage.getItem(REFRESH_TOKEN_KEY);
    }
    return await SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
  },

  async setRefreshToken(token: string): Promise<void> {
    if (isWeb) {
      localStorage.setItem(REFRESH_TOKEN_KEY, token);
      return;
    }
    await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token);
  },

  async removeRefreshToken(): Promise<void> {
    if (isWeb) {
      localStorage.removeItem(REFRESH_TOKEN_KEY);
      return;
    }
    await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
  },

  // 모든 토큰 삭제 (로그아웃 시 사용)
  // emitEvent: true for forced logout (token expiry), false for intentional logout
  async clearTokens(emitEvent = false): Promise<void> {
    await this.removeAccessToken();
    await this.removeRefreshToken();
    if (emitEvent) {
      authEvents.emit();
    }
  },

  // 토큰 저장 (로그인 성공 시 사용)
  async saveTokens(accessToken: string, refreshToken: string): Promise<void> {
    await this.setAccessToken(accessToken);
    await this.setRefreshToken(refreshToken);
  },

  // Timezone
  async getTimezone(): Promise<string | null> {
    if (isWeb) {
      return localStorage.getItem(TIMEZONE_KEY);
    }
    return await SecureStore.getItemAsync(TIMEZONE_KEY);
  },

  async setTimezone(timezone: string): Promise<void> {
    if (isWeb) {
      localStorage.setItem(TIMEZONE_KEY, timezone);
      return;
    }
    await SecureStore.setItemAsync(TIMEZONE_KEY, timezone);
  },

  // Nickname
  async getNickname(): Promise<string | null> {
    if (isWeb) {
      return localStorage.getItem(NICKNAME_KEY);
    }
    return await SecureStore.getItemAsync(NICKNAME_KEY);
  },

  async setNickname(nickname: string): Promise<void> {
    if (isWeb) {
      localStorage.setItem(NICKNAME_KEY, nickname);
      return;
    }
    await SecureStore.setItemAsync(NICKNAME_KEY, nickname);
  },

  // User ID
  async getUserId(): Promise<number | null> {
    if (isWeb) {
      const value = localStorage.getItem(USER_ID_KEY);
      return value ? parseInt(value, 10) : null;
    }
    const value = await SecureStore.getItemAsync(USER_ID_KEY);
    return value ? parseInt(value, 10) : null;
  },

  async setUserId(userId: number): Promise<void> {
    if (isWeb) {
      localStorage.setItem(USER_ID_KEY, userId.toString());
      return;
    }
    await SecureStore.setItemAsync(USER_ID_KEY, userId.toString());
  },

  // Role
  async getRole(): Promise<string | null> {
    if (isWeb) {
      return localStorage.getItem(ROLE_KEY);
    }
    return await SecureStore.getItemAsync(ROLE_KEY);
  },

  async setRole(role: string): Promise<void> {
    if (isWeb) {
      localStorage.setItem(ROLE_KEY, role);
      return;
    }
    await SecureStore.setItemAsync(ROLE_KEY, role);
  },

  // Email
  async getEmail(): Promise<string | null> {
    if (isWeb) {
      return localStorage.getItem(EMAIL_KEY);
    }
    return await SecureStore.getItemAsync(EMAIL_KEY);
  },

  async setEmail(email: string): Promise<void> {
    if (isWeb) {
      localStorage.setItem(EMAIL_KEY, email);
      return;
    }
    await SecureStore.setItemAsync(EMAIL_KEY, email);
  },
};
