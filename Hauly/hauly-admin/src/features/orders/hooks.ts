import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  changeFulfillmentStatus,
  changePaymentStatus,
  createOrder,
  createOrderNote,
  deleteOrder,
  deleteOrderNote,
  getOrder,
  listOrderNotes,
  listOrders,
  updateOrderNote,
  type CreateOrderInput,
  type FulfillmentStatus,
  type OrderSortOption,
  type PaymentStatus,
} from '@/lib/api/orders'

const ORDERS_KEY = 'orders' as const
const NOTES_KEY = 'order-notes' as const

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

export function useOrderNotes(orderId: number | null) {
  return useQuery({
    queryKey: [NOTES_KEY, orderId],
    queryFn: () => listOrderNotes(orderId!),
    enabled: orderId != null,
  })
}

export function useCreateOrderNote(orderId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: string) => createOrderNote(orderId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: [NOTES_KEY, orderId] }),
  })
}

export function useUpdateOrderNote(orderId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: ({ noteId, body }: { noteId: number; body: string }) =>
      updateOrderNote(orderId, noteId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: [NOTES_KEY, orderId] }),
  })
}

export function useDeleteOrderNote(orderId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (noteId: number) => deleteOrderNote(orderId, noteId),
    onSuccess: () => qc.invalidateQueries({ queryKey: [NOTES_KEY, orderId] }),
  })
}
