import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  ReactNode,
} from 'react';
import { router } from 'expo-router';
import { authService } from '../services';
import { storage, authEvents } from '../utils/storage';
import { LoginRequest, SignUpRequest } from '../types/api';

interface User {
  email: string;
  nickname: string;
  timezone: string;
  userId: number | null;
  role: string | null;
}

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  timezone: string | null;
  login: (data: LoginRequest) => Promise<void>;
  signUp: (data: SignUpRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // 앱 시작 시 저장된 토큰 확인
  useEffect(() => {
    checkAuthStatus();
  }, []);

  // Listen for token expiry events (from API interceptor)
  useEffect(() => {
    const unsubscribe = authEvents.subscribe(() => {
      setUser(null);
      router.replace('/(auth)/login');
    });
    return unsubscribe;
  }, []);

  const checkAuthStatus = async () => {
    try {
      const accessToken = await storage.getAccessToken();
      const refreshToken = await storage.getRefreshToken();

      // accessToken과 refreshToken 둘 다 있어야 로그인 상태로 인정
      if (!accessToken || !refreshToken) {
        // 토큰이 불완전하면 모두 정리
        if (accessToken || refreshToken) {
          console.log('Incomplete tokens found, clearing...');
          await storage.clearTokens();
        }
        setIsLoading(false);
        return;
      }

      const savedEmail = await storage.getEmail();
      const savedTimezone = await storage.getTimezone();
      const savedNickname = await storage.getNickname();
      const savedUserId = await storage.getUserId();
      const savedRole = await storage.getRole();

      setUser({
        email: savedEmail || '',
        nickname: savedNickname || '',
        timezone: savedTimezone || 'UTC',
        userId: savedUserId,
        role: savedRole,
      });
    } catch (error) {
      console.error('Auth check failed:', error);
      // 에러 발생 시 토큰 정리
      await storage.clearTokens();
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (data: LoginRequest) => {
    const response = await authService.login(data);
    if (response.data) {
      const timezone = response.data.timezone || 'UTC';
      const nickname = response.data.nickname || '';
      const userId = response.data.userId ?? null;
      const role = response.data.role || 'USER';
      await storage.setEmail(data.email);
      await storage.setTimezone(timezone);
      await storage.setNickname(nickname);
      if (userId !== null) {
        await storage.setUserId(userId);
      }
      await storage.setRole(role);
      setUser({ email: data.email, nickname, timezone, userId, role });
    }
  };

  const signUp = async (data: SignUpRequest) => {
    await authService.signUp(data);
    // 회원가입 후 자동 로그인
    await login({ email: data.email, password: data.password });
  };

  const logout = async () => {
    await authService.logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isLoading,
        isAuthenticated: !!user,
        timezone: user?.timezone || null,
        login,
        signUp,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
