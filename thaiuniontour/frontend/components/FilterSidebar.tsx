'use client';

import { useRouter, usePathname, useSearchParams } from 'next/navigation';
import { useCallback } from 'react';

interface Destination {
  id: string;
  name: string;
}

interface FilterSidebarProps {
  destinations: Destination[];
  currentDestinationId?: string;
  currentMinPrice?: string;
  currentMaxPrice?: string;
  currentSort?: string;
}

const SORT_OPTIONS = [
  { value: 'latest', label: '최신순' },
  { value: 'priceAsc', label: '낮은가격순' },
  { value: 'priceDesc', label: '높은가격순' },
] as const;

export default function FilterSidebar({
  destinations,
  currentDestinationId = '',
  currentMinPrice = '',
  currentMaxPrice = '',
  currentSort = 'latest',
}: FilterSidebarProps) {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();

  const updateQuery = useCallback(
    (updates: Record<string, string>) => {
      const params = new URLSearchParams(searchParams.toString());
      // 변경 항목 반영, 빈 값은 제거
      Object.entries(updates).forEach(([key, val]) => {
        if (val) {
          params.set(key, val);
        } else {
          params.delete(key);
        }
      });
      // 필터 변경 시 page 리셋
      params.delete('page');
      router.push(`${pathname}?${params.toString()}`);
    },
    [router, pathname, searchParams],
  );

  const handleDestination = (id: string) => {
    updateQuery({ destinationId: id });
  };

  const handleSort = (sort: string) => {
    updateQuery({ sort });
  };

  const handleMinPrice = (e: React.ChangeEvent<HTMLInputElement>) => {
    updateQuery({ minPrice: e.target.value });
  };

  const handleMaxPrice = (e: React.ChangeEvent<HTMLInputElement>) => {
    updateQuery({ maxPrice: e.target.value });
  };

  const handleReset = () => {
    router.push(pathname);
  };

  const hasFilter =
    currentDestinationId || currentMinPrice || currentMaxPrice || currentSort !== 'latest';

  return (
    <aside className="w-full rounded-xl border border-cream-200 bg-white p-5 shadow-sm">
      {/* 헤더 */}
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-sm font-bold text-ink-800">필터</h2>
        {hasFilter && (
          <button
            onClick={handleReset}
            className="text-xs text-brand-600 hover:underline"
          >
            초기화
          </button>
        )}
      </div>

      {/* 정렬 */}
      <div className="mb-5">
        <p className="mb-2 text-xs font-semibold text-ink-400 uppercase tracking-wider">정렬</p>
        <div className="flex flex-col gap-1">
          {SORT_OPTIONS.map(({ value, label }) => (
            <button
              key={value}
              onClick={() => handleSort(value)}
              className={`rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                currentSort === value
                  ? 'bg-brand-100 font-semibold text-brand-700'
                  : 'text-ink-600 hover:bg-cream-200'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* 지역 */}
      {destinations.length > 0 && (
        <div className="mb-5">
          <p className="mb-2 text-xs font-semibold text-ink-400 uppercase tracking-wider">지역</p>
          <div className="flex flex-col gap-1">
            <button
              onClick={() => handleDestination('')}
              className={`rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                !currentDestinationId
                  ? 'bg-brand-100 font-semibold text-brand-700'
                  : 'text-ink-600 hover:bg-cream-200'
              }`}
            >
              전체 지역
            </button>
            {destinations.map((dest) => (
              <button
                key={dest.id}
                onClick={() => handleDestination(String(dest.id))}
                className={`rounded-lg px-3 py-2 text-left text-sm transition-colors ${
                  currentDestinationId === String(dest.id)
                    ? 'bg-brand-100 font-semibold text-brand-700'
                    : 'text-ink-600 hover:bg-cream-200'
                }`}
              >
                {dest.name}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 가격 범위 */}
      <div>
        <p className="mb-2 text-xs font-semibold text-ink-400 uppercase tracking-wider">가격 범위</p>
        <div className="flex items-center gap-2">
          <input
            type="number"
            placeholder="최소"
            min={0}
            defaultValue={currentMinPrice}
            onBlur={handleMinPrice}
            className="w-full rounded-lg border border-cream-300 px-3 py-2 text-sm text-ink-700 placeholder-ink-400 focus:border-brand-400 focus:outline-none"
          />
          <span className="flex-shrink-0 text-ink-400">~</span>
          <input
            type="number"
            placeholder="최대"
            min={0}
            defaultValue={currentMaxPrice}
            onBlur={handleMaxPrice}
            className="w-full rounded-lg border border-cream-300 px-3 py-2 text-sm text-ink-700 placeholder-ink-400 focus:border-brand-400 focus:outline-none"
          />
        </div>
        <p className="mt-1 text-xs text-ink-400">입력 후 포커스 해제 시 적용</p>
      </div>
    </aside>
  );
}
