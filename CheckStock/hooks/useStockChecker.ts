import { useState, useCallback, useRef, useEffect } from 'react';
import { generateInjectedJs } from '@/utils/generateInjectedJs';
import { checkSiteNowByUrl } from '@/utils/api';
import { fireLocalAlert } from '@/utils/pushNotifications';
import {
  getLatestSnapshot,
  insertSnapshot,
  insertChange,
} from '@/utils/database';
import type { Product, Site } from '@/types';

export type CrawlMode = 'backend' | 'app';

export interface StockState {
  totalCount: number | null;
  matchedCount: number | null;
  products: Product[];
  lastChecked: Date | null;
  isLoading: boolean;
  error: string | null;
}

const ALERT_COOLDOWN_MS = 5 * 60 * 1000;

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

function applyKeywordFilter(
  products: Product[],
  include: string[],
  exclude: string[]
): Product[] {
  const inc = include ?? [];
  const exc = exclude ?? [];
  return products.filter((p) => {
    if (inc.length > 0 && !inc.some((k) => p.name.includes(k))) return false;
    if (exc.some((k) => p.name.includes(k))) return false;
    return true;
  });
}

export function useStockChecker(site: Site | null, mode: CrawlMode = 'app') {
  const [state, setState] = useState<StockState>({
    totalCount: null,
    matchedCount: null,
    products: [],
    lastChecked: null,
    isLoading: false,
    error: null,
  });
  const [countdown, setCountdown] = useState(site?.refreshInterval ?? 30);
  const webViewRef = useRef<any>(null);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const countdownValueRef = useRef(site?.refreshInterval ?? 30);

  const modeRef = useRef<CrawlMode>(mode);
  const requestSeqRef = useRef(0);

  // 알림 cooldown 관리
  const lastAlertedAtRef = useRef<number>(0);
  const lastAlertedNamesRef = useRef<Set<string>>(new Set());

  const refreshInterval = site?.refreshInterval ?? 30;
  const injectedJs = site && mode === 'app' ? generateInjectedJs(site.selector, site.countSelector) : '';

  useEffect(() => {
    modeRef.current = mode;
  }, [mode]);

  const persistSnapshotIfChanged = useCallback(
    async (matchedProducts: Product[], matchedCount: number, now: Date) => {
      if (!site) return;
      try {
        const prev = await getLatestSnapshot(site.id);
        const hasChange =
          !prev ||
          prev.productCount !== matchedCount ||
          JSON.stringify(prev.products.map((p) => p.name).sort()) !==
            JSON.stringify(matchedProducts.map((p) => p.name).sort());

        if (hasChange) {
          await insertSnapshot({
            id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
            siteId: site.id,
            productCount: matchedCount,
            products: matchedProducts,
            checkedAt: now.toISOString(),
          });

          if (prev) {
            const { added, removed } = diffProducts(prev.products, matchedProducts);
            if (added.length > 0 || removed.length > 0) {
              await insertChange({
                id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
                siteId: site.id,
                oldCount: prev.productCount,
                newCount: matchedCount,
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
    },
    [site]
  );

  /** 매칭 상품에 대해 cooldown 고려해서 로컬 알림 발사 (신규 매칭은 즉시) */
  const maybeFireLocalAlert = useCallback(
    async (matched: Product[]) => {
      if (!site || matched.length === 0) return;
      const currentNames = matched.map((p) => p.name);
      const lastSet = lastAlertedNamesRef.current;
      const hasNew = currentNames.some((n) => !lastSet.has(n));
      const now = Date.now();
      const cooldownExpired = now - lastAlertedAtRef.current >= ALERT_COOLDOWN_MS;

      if (!hasNew && !cooldownExpired) return;

      const body =
        matched.length === 1
          ? `재고 있음: ${matched[0].name}`
          : `재고 ${matched.length}개: ${matched[0].name} 외`;
      await fireLocalAlert(site.name, body, {
        siteId: site.id,
        firstProductUrl: matched[0].url,
      });
      lastAlertedAtRef.current = now;
      lastAlertedNamesRef.current = new Set(currentNames);
    },
    [site]
  );

  const fetchFromBackend = useCallback(async () => {
    if (!site) return;
    const seq = ++requestSeqRef.current;
    const requestedMode = modeRef.current;
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    try {
      const res = await checkSiteNowByUrl(site.url, {
        includeKeywords: site.includeKeywords,
        excludeKeywords: site.excludeKeywords,
      });
      if (seq !== requestSeqRef.current || modeRef.current !== requestedMode) return;
      if (!res) {
        setState((prev) => ({
          ...prev,
          isLoading: false,
          error: '백엔드에 등록된 사이트가 없습니다',
        }));
        return;
      }

      const now = new Date();
      const products: Product[] = res.matched.map((p) => ({ name: p.name, url: p.url }));
      await persistSnapshotIfChanged(products, res.matchedCount, now);
      await maybeFireLocalAlert(products);
      setState({
        totalCount: res.totalCount,
        matchedCount: res.matchedCount,
        products,
        lastChecked: now,
        isLoading: false,
        error: null,
      });
    } catch (e) {
      if (seq !== requestSeqRef.current || modeRef.current !== requestedMode) return;
      setState((prev) => ({
        ...prev,
        isLoading: false,
        error: e instanceof Error ? e.message : '백엔드 요청 실패',
      }));
    }
  }, [site, persistSnapshotIfChanged, maybeFireLocalAlert]);

  // app 모드 — WebView 추출 → site의 키워드로 로컬 필터 → 로컬 알림
  const handleMessage = useCallback(
    async (event: any) => {
      if (!site) return;
      const seq = ++requestSeqRef.current;
      const requestedMode = modeRef.current;
      try {
        const data = JSON.parse(event.nativeEvent.data);
        if (typeof data?.count !== 'number') return;

        const rawProducts = (Array.isArray(data.products) ? data.products : []) as {
          name: string;
          href: string;
        }[];
        const products: Product[] = rawProducts
          .filter((p) => p && typeof p.name === 'string')
          .map((p) => ({
            name: p.name,
            url: p.href?.startsWith('http') ? p.href : `${site.baseUrl}${p.href ?? ''}`,
          }));
        const now = new Date();

        const matched = applyKeywordFilter(
          products,
          site.includeKeywords,
          site.excludeKeywords
        );
        if (seq !== requestSeqRef.current || modeRef.current !== requestedMode) return;

        await persistSnapshotIfChanged(matched, matched.length, now);
        await maybeFireLocalAlert(matched);

        setState({
          totalCount: products.length,
          matchedCount: matched.length,
          products: matched,
          lastChecked: now,
          isLoading: false,
          error: null,
        });
      } catch (err) {
        if (seq !== requestSeqRef.current || modeRef.current !== requestedMode) return;
        setState((prev) => ({
          ...prev,
          isLoading: false,
          error: err instanceof Error ? err.message : '데이터 처리 실패',
        }));
      }
    },
    [site, persistSnapshotIfChanged, maybeFireLocalAlert]
  );

  const handleLoadStart = useCallback(() => {
    queueMicrotask(() => setState((prev) => ({ ...prev, isLoading: true, error: null })));
  }, []);

  const handleError = useCallback(() => {
    queueMicrotask(() =>
      setState((prev) => ({ ...prev, isLoading: false, error: '페이지 로드 실패' }))
    );
  }, []);

  useEffect(() => {
    if (!site) return;
    if (mode === 'backend') {
      fetchFromBackend();
    }
  }, [site, mode, fetchFromBackend]);

  useEffect(() => {
    countdownValueRef.current = refreshInterval;
    setCountdown(refreshInterval);
  }, [refreshInterval]);

  const resetCountdown = useCallback(() => {
    countdownValueRef.current = refreshInterval;
    setCountdown(refreshInterval);
  }, [refreshInterval]);

  const refresh = useCallback(() => {
    resetCountdown();
    if (mode === 'backend') {
      fetchFromBackend();
      return;
    }
    setState((prev) => ({ ...prev, isLoading: true, error: null }));
    if (webViewRef.current) {
      webViewRef.current.reload();
    }
  }, [mode, fetchFromBackend, resetCountdown]);

  useEffect(() => {
    if (countdownRef.current) clearInterval(countdownRef.current);

    countdownRef.current = setInterval(() => {
      countdownValueRef.current -= 1;
      if (countdownValueRef.current <= 0) {
        if (mode === 'backend') {
          fetchFromBackend();
        } else if (webViewRef.current) {
          webViewRef.current.reload();
        }
        countdownValueRef.current = refreshInterval;
      }
      setCountdown(countdownValueRef.current);
    }, 1000);

    return () => {
      if (countdownRef.current) clearInterval(countdownRef.current);
    };
  }, [refreshInterval, mode, fetchFromBackend]);

  return {
    ...state,
    countdown,
    refreshInterval,
    refresh,
    webViewRef,
    injectedJs,
    handleMessage,
    handleLoadStart,
    handleError,
  };
}
