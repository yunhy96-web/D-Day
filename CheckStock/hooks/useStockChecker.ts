import { useState, useCallback, useRef, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

const TARGET_URL =
  'https://www.ralphlauren.co.kr/men/brands/double-rl?prefn1=CategoryCode&prefv1=%EB%8D%B0%EB%8B%98';

const BASE_URL = 'https://www.ralphlauren.co.kr';
const STORAGE_KEY_INTERVAL = 'refreshInterval';
const DEFAULT_INTERVAL = 30; // 초

const INJECTED_JS = `
(function() {
  function extract() {
    var products = [];
    var links = document.querySelectorAll('a.name-link.js-pdp-link');
    for (var i = 0; i < links.length; i++) {
      var name = links[i].textContent.trim();
      var href = links[i].getAttribute('href') || '';
      if (name) products.push({ name: name, href: href });
    }
    var countInput = document.querySelector('input[name="totalProductsCount"]');
    var count = countInput ? parseInt(countInput.value, 10) : products.length;
    window.ReactNativeWebView.postMessage(JSON.stringify({ count: count, products: products }));
  }
  if (document.readyState === 'complete') {
    setTimeout(extract, 1000);
  } else {
    window.addEventListener('load', function() { setTimeout(extract, 1000); });
  }
})();
true;
`;

export interface Product {
  name: string;
  url: string;
}

export interface StockState {
  count: number | null;
  products: Product[];
  lastChecked: Date | null;
  isLoading: boolean;
  error: string | null;
}

export function useStockChecker() {
  const [state, setState] = useState<StockState>({
    count: null,
    products: [],
    lastChecked: null,
    isLoading: false,
    error: null,
  });
  const [refreshInterval, setRefreshInterval] = useState(DEFAULT_INTERVAL);
  const [countdown, setCountdown] = useState(DEFAULT_INTERVAL);
  const webViewRef = useRef<any>(null);
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // 저장된 새로고침 주기 로드
  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY_INTERVAL).then((val) => {
      if (val) {
        const saved = parseInt(val, 10);
        setRefreshInterval(saved);
        setCountdown(saved);
      }
    });
  }, []);

  const handleMessage = useCallback((event: any) => {
    try {
      const data = JSON.parse(event.nativeEvent.data);
      if (data.count !== undefined) {
        const products: Product[] = (data.products || []).map(
          (p: { name: string; href: string }) => ({
            name: p.name,
            url: p.href.startsWith('http') ? p.href : `${BASE_URL}${p.href}`,
          })
        );
        setState({
          count: data.count,
          products,
          lastChecked: new Date(),
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
  }, []);

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

  // 새로고침 주기 변경
  const updateRefreshInterval = useCallback(async (seconds: number) => {
    setRefreshInterval(seconds);
    setCountdown(seconds);
    await AsyncStorage.setItem(STORAGE_KEY_INTERVAL, seconds.toString());
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
    updateRefreshInterval,
    webViewRef,
    targetUrl: TARGET_URL,
    injectedJs: INJECTED_JS,
    refresh,
    handleMessage,
    handleLoadStart,
    handleError,
  };
}
