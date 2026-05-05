import { apiClient } from './client'
import type { PageResponse } from './orders'

export type DepositTransactionKind = 'TOP_UP' | 'PURCHASE' | 'REFUND' | 'ADJUSTMENT'

export interface DepositSummary {
  balanceKrw: string
}

export interface DepositTransaction {
  id: number
  kind: DepositTransactionKind
  amountKrw: string
  relatedOrderId: number | null
  relatedOrderNo: string | null
  note: string | null
  createdBy: number | null
  createdByName: string | null
  createdAt: string
}

export async function getDepositSummary(): Promise<DepositSummary> {
  const { data } = await apiClient.get<DepositSummary>('/intake/deposits')
  return data
}

export async function listDepositTransactions(params: {
  page?: number
  size?: number
} = {}): Promise<PageResponse<DepositTransaction>> {
  const { data } = await apiClient.get<PageResponse<DepositTransaction>>(
    '/intake/deposits/transactions',
    { params },
  )
  return data
}

export async function recordDepositAdjustment(
  amountKrw: string,
  note: string,
): Promise<DepositTransaction> {
  const { data } = await apiClient.post<DepositTransaction>(
    '/intake/deposits/adjustments',
    { amountKrw, note },
  )
  return data
}
