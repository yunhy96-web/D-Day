export const dynamic = 'force-dynamic';

import { getProducts } from '@/lib/api';
import ProductCard from '@/components/ProductCard';
import SearchBar from '@/components/SearchBar';

interface SearchPageProps {
  searchParams: {
    q?: string;
    sort?: string;
    page?: string;
  };
}

export default async function SearchPage({ searchParams }: SearchPageProps) {
  const q = searchParams.q ?? '';
  const sort = (searchParams.sort as 'latest' | 'priceAsc' | 'priceDesc') || 'latest';
  const page = Number(searchParams.page ?? 0);

  const { items, page: pageMeta } = await getProducts({ q, sort, page, size: 20 });

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      {/* 검색바 */}
      <div className="mb-6 max-w-lg">
        <SearchBar defaultValue={q} />
      </div>

      {/* 헤더 정보 */}
      <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
        <div>
          {q ? (
            <h1 className="text-xl font-bold text-gray-800">
              <span className="text-blue-600">&ldquo;{q}&rdquo;</span> 검색 결과
              <span className="ml-2 text-base font-normal text-gray-500">
                {pageMeta.totalElements.toLocaleString('ko-KR')}건
              </span>
            </h1>
          ) : (
            <h1 className="text-xl font-bold text-gray-800">
              전체 상품
              <span className="ml-2 text-base font-normal text-gray-500">
                {pageMeta.totalElements.toLocaleString('ko-KR')}건
              </span>
            </h1>
          )}
        </div>

        {/* 정렬 */}
        <div className="flex gap-2">
          {(['latest', 'priceAsc', 'priceDesc'] as const).map((s) => {
            const label = s === 'latest' ? '최신순' : s === 'priceAsc' ? '낮은가격' : '높은가격';
            const isActive = sort === s;
            const href = `?${new URLSearchParams({ ...(q ? { q } : {}), sort: s }).toString()}`;
            return (
              <a
                key={s}
                href={href}
                className={`rounded-full border px-3 py-1 text-xs font-medium transition-colors ${
                  isActive
                    ? 'border-blue-500 bg-blue-500 text-white'
                    : 'border-gray-300 text-gray-600 hover:border-blue-400 hover:text-blue-600'
                }`}
              >
                {label}
              </a>
            );
          })}
        </div>
      </div>

      {/* 상품 그리드 */}
      {items.length === 0 ? (
        <div className="rounded-xl border border-dashed border-gray-200 bg-white py-20 text-center text-gray-400">
          <svg className="mx-auto mb-4 h-12 w-12 text-gray-200" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p className="font-medium">검색 결과가 없습니다.</p>
          {q && <p className="mt-1 text-sm">다른 검색어로 시도해보세요.</p>}
        </div>
      ) : (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
          {items.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      )}

      {/* 페이지네이션 */}
      {pageMeta.totalPages > 1 && (
        <div className="mt-10 flex items-center justify-center gap-2">
          {Array.from({ length: pageMeta.totalPages }, (_, i) => {
            const isActive = i === pageMeta.page;
            const href = `?${new URLSearchParams({ ...(q ? { q } : {}), sort, page: String(i) }).toString()}`;
            return (
              <a
                key={i}
                href={href}
                className={`flex h-9 w-9 items-center justify-center rounded-full text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-600 text-white'
                    : 'border border-gray-300 text-gray-600 hover:border-blue-400 hover:text-blue-600'
                }`}
              >
                {i + 1}
              </a>
            );
          })}
        </div>
      )}
    </div>
  );
}
