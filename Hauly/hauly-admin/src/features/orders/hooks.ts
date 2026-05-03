import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  changeFulfillmentStatus,
  changePaymentStatus,
  createOrder,
  deleteOrder,
  getOrder,
  listOrders,
  type CreateOrderInput,
  type FulfillmentStatus,
  type OrderSortOption,
  type PaymentStatus,
} from '@/lib/api/orders'

const ORDERS_KEY = 'orders' as const

export function useOrders(params: {
  status?: FulfillmentStatus
  q?: string
  sort?: OrderSortOption
  page?: number
  size?: number
} = {}) {
  return useQuery({
    queryKey: [ORDERS_KEY, 'list', params],
    queryFn: () => listOrders(params),
    placeholderData: (prev) => prev, // keep showing the previous page while typing in search
  })
}

export function useOrder(id: number | null) {
  return useQuery({
    queryKey: [ORDERS_KEY, 'detail', id],
    queryFn: () => getOrder(id!),
    enabled: id != null,
  })
}

export function useCreateOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateOrderInput) => createOrder(input),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: [ORDERS_KEY, 'list'] })
    },
  })
}

export function useDeleteOrder() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteOrder(id),
    onSuccess: (_data, id) => {
      qc.removeQueries({ queryKey: [ORDERS_KEY, 'detail', id] })
      qc.invalidateQueries({ queryKey: [ORDERS_KEY, 'list'] })
      qc.invalidateQueries({ queryKey: ['dashboard', 'summary'] })
    },
  })
}

export function useChangeFulfillmentStatus(orderId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ target, note }: { target: FulfillmentStatus; note?: string }) =>
      changeFulfillmentStatus(orderId, target, note),
    onSuccess: (data) => {
      qc.setQueryData([ORDERS_KEY, 'detail', orderId], data)
      qc.invalidateQueries({ queryKey: [ORDERS_KEY, 'list'] })
    },
  })
}

export function useChangePaymentStatus(orderId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ target, note }: { target: PaymentStatus; note?: string }) =>
      changePaymentStatus(orderId, target, note),
    onSuccess: (data) => {
      qc.setQueryData([ORDERS_KEY, 'detail', orderId], data)
      qc.invalidateQueries({ queryKey: [ORDERS_KEY, 'list'] })
    },
  })
}
