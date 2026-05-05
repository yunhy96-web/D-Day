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

export type OrderType = 'INDIVIDUAL' | 'SET'

export interface OrderListItem {
  id: number
  orderNo: string
  customerId: number
  customerName: string
  orderType: OrderType
  fulfillmentStatus: FulfillmentStatus
  paymentStatus: PaymentStatus
  itemCount: number
  firstProductName: string | null
  firstImageUrl: string | null
  koreanCourier: string | null
  koreanTrackingNo: string | null
  shippingAddressLabel: string | null
  paidAmountKrw: string | null
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
  forced: boolean
  createdAt: string
}

export interface OrderDetail {
  id: number
  orderNo: string
  customerId: number
  customerName: string
  customerLineId: string | null
  customerPhone: string | null
  orderType: OrderType
  fulfillmentStatus: FulfillmentStatus
  paymentStatus: PaymentStatus
  allowedFulfillmentNext: FulfillmentStatus[]
  allowedPaymentNext: PaymentStatus[]
  customerMemo: string | null
  internalMemo: string | null
  koreanTrackingNo: string | null
  koreanCourier: string | null
  paidAmountKrw: string | null
  purchaseProofKeys: string[]
  purchaseProofUrls: string[]
  recipientName: string | null
  recipientPhone: string | null
  postalCode: string | null
  addressLine: string | null
  country: string | null
  shippingAddressLabel: string | null
  customerRevenueAmount: string | null
  customerRevenueCurrency: 'KRW' | 'THB' | null
  logisticsKrToThAmount: string | null
  logisticsKrToThCurrency: 'KRW' | 'THB' | null
  logisticsThDomesticAmount: string | null
  logisticsThDomesticCurrency: 'KRW' | 'THB' | null
  krwPerThb: string | null
  netProfitKrw: string | null
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
  orderType?: OrderType
  customerMemo?: string
  internalMemo?: string
  koreanTrackingNo?: string
  koreanCourier?: string
  recipientName?: string
  recipientPhone?: string
  postalCode?: string
  addressLine?: string
  country?: string
  shippingAddressLabel?: string
  items: Array<{
    productName: string
    productUrl?: string
    quantity: number
    categoryId?: number
    attributes?: Record<string, unknown>
    unitPriceAmount: string
    unitPriceCurrency: CurrencyCode
    tempImageKeys?: string[]
  }>
}

export interface DashboardSummary {
  totalsByCurrency: Partial<Record<CurrencyCode, string>>
  totalOrderCount: number
  ordersByFulfillmentStatus: Partial<Record<FulfillmentStatus, number>>
  /** Negative = customer owes us. */
  depositBalanceKrw: string
  /** 재무 입력이 완료된 주문 합계 순수익 (KRW). */
  totalNetProfitKrw: string
}

export interface UpdateFinancialsInput {
  customerRevenueAmount?: string | null
  customerRevenueCurrency?: 'KRW' | 'THB' | null
  logisticsKrToThAmount?: string | null
  logisticsKrToThCurrency?: 'KRW' | 'THB' | null
  logisticsThDomesticAmount?: string | null
  logisticsThDomesticCurrency?: 'KRW' | 'THB' | null
  krwPerThb?: string | null
}

export async function updateFinancials(
  id: number,
  input: UpdateFinancialsInput,
): Promise<OrderDetail> {
  const { data } = await apiClient.patch<OrderDetail>(
    `/intake/orders/${id}/financials`,
    input,
  )
  return data
}

export async function updatePaidAmount(
  id: number,
  paidAmountKrw: string,
): Promise<OrderDetail> {
  const { data } = await apiClient.patch<OrderDetail>(
    `/intake/orders/${id}/paid-amount`,
    { paidAmountKrw },
  )
  return data
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
  note?: string,
  paidAmountKrw?: string,
  proofTempKeys?: string[]
): Promise<OrderDetail> {
  const { data } = await apiClient.patch<OrderDetail>(
    `/intake/orders/${id}/fulfillment-status`,
    { target, note, paidAmountKrw, proofTempKeys }
  )
  return data
}

export async function updateTracking(
  id: number,
  koreanCourier: string | null,
  koreanTrackingNo: string | null
): Promise<OrderDetail> {
  const { data } = await apiClient.patch<OrderDetail>(
    `/intake/orders/${id}/tracking`,
    { koreanCourier, koreanTrackingNo }
  )
  return data
}

export async function addPurchaseProofs(
  id: number,
  proofTempKeys: string[]
): Promise<OrderDetail> {
  const { data } = await apiClient.post<OrderDetail>(
    `/intake/orders/${id}/proof`,
    { proofTempKeys }
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

export async function forceFulfillmentStatus(
  id: number,
  target: FulfillmentStatus,
  reason: string
): Promise<OrderDetail> {
  const { data } = await apiClient.patch<OrderDetail>(
    `/intake/orders/${id}/fulfillment-status/force`,
    { target, reason }
  )
  return data
}

export async function forcePaymentStatus(
  id: number,
  target: PaymentStatus,
  reason: string
): Promise<OrderDetail> {
  const { data } = await apiClient.patch<OrderDetail>(
    `/intake/orders/${id}/payment-status/force`,
    { target, reason }
  )
  return data
}

export interface OrderNote {
  id: number
  authorId: number
  authorName: string
  body: string
  createdAt: string
  updatedAt: string
  edited: boolean
}

export async function listOrderNotes(orderId: number): Promise<OrderNote[]> {
  const { data } = await apiClient.get<OrderNote[]>(`/intake/orders/${orderId}/notes`)
  return data
}

export async function createOrderNote(orderId: number, body: string): Promise<OrderNote> {
  const { data } = await apiClient.post<OrderNote>(
    `/intake/orders/${orderId}/notes`,
    { body }
  )
  return data
}

export async function updateOrderNote(
  orderId: number,
  noteId: number,
  body: string
): Promise<OrderNote> {
  const { data } = await apiClient.patch<OrderNote>(
    `/intake/orders/${orderId}/notes/${noteId}`,
    { body }
  )
  return data
}

export async function deleteOrderNote(orderId: number, noteId: number): Promise<void> {
  await apiClient.delete(`/intake/orders/${orderId}/notes/${noteId}`)
}
