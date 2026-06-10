import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'thaiuniontour | 태국 여행상품',
  description: '태국 여행상품(투어·패키지) 예약 — thaiuniontour',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      {/* M1에서 Header(MegaMenu+검색바)·Footer·라벨 Provider 추가 (05-frontend.md) */}
      <body>{children}</body>
    </html>
  );
}
