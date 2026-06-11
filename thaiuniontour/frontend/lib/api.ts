const BASE = process.env.API_INTERNAL_BASE || 'http://localhost:8080';

// ── 공통 타입 ──────────────────────────────────────────────────────────
export interface CategoryNode {
  id: number;
  name: string;
  slug: string;
  level: number;
  children?: CategoryNode[];
}

export interface ProductSummary {
  id: number;
  name: string;
  summary: string;
  thumbnailUrl: string | null;
  basePrice: number;
  status: 'ON_SALE' | 'SOLD_OUT' | 'HIDDEN';
}

export interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProductDetail {
  id: number;
  name: string;
  summary: string;
  description: string;
  stockType: 'LIMITED' | 'UNLIMITED';
  basePrice: number;
  status: 'ON_SALE' | 'SOLD_OUT' | 'HIDDEN';
  thumbnailUrl: string | null;
  categories: {
    theme: { id: number; name: string }[];
    destination: { id: number; name: string }[];
  };
  breadcrumb: string[];
}

// ── 내부 헬퍼 ─────────────────────────────────────────────────────────
async function apiFetch<T>(path: string): Promise<T> {
  const url = `${BASE}${path}`;
  const res = await fetch(url, { cache: 'no-store' });
  if (!res.ok) {
    throw new Error(`API ${res.status}: ${url}`);
  }
  const json = await res.json();
  if (!json.success) {
    throw new Error(`API error: ${JSON.stringify(json)}`);
  }
  return json.data as T;
}

// ── 공개 API 함수 ──────────────────────────────────────────────────────
export async function getCategoryTree(type: 'THEME' | 'DESTINATION'): Promise<CategoryNode[]> {
  try {
    return await apiFetch<CategoryNode[]>(`/api/v1/categories?type=${type}`);
  } catch {
    return [];
  }
}

export interface GetProductsParams {
  q?: string;
  categoryId?: number | string;
  sort?: 'latest' | 'priceAsc' | 'priceDesc';
  page?: number;
  size?: number;
}

export async function getProducts(params: GetProductsParams = {}): Promise<{
  items: ProductSummary[];
  page: PageMeta;
}> {
  try {
    const qs = new URLSearchParams();
    if (params.q) qs.set('q', params.q);
    if (params.categoryId != null) qs.set('categoryId', String(params.categoryId));
    if (params.sort) qs.set('sort', params.sort);
    if (params.page != null) qs.set('page', String(params.page));
    if (params.size != null) qs.set('size', String(params.size));

    const qStr = qs.toString();
    const path = `/api/v1/products${qStr ? `?${qStr}` : ''}`;

    const res = await fetch(`${BASE}${path}`, { cache: 'no-store' });
    if (!res.ok) return { items: [], page: { page: 0, size: 20, totalElements: 0, totalPages: 0 } };
    const json = await res.json();
    if (!json.success) return { items: [], page: { page: 0, size: 20, totalElements: 0, totalPages: 0 } };

    return {
      items: (json.data ?? []) as ProductSummary[],
      page: (json.page ?? { page: 0, size: 20, totalElements: 0, totalPages: 0 }) as PageMeta,
    };
  } catch {
    return { items: [], page: { page: 0, size: 20, totalElements: 0, totalPages: 0 } };
  }
}

export async function getProduct(id: number | string): Promise<ProductDetail | null> {
  try {
    return await apiFetch<ProductDetail>(`/api/v1/products/${id}`);
  } catch {
    return null;
  }
}
