/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  output: 'standalone', // Docker(라즈베리파이) 배포용 경량 빌드
  async rewrites() {
    // 로컬 개발: /api/* → 백엔드 api(8080). 배포 시 nginx가 라우팅.
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.API_PROXY_TARGET || 'http://localhost:8080'}/api/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
