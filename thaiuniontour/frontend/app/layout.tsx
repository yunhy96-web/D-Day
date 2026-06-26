import type { Metadata } from 'next';
import './globals.css';
import Header from '@/components/Header';

export const metadata: Metadata = {
  title: 'ThaiUnionTour | 태국 여행상품',
  description: '태국 투어·패키지·입장권 전문 예약 — ThaiUnionTour',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body className="min-h-screen bg-cream-100 text-ink-800 antialiased">
        <Header />
        <main>{children}</main>
        <footer className="mt-16 border-t border-cream-300 bg-white py-8 text-center text-xs text-ink-400">
          © 2026 ThaiUnionTour. All rights reserved.
        </footer>
      </body>
    </html>
  );
}
