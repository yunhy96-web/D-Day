import { z } from 'zod'

export const CURRENCY_OPTIONS = ['KRW', 'THB', 'USD'] as const
export type CurrencyCode = (typeof CURRENCY_OPTIONS)[number]

// Up to 12 digits with up to 2 decimals — matches NUMERIC(12,2) on the backend.
const priceRegex = /^\d{1,10}(\.\d{1,2})?$/

export const orderItemSchema = z
  .object({
    categoryId: z.coerce.number().int().positive({ message: 'msg.error.required' }),
    productName: z.string().trim().min(1, 'msg.error.required').max(255),
    productUrl: z.string().trim().max(2000).optional().or(z.literal('')),
    quantity: z.coerce.number().int().min(1, 'msg.error.required'),
    // attributes is a free-form bag for category-specific dynamic fields.
    // Schema-level validation lives on the backend (CategorySchemaValidator).
    attributes: z.record(z.string(), z.unknown()).optional(),
    unitPriceAmount: z
      .string()
      .trim()
      .optional()
      .or(z.literal(''))
      .refine((v) => !v || priceRegex.test(v), { message: 'msg.error.required' }),
    unitPriceCurrency: z.enum(CURRENCY_OPTIONS).optional().or(z.literal('')),
  })
  .superRefine((value, ctx) => {
    const hasAmount = !!value.unitPriceAmount
    const hasCurrency = !!value.unitPriceCurrency
    if (hasAmount !== hasCurrency) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: [hasAmount ? 'unitPriceCurrency' : 'unitPriceAmount'],
        message: 'msg.error.required',
      })
    }
  })

export const ORDER_TYPES = ['INDIVIDUAL', 'SET'] as const
export type OrderTypeCode = (typeof ORDER_TYPES)[number]

export const createOrderSchema = z
  .object({
    customerName: z.string().trim().min(1, 'msg.error.required').max(64),
    customerLineId: z.string().trim().max(64).optional().or(z.literal('')),
    customerPhone: z.string().trim().max(32).optional().or(z.literal('')),
    orderType: z.enum(ORDER_TYPES).default('INDIVIDUAL'),
    customerMemo: z.string().max(2000).optional().or(z.literal('')),
    internalMemo: z.string().max(2000).optional().or(z.literal('')),
    koreanTrackingNo: z.string().trim().max(64).optional().or(z.literal('')),
    koreanCourier: z.string().trim().max(32).optional().or(z.literal('')),
    recipientName: z.string().trim().max(64).optional().or(z.literal('')),
    recipientPhone: z.string().trim().max(32).optional().or(z.literal('')),
    postalCode: z.string().trim().max(16).optional().or(z.literal('')),
    addressLine: z.string().trim().max(1000).optional().or(z.literal('')),
    country: z.string().trim().max(2).optional().or(z.literal('')),
    items: z.array(orderItemSchema).min(1, 'msg.error.at_least_one_item'),
  })
  .superRefine((value, ctx) => {
    // SET orders ship as one bundle, so all items must share a category.
    // Skipped for single-item sets — nothing to compare.
    if (value.orderType !== 'SET' || value.items.length <= 1) return
    const first = value.items[0]?.categoryId
    value.items.forEach((it, idx) => {
      if (it.categoryId !== first) {
        ctx.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['items', idx, 'categoryId'],
          message: 'msg.error.set_category_mismatch',
        })
      }
    })
  })

export type CreateOrderFormValues = z.infer<typeof createOrderSchema>
