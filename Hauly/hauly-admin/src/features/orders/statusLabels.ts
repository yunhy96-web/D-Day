import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { fetchCommonCodes, type CommonCodeView } from '@/lib/api/commonCodes'
import type { FulfillmentStatus, PaymentStatus } from '@/lib/api/orders'
import { FULFILLMENT_TONE, PAYMENT_TONE } from './statusMeta'

/**
 * Fetches the FULFILLMENT_STATUS / PAYMENT_STATUS labels from the backend
 * common_code table. Cached per language — re-fetched when the user switches
 * UI language (the apiClient interceptor sets Accept-Language and the query
 * key includes the language so React Query revalidates automatically).
 */
export function useFulfillmentLabels() {
  const { i18n } = useTranslation()
  const lang = i18n.resolvedLanguage ?? 'ko'
  return useQuery({
    queryKey: ['common-codes', 'FULFILLMENT_STATUS', lang],
    queryFn: () => fetchCommonCodes('FULFILLMENT_STATUS'),
    staleTime: 5 * 60 * 1000,
  })
}

export function usePaymentLabels() {
  const { i18n } = useTranslation()
  const lang = i18n.resolvedLanguage ?? 'ko'
  return useQuery({
    queryKey: ['common-codes', 'PAYMENT_STATUS', lang],
    queryFn: () => fetchCommonCodes('PAYMENT_STATUS'),
    staleTime: 5 * 60 * 1000,
  })
}

export function findLabel(codes: CommonCodeView[] | undefined, code: string): string {
  return codes?.find((c) => c.code === code)?.name ?? code
}

export function fulfillmentTone(code: FulfillmentStatus) {
  return FULFILLMENT_TONE[code]
}

export function paymentTone(code: PaymentStatus) {
  return PAYMENT_TONE[code]
}
