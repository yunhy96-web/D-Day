import { useState, useCallback, useRef, useEffect } from 'react';
import { generateInjectedJs } from '@/utils/generateInjectedJs';
import { getLatestSnapshot, insertSnapshot, insertChange } from '@/utils/database';
import type { Product, Site } from '@/types';

export interface StockState {
  count: number | null;
  products: Product[];
  lastChecked: Date | null;
  isLoading: boolean;
  error: string | null;
}

function diffProducts(
  oldProducts: Product[],
  newProducts: Product[]
): { added: Product[]; removed: Product[] } {
  const oldNames = new Set(oldProducts.map((p) => p.name));
  const newNames = new Set(newProducts.map((p) => p.name));

  const added = newProducts.filter((p) => !oldNames.has(p.name));
  const removed = oldProducts.filter((p) => !newNames.has(p.name));

  return { added, removed };
}

export function useStockChecker(site: Site | null) {
  const [state, setState] = useState<StockState>({
    count: null,
    products: [],
    lastChecked: null,
    isLoading: false,
    error: null,
  });
  const [countdown, setCountdown] = useState(site?.refreshInterval ?? 30);
  const webViewRef = useRef<any>(null);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const refreshInterval = site?.refreshInterval ?? 30;
  const injectedJs = site ? generateInjectedJs(site.selector, site.countSelector) : '';

  // refreshInterval 변경 시 카운트다운 리셋
  useEffect(() => {
    setCountdown(refreshInterval);
  }, [refreshInterval]);

  const handleMessage = useCallback(
    async (event: any) => {
      if (!site) return;
      try {
        const data = JSON.parse(event.nativeEvent.data);
        if (data.count !== undefined) {
          const products: Product[] = (data.products || []).map(
            (p: { name: string; href: string }) => ({
              name: p.name,
              url: p.href.startsWith('http') ? p.href : `${site.baseUrl}${p.href}`,
            })
          );

          const newCount = data.count as number;
          const now = new Date();

          // 이전 스냅샷과 비교
          try {
            const prev = await getLatestSnapshot(site.id);
            const hasChange =
              !prev ||
              prev.productCount !== newCount ||
              JSON.stringify(prev.products.map((p) => p.name).sort()) !==
                JSON.stringify(products.map((p) => p.name).sort());

            if (hasChange) {
              // 스냅샷 저장
              await insertSnapshot({
                id: Date.now().toString(),
                siteId: site.id,
                productCount: newCount,
                products,
                checkedAt: now.toISOString(),
              });

              // 변동 기록 저장 (첫 스냅샷이 아닐 때만)
              if (prev) {
                const { added, removed } = diffProducts(prev.products, products);
                if (added.length > 0 || removed.length > 0) {
                  await insertChange({
                    id: (Date.now() + 1).toString(),
                    siteId: site.id,
                    oldCount: prev.productCount,
                    newCount,
                    addedProducts: added,
                    removedProducts: removed,
                    detectedAt: now.toISOString(),
                  });
                }
              }
            }
          } catch (dbErr) {
            console.error('DB write error:', dbErr);
          }

          setState({
            count: newCount,
            products,
            lastChecked: now,
            isLoading: false,
            error: null,
          });
        }
      } catch {
        setState((prev) => ({
          ...prev,
          isLoading: false,
          error: '데이터 파싱 실패',
        }));
      }
    },
    [site]
  );

  const resetCountdown = useCallback(() => {
    setCountdown(refreshInterval);
  }, [refreshInterval]);

  const refresh = useCallback(() => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    resetCountdown();
    if (webViewRef.current) {
      webViewRef.current.reload();
    }
  }, [resetCountdown]);

  const handleLoadStart = useCallback(() => {
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
  }, []);

  const handleError = useCallback(() => {
    setState((prev) => ({
      ...prev,
      isLoading: false,
      error: '페이지 로드 실패',
    }));
  }, []);

  // 카운트다운 타이머
  useEffect(() => {
    if (countdownRef.current) clearInterval(countdownRef.current);

    countdownRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          if (webViewRef.current) {
            webViewRef.current.reload();
          }
          return refreshInterval;
        }
        return prev - 1;
      });
    }, 1000);

    return () => {
      if (countdownRef.current) {
        clearInterval(countdownRef.current);
      }
    };
  }, [refreshInterval]);

  return {
    ...state,
    countdown,
    refreshInterval,
    webViewRef,
    injectedJs,
    refresh,
    handleMessage,
    handleLoadStart,
    handleError,
  };
}
