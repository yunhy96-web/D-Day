export const dynamic = 'force-dynamic';

import { notFound } from 'next/navigation';
import type { Metadata } from 'next';
import Link from 'next/link';
import { getCategoryBySlug, getProducts, getCategoryTree } from '@/lib/api';
import Breadcrumb from '@/components/Breadcrumb';
import ProductCard from '@/components/ProductCard';
import FilterSidebar from '@/components/FilterSidebar';

interface Props {
  params: { slug: string[] };
  searchParams: {
    destinationId?: string;
    minPrice?: string;
    maxPrice?: string;
    sort?: string;
    page?: string;
  };
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const lastSlug = params.slug[params.slug.length - 1];
  const category = await getCategoryBySlug('THEME', lastSlug);
  if (!category) {
    return { title: '카테고리를 찾을 수 없습니다 | ThaiUnionTour' };
  }
  return {
    title: `${category.name} | ThaiUnionTour`,
    description: `${category.name} 여행상품을 확인해보세요.`,
  };
}

// destinations 트리를 평탄화 (1단계 목록)
function flattenDestinations(
  nodes: { id: string; name: string; slug: string; children?: { id: string; name: string; slug: string }[] }[],
): { id: string; name: string }[] {
  return nodes.flatMap((node) => [
    { id: node.id, name: node.name },
    ...(node.children ?? []).map((c) => ({ id: c.id, name: c.name })),
  ]);
}

export default async function CategoryLandingPage({ params, searchParams }: Props) {
  const lastSlug = params.slug[params.slug.length - 1];

  // 카테고리 정보 먼저 확인
  const category = await getCategoryBySlug('THEME', lastSlug);
  if (!category) {
    notFound();
  }

  const destinationId = searchParams.destinationId ?? '';
  const minPrice = searchParams.minPrice ?? '';
  const maxPrice = searchParams.maxPrice ?? '';
  const sort = (searchParams.sort as 'latest' | 'priceAsc' | 'priceDesc') || 'latest';
  const page = Number(searchParams.page ?? 0);

  // 상품 목록 + 지역 목록 병렬 fetch
  const [{ items, page: pageMeta }, destinationTree] = await Promise.all([
    getProducts({
      categoryId: category.id,
      destinationId: destinationId || undefined,
      minPrice: minPrice || undefined,
      maxPrice: maxPrice || undefined,
      sort,
      page,
      size: 12,
    }),
    getCategoryTree('DESTINATION'),
  ]);

  const destinations = flattenDestinations(destinationTree);

  // 브레드크럼: breadcrumb 배열의 name만 추출 (홈은 Breadcrumb 컴포넌트 내부 처리)
  const breadcrumbItems = category.breadcrumb.map((b) => b.name);

  // 현재 페이지 URL 빌더 (페이지네이션용)
  function pageHref(p: number) {
    const qs = new URLSearchParams();
    if (destinationId) qs.set('destinationId', destinationId);
    if (minPrice) qs.set('minPrice', minPrice);
    if (maxPrice) qs.set('maxPrice', maxPrice);
    if (sort !== 'latest') qs.set('sort', sort);
    qs.set('page', String(p));
    return `?${qs.toString()}`;
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      {/* 브레드크럼 */}
      <div className="mb-4">
        <Breadcrumb items={breadcrumbItems} />
      </div>

      {/* 카테고리 제목 */}
      <h1 className="mb-4 text-2xl font-bold text-ink-900">{category.name}</h1>

      {/* 하위 카테고리 칩 */}
      {category.children.length > 0 && (
        <div className="mb-6 flex flex-wrap gap-2">
          {category.children.map((child) => (
            <Link
              key={child.id}
              href={`/c/${child.slug}`}
              className="rounded-full border border-brand-200 bg-brand-50 px-4 py-1.5 text-sm font-medium text-brand-700 hover:bg-brand-100 hover:border-brand-400 transition-colors"
            >
              {child.name}
            </Link>
          ))}
        </div>
      )}

      {/* 2컬럼 레이아웃: 필터 사이드바 + 상품 그리드 */}
      <div className="flex flex-col gap-6 lg:flex-row lg:items-start">
        {/* 필터 사이드바 */}
        <div className="w-full lg:w-56 lg:flex-shrink-0">
          <FilterSidebar
            destinations={destinations}
            currentDestinationId={destinationId}
            currentMinPrice={minPrice}
            currentMaxPrice={maxPrice}
            currentSort={sort}
          />
        </div>

        {/* 상품 영역 */}
        <div className="flex-1 min-w-0">
          {/* 건수 표시 */}
          <p className="mb-4 text-sm text-ink-500">
            총{' '}
            <span className="font-semibold text-ink-800">
              {pageMeta.totalElements.toLocaleString('ko-KR')}건
            </span>
          </p>

          {/* 빈 상태 */}
          {items.length === 0 ? (
            <div className="rounded-xl border border-dashed border-cream-300 bg-white py-20 text-center text-ink-400">
              <svg
                className="mx-auto mb-4 h-12 w-12 text-ink-200"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1}
                  d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
              <p className="font-medium">해당 조건의 상품이 없습니다.</p>
              <p className="mt-1 text-sm">필터 조건을 변경해보세요.</p>
            </div>
          ) : (
            <div className="grid grid-cols-2 gap-4 md:grid-cols-3">
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
                return (
                  <a
                    key={i}
                    href={pageHref(i)}
                    className={`flex h-9 w-9 items-center justify-center rounded-full text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-brand-500 text-white'
                        : 'border border-ink-200 text-ink-600 hover:border-brand-400 hover:text-brand-600'
                    }`}
                  >
                    {i + 1}
                  </a>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
