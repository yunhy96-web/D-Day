import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  createShippingTemplate,
  deleteShippingTemplate,
  listShippingTemplates,
  type CreateShippingTemplateInput,
} from '@/lib/api/shippingTemplates'

const KEY = 'shipping-templates' as const

export function useShippingTemplates() {
  return useQuery({
    queryKey: [KEY],
    queryFn: listShippingTemplates,
  })
}

export function useCreateShippingTemplate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateShippingTemplateInput) => createShippingTemplate(input),
    onSuccess: () => qc.invalidateQueries({ queryKey: [KEY] }),
  })
}

export function useDeleteShippingTemplate() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => deleteShippingTemplate(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: [KEY] }),
  })
}
