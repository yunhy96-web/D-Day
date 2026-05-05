import { z } from 'zod'

export const CURRENCY_OPTIONS = ['KRW', 'THB', 'USD'] as const
export type CurrencyCode = (typeof CURRENCY_OPTIONS)[number]

// Up to 12 digits with up to 2 decimals — matches NUMERIC(12,2) on the backend.
const priceRegex = /^\d{1,10}(\.\d{1,2})?$/

export const orderItemSchema = z.object({
  categoryId: z.coerce.number().int().positive({ message: 'msg.error.required' }),
  productName: z.string().trim().min(1, 'msg.error.required').max(255),
  productUrl: z.string().trim().max(2000).optional().or(z.literal('')),
  quantity: z.coerce.number().int().min(1, 'msg.error.required'),
  // attributes is a free-form bag for category-specific dynamic fields.
  // Schema-level validation lives on the backend (CategorySchemaValidator).
  attributes: z.record(z.string(), z.unknown()).optional(),
  // 단가/통화 모두 필수. 디파짓/매출 계산이 등록 시점부터 정확해야 하므로
  // 추후 채우는 흐름은 허용하지 않는다. 폼 초기값이 ''이라 enum 대신 string+refine 사용.
  unitPriceAmount: z
    .string()
    .trim()
    .min(1, 'msg.error.price_required')
    .refine((v) => priceRegex.test(v) && Number(v) >= 0.01, {
      message: 'msg.error.price_required',
    }),
  unitPriceCurrency: z
    .string()
    .min(1, 'msg.error.currency_required')
    .refine((v) => (CURRENCY_OPTIONS as readonly string[]).includes(v), {
      message: 'msg.error.currency_required',
    }),
})

export const ORDER_TYPES = ['INDIVIDUAL', 'SET'] as const
export type OrderTypeCode = (typeof ORDER_TYPES)[number]

export const createOrderSchema = z
  .object({
    customerName: z.string().trim().min(1, 'msg.error.required').max(64),
    customerLineId: z.string().trim().max(500).optional().or(z.literal('')),
    customerPhone: z.string().trim().max(32).optional().or(z.literal('')),
    orderType: z.enum(ORDER_TYPES).default('INDIVIDUAL'),
    customerMemo: z.string().max(2000).optional().or(z.literal('')),
    internalMemo: z.string().max(2000).optional().or(z.literal('')),
    koreanTrackingNo: z.string().trim().max(500).optional().or(z.literal('')),
    koreanCourier: z.string().trim().max(32).optional().or(z.literal('')),
    recipientName: z.string().trim().max(64).optional().or(z.literal('')),
    recipientPhone: z.string().trim().max(32).optional().or(z.literal('')),
    postalCode: z.string().trim().max(16).optional().or(z.literal('')),
    addressLine: z.string().trim().max(1000).optional().or(z.literal('')),
    country: z.string().trim().max(2).optional().or(z.literal('')),
    shippingAddressLabel: z.string().trim().max(64).optional().or(z.literal('')),
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
