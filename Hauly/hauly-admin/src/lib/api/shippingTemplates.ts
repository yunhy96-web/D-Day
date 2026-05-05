import { apiClient } from './client'

export interface ShippingTemplate {
  id: number
  label: string
  recipientName: string | null
  recipientPhone: string | null
  postalCode: string | null
  addressLine: string | null
  country: string | null
}

export interface CreateShippingTemplateInput {
  label: string
  recipientName?: string
  recipientPhone?: string
  postalCode?: string
  addressLine?: string
  country?: string
}

export async function listShippingTemplates(): Promise<ShippingTemplate[]> {
  const { data } = await apiClient.get<ShippingTemplate[]>('/intake/shipping-templates')
  return data
}

export async function createShippingTemplate(
  input: CreateShippingTemplateInput
): Promise<ShippingTemplate> {
  const { data } = await apiClient.post<ShippingTemplate>('/intake/shipping-templates', input)
  return data
}

export async function deleteShippingTemplate(id: number): Promise<void> {
  await apiClient.delete(`/intake/shipping-templates/${id}`)
}
