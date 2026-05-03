import { apiClient } from './client'

export type FulfillmentStatus =
  | 'DRAFT'
  | 'REQUESTED'
  | 'ACKNOWLEDGED'
  | 'PURCHASING'
  | 'PURCHASED'
  | 'SHIPPED_TO_AGENT'
  | 'COMPLETED'
  | 'OUT_OF_STOCK'
  | 'CANCELLED'
  | 'REJECTED'

export type PaymentStatus =
  | 'NOT_REQUIRED'
  | 'PENDING'
  | 'SUBMITTED'
  | 'CONFIRMED'
  | 'REJECTED'

export type StatusDimension = 'FULFILLMENT' | 'PAYMENT'

export type CurrencyCode = 'KRW' | 'THB' | 'USD'

export interface OrderListItem {
  id: number
  orderNo: string
  customerId: number
  customerName: string
  fulfillmentStatus: FulfillmentStatus
  paymentStatus: PaymentStatus
  itemCount: number
  totalsByCurrency: Partial<Record<CurrencyCode, string>>
  createdAt: string
}

export interface OrderItemDetail {
  id: number
  productName: string
  productUrl: string | null
  quantity: number
  categoryId: number | null
  attributes: Record<string, unknown> | null
  unitPriceAmount: string | null
  unitPriceCurrency: CurrencyCode | null
  requestImageKeys: string[]
  requestImageUrls: string[]
}

export interface OrderStatusLogEntry {
  id: number
  dimension: StatusDimension
  fromCode: string | null
  toCode: string
  changedBy: number | null
  note: string | null
  createdAt: string
}

export interface OrderDetail {
  id: number
  orderNo: string
  customerId: number
  customerName: string
  customerLineId: string | null
  customerPhone: string | null
  fulfillmentStatus: FulfillmentStatus
  paymentStatus: PaymentStatus
  allowedFulfillmentNext: FulfillmentStatus[]
  allowedPaymentNext: PaymentStatus[]
  customerMemo: string | null
  internalMemo: string | null
  koreanTrackingNo: string | null
  koreanCourier: string | null
  items: OrderItemDetail[]
  history: OrderStatusLogEntry[]
  createdAt: string
  updatedAt: string
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export interface CreateOrderInput {
  customerName: string
  customerLineId?: string
  customerPhone?: string
  customerMemo?: string
  internalMemo?: string
  koreanTrackingNo?: string
  koreanCourier?: string
  items: Array<{
    productName: string
    productUrl?: string
    quantity: number
    categoryId?: number
    attributes?: Record<string, unknown>
    unitPriceAmount?: string
    unitPriceCurrency?: CurrencyCode
    tempImageKeys?: string[]
  }>
}

export interface DashboardSummary {
  totalsByCurrency: Partial<Record<CurrencyCode, string>>
  totalOrderCount: number
  ordersByFulfillmentStatus: Partial<Record<FulfillmentStatus, number>>
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const { data } = await apiClient.get<DashboardSummary>('/intake/dashboard/summary')
  return data
}

export type OrderSortOption =
  | 'createdAt-desc'
  | 'createdAt-asc'
  | 'fulfillmentStatus-asc'
  | 'productName-asc'

export async function listOrders(params: {
  status?: FulfillmentStatus
  q?: string
  sort?: OrderSortOption
  page?: number
  size?: number
} = {}): Promise<PageResponse<OrderListItem>> {
  const { sort, ...rest } = params
  const sortField = sort?.split('-')[0]
  const sortDir = sort?.split('-')[1]
  const { data } = await apiClient.get<PageResponse<OrderListItem>>('/intake/orders', {
    params: { ...rest, sort: sortField, dir: sortDir },
  })
  return data
}

export async function getOrder(id: number): Promise<OrderDetail> {
  const { data } = await apiClient.get<OrderDetail>(`/intake/orders/${id}`)
  return data
}

export async function createOrder(input: CreateOrderInput): Promise<OrderDetail> {
  const { data } = await apiClient.post<OrderDetail>('/intake/orders', input)
  return data
}

export async function changeFulfillmentStatus(
  id: number,
  target: FulfillmentStatus,
  note?: string
): Promise<OrderDetail> {
  const { data } = await apiClient.patch<OrderDetail>(
    `/intake/orders/${id}/fulfillment-status`,
    { target, note }
  )
  return data
}

export async function deleteOrder(id: number): Promise<void> {
  await apiClient.delete(`/intake/orders/${id}`)
}

export async function changePaymentStatus(
  id: number,
  target: PaymentStatus,
  note?: string
): Promise<OrderDetail> {
  const { data } = await apiClient.patch<OrderDetail>(
    `/intake/orders/${id}/payment-status`,
    { target, note }
  )
  return data
}
