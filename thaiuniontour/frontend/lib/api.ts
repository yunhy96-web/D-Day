const BASE = process.env.API_INTERNAL_BASE || 'http://localhost:8080';

// ── 공통 타입 ──────────────────────────────────────────────────────────
export interface CategoryNode {
  id: string;
  name: string;
  slug: string;
  level: number;
  children?: CategoryNode[];
}

export interface CategoryDetail {
  id: string;
  name: string;
  slug: string;
  level: number;
  breadcrumb: { id: string; name: string; slug: string }[];
  children: { id: string; name: string; slug: string }[];
}

export interface ProductSummary {
  id: string;
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
  id: string;
  name: string;
  summary: string;
  description: string;
  stockType: 'LIMITED' | 'UNLIMITED';
  basePrice: number;
  status: 'ON_SALE' | 'SOLD_OUT' | 'HIDDEN';
  thumbnailUrl: string | null;
  categories: {
    theme: { id: string; name: string }[];
    destination: { id: string; name: string }[];
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
  categoryId?: string;
  destinationId?: string;
  minPrice?: number | string;
  maxPrice?: number | string;
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
    if (params.destinationId != null) qs.set('destinationId', String(params.destinationId));
    if (params.minPrice != null) qs.set('minPrice', String(params.minPrice));
    if (params.maxPrice != null) qs.set('maxPrice', String(params.maxPrice));
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

export async function getProduct(id: string | string): Promise<ProductDetail | null> {
  try {
    return await apiFetch<ProductDetail>(`/api/v1/products/${id}`);
  } catch {
    return null;
  }
}

export async function getCategoryBySlug(
  type: 'THEME' | 'DESTINATION',
  slug: string,
): Promise<CategoryDetail | null> {
  try {
    return await apiFetch<CategoryDetail>(
      `/api/v1/categories/by-slug?type=${type}&slug=${encodeURIComponent(slug)}`,
    );
  } catch {
    return null;
  }
}

// ── 예약 타입 ──────────────────────────────────────────────────────────
export interface Schedule {
  scheduleId: string;
  saleDate: string; // YYYY-MM-DD
  stock: number | null; // null = 무제한
  price: number;
  status: string;
  soldOut: boolean;
}

export interface BankInfo {
  bank: string;
  account: string;
  holder: string;
  deadline: string;
}

export interface ReservationDetail {
  id: string;
  reservationNo: string;
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED';
  productId: string;
  productScheduleId: string;
  saleDate: string;
  quantity: number;
  amount: number;
  paymentMethod: string;
  reserver: { name: string; email: string; phone: string };
  paymentDeadline: string | null;
  confirmedAt: string | null;
  cancelledAt: string | null;
  bankInfo?: BankInfo;
}

export interface CreateReservationBody {
  productScheduleId: string;
  quantity: number;
  paymentMethod: 'BANK_TRANSFER';
  reserver: { name: string; email: string; phone: string };
}

export interface CreateReservationResult {
  reservationId: string;
  reservationNo: string;
  status: string;
  amount: number;
  paymentMethod: string;
  lookupToken: string;
  bankInfo: BankInfo;
}

export interface PaymentStatus {
  reservationId: string;
  status: 'PAID' | 'PENDING';
  method: string;
  amount: number;
  paidAt: string | null;
}

// ── 서버 전용 예약 API ─────────────────────────────────────────────────
export async function getSchedules(productId: string): Promise<Schedule[]> {
  try {
    return await apiFetch<Schedule[]>(`/api/v1/products/${productId}/schedules`);
  } catch {
    return [];
  }
}

export async function getReservation(id: string): Promise<ReservationDetail | null> {
  try {
    return await apiFetch<ReservationDetail>(`/api/v1/reservations/${id}`);
  } catch {
    return null;
  }
}

export async function getReservationByToken(token: string): Promise<ReservationDetail | null> {
  try {
    return await apiFetch<ReservationDetail>(`/api/v1/reservations/by-token/${token}`);
  } catch {
    return null;
  }
}

export async function lookupReservation(
  no: string,
  email: string,
): Promise<ReservationDetail | null> {
  try {
    return await apiFetch<ReservationDetail>(
      `/api/v1/reservations/lookup?no=${encodeURIComponent(no)}&email=${encodeURIComponent(email)}`,
    );
  } catch {
    return null;
  }
}

// ── 클라이언트 전용 예약 API (상대경로 프록시) ────────────────────────
export async function createReservation(
  body: CreateReservationBody,
): Promise<CreateReservationResult> {
  const res = await fetch('/api/v1/reservations', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  const json = await res.json();
  if (res.status === 409 || !json.success) {
    const code = json?.error?.code ?? '';
    if (code === 'RESERVATION_SOLD_OUT') {
      throw new Error('이 일정은 이미 매진되었습니다.');
    }
    throw new Error(json?.error?.message ?? '예약에 실패했습니다. 다시 시도해주세요.');
  }
  return json.data as CreateReservationResult;
}

export async function getPaymentStatus(reservationId: string): Promise<PaymentStatus | null> {
  try {
    const res = await fetch(`/api/v1/payments/by-reservation/${reservationId}`);
    if (!res.ok) return null;
    const json = await res.json();
    if (!json.success) return null;
    return json.data as PaymentStatus;
  } catch {
    return null;
  }
}
