import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './app/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // 코랄 브랜드 팔레트
        brand: {
          50:  '#FFF3EC',
          100: '#FFE9DD',
          200: '#FFD0B5',
          300: '#FFB48A',
          400: '#FBA062',  // light accent
          500: '#F9A176',  // primary CTA
          600: '#F08C5A',  // hover
          700: '#D6723E',  // pressed / deep
        },
        // 배경 크림 팔레트
        cream: {
          50:  '#FFFCFA',
          100: '#FFF8F5',  // base background
          200: '#FFF0E8',
          300: '#FFE6D8',
        },
        // 브라운 잉크 팔레트
        ink: {
          50:  '#F5EFED',
          100: '#E8DDD9',
          200: '#D4C4BE',
          300: '#BEA89F',
          400: '#A08070',  // muted
          500: '#8D6E63',  // secondary text
          600: '#6B4F45',
          700: '#4E342E',
          800: '#3E2723',  // primary text
          900: '#2C1A16',
        },
      },
    },
  },
  plugins: [],
};

export default config;
