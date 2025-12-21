import { Redirect } from 'expo-router';
import { useAuth } from '@/contexts';
import { Loading } from '@/components';

export default function Index() {
  const { isAuthenticated, isLoading } = useAuth();

  // 인증 상태 확인 중
  if (isLoading) {
    return <Loading fullScreen text="로딩 중..." />;
  }

  // 로그인 상태에 따라 리다이렉트
  if (isAuthenticated) {
    return <Redirect href="/(tabs)" />;
  }

  return <Redirect href="/(auth)/login" />;
}
