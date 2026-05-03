import { apiClient } from './client'

/** A single field definition parsed from product_category.field_schema. */
export interface FieldDefinition {
  key: string
  labelKey: string | null
  /** "text" | "decimal" | "select" | "group" */
  type: string
  required: boolean
  optionsCode: string | null
  min: number | null
  max: number | null
  step: number | null
  /** Nested fields for type=group (e.g. left_eye.power) */
  fields: FieldDefinition[] | null
}

export interface CategoryView {
  id: number
  code: string
  /** i18n key (e.g. "category.contact_lens.name") — translate via t() */
  nameKey: string
  sortOrder: number
  active: boolean
  fields: FieldDefinition[]
}

export async function fetchCategories(): Promise<CategoryView[]> {
  const { data } = await apiClient.get<CategoryView[]>('/intake/categories')
  return data
}
