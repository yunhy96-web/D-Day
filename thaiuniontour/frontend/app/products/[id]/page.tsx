export const dynamic = 'force-dynamic';

import { notFound } from 'next/navigation';
import type { Metadata } from 'next';
import { getProduct } from '@/lib/api';
import Breadcrumb from '@/components/Breadcrumb';

interface Props {
  params: { id: string };
}

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const product = await getProduct(params.id);
  if (!product) {
    return { title: '상품을 찾을 수 없습니다 | ThaiUnionTour' };
  }
  return {
    title: `${product.name} | ThaiUnionTour`,
    description: product.summary,
    openGraph: {
      title: product.name,
      description: product.summary,
      images: product.thumbnailUrl ? [{ url: product.thumbnailUrl }] : [],
    },
  };
}

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR') + '원';
}

export default async function ProductDetailPage({ params }: Props) {
  const product = await getProduct(params.id);

  if (!product) {
    notFound();
  }

  const isSoldOut = product.status === 'SOLD_OUT';
  const allCategories = [
    ...(product.categories.theme ?? []),
    ...(product.categories.destination ?? []),
  ];

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      {/* 브레드크럼 */}
      {product.breadcrumb && product.breadcrumb.length > 0 && (
        <div className="mb-6">
          <Breadcrumb items={product.breadcrumb} />
        </div>
      )}

      <div className="grid grid-cols-1 gap-8 lg:grid-cols-5">
        {/* 왼쪽: 갤러리 + 상세 내용 */}
        <div className="lg:col-span-3">
          {/* 썸네일 갤러리 */}
          <div className="overflow-hidden rounded-2xl bg-gray-100 aspect-[16/9]">
            {product.thumbnailUrl ? (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={product.thumbnailUrl}
                alt={product.name}
                className="h-full w-full object-cover"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-blue-50 to-blue-100">
                <svg className="h-20 w-20 text-blue-200" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                </svg>
              </div>
            )}
          </div>

          {/* 제목/요약 (모바일에서 갤러리 아래) */}
          <div className="mt-6 lg:hidden">
            <ProductTitle
              name={product.name}
              summary={product.summary}
              basePrice={product.basePrice}
              isSoldOut={isSoldOut}
              categories={allCategories}
            />
          </div>

          {/* 상세 설명 HTML */}
          {product.description && (
            <div className="mt-8">
              <h2 className="mb-4 text-base font-bold text-gray-800 border-b border-gray-200 pb-2">
                상품 상세
              </h2>
              <div
                className="prose prose-sm max-w-none text-gray-700 leading-relaxed"
                dangerouslySetInnerHTML={{ __html: product.description }}
              />
            </div>
          )}
        </div>

        {/* 오른쪽: 예약 패널 (sticky) */}
        <div className="lg:col-span-2">
          <div className="sticky top-24 rounded-2xl border border-gray-200 bg-white p-6 shadow-sm">
            {/* 데스크탑에서만 제목 표시 */}
            <div className="hidden lg:block">
              <ProductTitle
                name={product.name}
                summary={product.summary}
                basePrice={product.basePrice}
                isSoldOut={isSoldOut}
                categories={allCategories}
              />
              <div className="my-4 border-t border-gray-100" />
            </div>

            {/* 가격 */}
            <div className="flex items-baseline gap-1">
              <span className="text-xs text-gray-500">최저</span>
              <span className="text-2xl font-bold text-blue-600">
                {formatPrice(product.basePrice)}
              </span>
              <span className="text-xs text-gray-500">부터</span>
            </div>

            {/* 재고 타입 표시 */}
            {product.stockType === 'LIMITED' && (
              <p className="mt-1 text-xs text-amber-600">
                * 선착순 한정 수량 상품입니다.
              </p>
            )}

            {/* 예약 버튼 (M2 구현 예정) */}
            <div className="mt-6">
              {isSoldOut ? (
                <div className="w-full rounded-xl bg-gray-100 py-3 text-center text-sm font-semibold text-gray-500">
                  매진된 상품입니다
                </div>
              ) : (
                <button
                  disabled
                  title="예약 기능은 M2에서 구현 예정입니다"
                  className="w-full cursor-not-allowed rounded-xl bg-blue-600 py-3 text-center text-sm font-semibold text-white opacity-60"
                >
                  예약하기 (준비중)
                </button>
              )}
              <p className="mt-2 text-center text-xs text-gray-400">
                예약 시스템은 M2에서 오픈 예정입니다.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

// ── 내부 서브컴포넌트 ────────────────────────────────────────────────
interface ProductTitleProps {
  name: string;
  summary: string;
  basePrice: number;
  isSoldOut: boolean;
  categories: { id: string; name: string }[];
}

function ProductTitle({ name, summary, isSoldOut, categories }: ProductTitleProps) {
  return (
    <div>
      {/* 카테고리 칩 */}
      {categories.length > 0 && (
        <div className="mb-2 flex flex-wrap gap-1">
          {categories.map((cat) => (
            <span
              key={cat.id}
              className="rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-600"
            >
              {cat.name}
            </span>
          ))}
          {isSoldOut && (
            <span className="rounded-full bg-red-50 px-2 py-0.5 text-xs font-medium text-red-500">
              매진
            </span>
          )}
        </div>
      )}

      <h1 className="text-xl font-bold text-gray-900 leading-snug">{name}</h1>
      {summary && (
        <p className="mt-2 text-sm text-gray-500 leading-relaxed">{summary}</p>
      )}
    </div>
  );
}
