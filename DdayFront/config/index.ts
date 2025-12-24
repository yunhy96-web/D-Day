// API 설정
// 환경변수 또는 기본값 사용
const API_URL = process.env.EXPO_PUBLIC_API_URL || 'https://api.union-selim.com';

export const config = {
  apiUrl: API_URL,
  // API 엔드포인트
  endpoints: {
    auth: {
      signup: '/api/auth/signup',
      login: '/api/auth/login',
      refresh: '/api/auth/refresh',
      logout: '/api/auth/logout',
    },
    articles: '/api/articles',
    codes: {
      articleTypes: '/api/codes/article-types',
      articleTopics: '/api/codes/article-topics',
    },
  },
};
