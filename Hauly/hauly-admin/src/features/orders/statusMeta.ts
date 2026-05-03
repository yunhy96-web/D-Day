import type { FulfillmentStatus, PaymentStatus } from '@/lib/api/orders'

type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger'

export const FULFILLMENT_TONE: Record<FulfillmentStatus, Tone> = {
  DRAFT: 'neutral',
  REQUESTED: 'info',
  ACKNOWLEDGED: 'info',
  PURCHASING: 'warning',
  PURCHASED: 'warning',
  SHIPPED_TO_AGENT: 'warning',
  COMPLETED: 'success',
  OUT_OF_STOCK: 'danger',
  CANCELLED: 'neutral',
  REJECTED: 'danger',
}

export const PAYMENT_TONE: Record<PaymentStatus, Tone> = {
  NOT_REQUIRED: 'neutral',
  PENDING: 'warning',
  SUBMITTED: 'info',
  CONFIRMED: 'success',
  REJECTED: 'danger',
}

