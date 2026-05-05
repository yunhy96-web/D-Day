import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  getDepositSummary,
  listDepositTransactions,
  recordDepositAdjustment,
} from '@/lib/api/deposits'

const DEPOSITS_KEY = 'deposits' as const

export function useDepositSummary() {
  return useQuery({
    queryKey: [DEPOSITS_KEY, 'summary'],
    queryFn: getDepositSummary,
  })
}

export function useDepositTransactions(params: { page?: number; size?: number } = {}) {
  return useQuery({
    queryKey: [DEPOSITS_KEY, 'transactions', params],
    queryFn: () => listDepositTransactions(params),
  })
}

export function useRecordDepositAdjustment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ amountKrw, note }: { amountKrw: string; note: string }) =>
      recordDepositAdjustment(amountKrw, note),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [DEPOSITS_KEY] })
      qc.invalidateQueries({ queryKey: ['dashboard', 'summary'] })
    },
  })
}
