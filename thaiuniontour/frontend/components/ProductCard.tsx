'use client';

import Link from 'next/link';
import type { ProductSummary } from '@/lib/api';

interface ProductCardProps {
  product: ProductSummary;
}

function formatPrice(price: number): string {
  return price.toLocaleString('ko-KR') + '원';
}

export default function ProductCard({ product }: ProductCardProps) {
  const isSoldOut = product.status === 'SOLD_OUT';

  return (
    <Link
      href={`/products/${product.id}`}
      className={`group block rounded-xl border border-gray-100 bg-white shadow-sm hover:shadow-md transition-shadow overflow-hidden ${isSoldOut ? 'opacity-75' : ''}`}
    >
      {/* 썸네일 */}
      <div className="relative aspect-[4/3] w-full overflow-hidden bg-gray-100">
        {product.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            src={product.thumbnailUrl}
            alt={product.name}
            className="h-full w-full object-cover group-hover:scale-105 transition-transform duration-300"
            onError={(e) => {
              (e.target as HTMLImageElement).src = '/placeholder.svg';
            }}
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-blue-50 to-blue-100">
            <svg className="h-12 w-12 text-blue-200" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          </div>
        )}

        {/* 매진 뱃지 */}
        {isSoldOut && (
          <div className="absolute inset-0 flex items-center justify-center bg-black/40">
            <span className="rounded-full bg-white px-3 py-1 text-xs font-bold text-gray-700 shadow">
              매진
            </span>
          </div>
        )}
      </div>

      {/* 텍스트 */}
      <div className="p-3">
        <h3 className="line-clamp-2 text-sm font-semibold text-gray-800 group-hover:text-blue-600 transition-colors leading-snug">
          {product.name}
        </h3>
        {product.summary && (
          <p className="mt-1 line-clamp-1 text-xs text-gray-500">{product.summary}</p>
        )}
        <p className="mt-2 text-base font-bold text-blue-600">
          {formatPrice(product.basePrice)}
          <span className="ml-1 text-xs font-normal text-gray-500">부터</span>
        </p>
      </div>
    </Link>
  );
}
