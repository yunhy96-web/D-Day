export const dynamic = 'force-dynamic';

import Link from 'next/link';
import { getProducts, getCategoryTree } from '@/lib/api';
import ProductCard from '@/components/ProductCard';

export default async function HomePage() {
  const [{ items: products }, themes] = await Promise.all([
    getProducts({ size: 8 }),
    getCategoryTree('THEME'),
  ]);

  return (
    <div>
      {/* 히어로 배너 */}
      <section className="relative overflow-hidden bg-gradient-to-br from-blue-600 via-blue-500 to-cyan-400 text-white">
        <div className="mx-auto max-w-7xl px-4 py-20 sm:py-28">
          <div className="max-w-xl">
            <p className="text-sm font-semibold uppercase tracking-widest text-blue-100">
              Thailand No.1 Tour
            </p>
            <h1 className="mt-3 text-4xl font-bold leading-tight sm:text-5xl">
              태국 여행의 모든 것,<br />
              <span className="text-cyan-200">ThaiUnionTour</span>
            </h1>
            <p className="mt-4 text-lg text-blue-100">
              방콕·푸켓·치앙마이 투어부터 패키지·입장권까지.<br />
              현지 전문가가 엄선한 여행상품을 만나보세요.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/search"
                className="rounded-full bg-white px-6 py-3 text-sm font-semibold text-blue-600 shadow hover:bg-blue-50 transition-colors"
              >
                전체 상품 보기
              </Link>
              <Link
                href="/search?q=방콕"
                className="rounded-full border border-white/40 bg-white/10 px-6 py-3 text-sm font-semibold text-white hover:bg-white/20 transition-colors"
              >
                방콕 투어
              </Link>
            </div>
          </div>
        </div>
        {/* 장식 원 */}
        <div className="absolute -right-16 -top-16 h-64 w-64 rounded-full bg-white/10" />
        <div className="absolute -bottom-10 right-20 h-40 w-40 rounded-full bg-white/10" />
      </section>

      {/* 테마 카테고리 바로가기 */}
      {themes.length > 0 && (
        <section className="mx-auto max-w-7xl px-4 py-10">
          <h2 className="mb-5 text-lg font-bold text-gray-800">테마별 여행</h2>
          <div className="flex flex-wrap gap-2">
            {themes.map((cat) => (
              <Link
                key={cat.id}
                href={`/c/${cat.slug}`}
                className="rounded-full border border-blue-200 bg-blue-50 px-4 py-1.5 text-sm font-medium text-blue-700 hover:bg-blue-100 transition-colors"
              >
                {cat.name}
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* 상품 그리드 */}
      <section className="mx-auto max-w-7xl px-4 pb-16">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-bold text-gray-800">인기 여행상품</h2>
          <Link href="/search" className="text-sm text-blue-600 hover:underline">
            전체보기 →
          </Link>
        </div>

        {products.length === 0 ? (
          <div className="rounded-xl border border-dashed border-gray-200 bg-white py-20 text-center text-gray-400">
            <p>등록된 상품이 없습니다.</p>
            <p className="mt-1 text-xs">백엔드 서버를 확인해주세요.</p>
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
            {products.map((product) => (
              <ProductCard key={product.id} product={product} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
