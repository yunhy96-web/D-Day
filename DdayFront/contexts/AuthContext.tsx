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
}

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
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
      const token = await storage.getAccessToken();
      if (token) {
        // TODO: 서버에서 유저 정보 가져오기 API가 있다면 호출
        // 지금은 토큰이 있으면 로그인된 것으로 간주
        setUser({ email: '' });
      }
    } catch (error) {
      console.error('Auth check failed:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (data: LoginRequest) => {
    const response = await authService.login(data);
    if (response.data) {
      setUser({ email: data.email });
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
