import Link from 'next/link';
import { getCategoryTree } from '@/lib/api';
import MegaMenu from './MegaMenu';
import SearchBar from './SearchBar';

export default async function Header() {
  const categories = await getCategoryTree('THEME');

  return (
    <header className="sticky top-0 z-40 border-b border-gray-200 bg-white shadow-sm">
      <div className="mx-auto flex max-w-7xl items-center gap-4 px-4 py-3">
        {/* 로고 */}
        <Link href="/" className="flex-shrink-0">
          <span className="text-xl font-bold text-blue-600">ThaiUnionTour</span>
          <span className="ml-1 text-xs text-gray-500 hidden sm:inline">태국여행</span>
        </Link>

        {/* 메가 메뉴 */}
        <MegaMenu categories={categories} />

        {/* 검색바 */}
        <div className="flex-1 max-w-sm mx-auto">
          <SearchBar />
        </div>

        {/* 우측 액션 */}
        <div className="flex items-center gap-3 flex-shrink-0">
          <span className="text-xs text-gray-500 hidden sm:inline">ko</span>
          <Link
            href="/login"
            className="rounded-full border border-gray-300 px-3 py-1.5 text-xs font-medium text-gray-700 hover:border-blue-500 hover:text-blue-600 transition-colors hidden sm:block"
          >
            로그인
          </Link>
        </div>
      </div>
    </header>
  );
}
