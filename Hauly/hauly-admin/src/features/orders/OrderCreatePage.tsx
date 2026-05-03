import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useFieldArray, useForm, useWatch, type Control } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useTranslation } from 'react-i18next'
import { Trash2, Plus } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Select } from '@/components/ui/select'
import { Alert, AlertDescription } from '@/components/ui/alert'

import { useCreateOrder } from './hooks'
import { useCategories } from './categoryHooks'
import { useCommonCodeGroup } from './commonCodeHooks'
import { DynamicFields } from './DynamicFields'
import { ImageUploader, type UploadedImage } from './ImageUploader'
import { NumberInput } from '@/components/ui/NumberInput'
import { createOrderSchema, type CreateOrderFormValues, type CurrencyCode } from './schema'
import type { CategoryView } from '@/lib/api/categories'
import { ApiError } from '@/lib/api/types'
import { COUNTRY_CODES, countryDisplayName } from './countries'

export default function OrderCreatePage() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const mutation = useCreateOrder()
  const { data: categories } = useCategories()
  const { data: currencyCodes } = useCommonCodeGroup('CURRENCY')
  const { data: courierCodes } = useCommonCodeGroup('COURIER_KR')

  // GENERAL is sorted first (sort_order=10) — use it as the default selection
  // so the form is valid as soon as it loads. The user can switch to lens / cosmetics.
  const defaultCategoryId = categories?.[0]?.id ?? 0

  const {
    register,
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateOrderFormValues>({
    resolver: zodResolver(createOrderSchema),
    defaultValues: {
      customerName: '',
      customerLineId: '',
      customerPhone: '',
      orderType: 'INDIVIDUAL',
      customerMemo: '',
      internalMemo: '',
      koreanTrackingNo: '',
      koreanCourier: '',
      recipientName: '',
      recipientPhone: '',
      postalCode: '',
      addressLine: '',
      country: 'TH',
      items: [{
        categoryId: 0,
        productName: '',
        productUrl: '',
        quantity: 1,
        attributes: {},
        unitPriceAmount: '',
        unitPriceCurrency: '',
      }],
    },
  })

  const orderType = useWatch({ control, name: 'orderType' })
  const isSet = orderType === 'SET'
  const firstItemCategory = useWatch({ control, name: 'items.0.categoryId' })

  // Once categories load, fold the default category id into the (still-empty) form
  // so the user doesn't have to pick before submitting.
  useEffect(() => {
    if (defaultCategoryId) {
      reset((current) => ({
        ...current,
        items: current.items.map((i) => ({
          ...i,
          categoryId: i.categoryId || defaultCategoryId,
        })),
      }))
    }
  }, [defaultCategoryId, reset])

  const { fields, append, remove } = useFieldArray({ control, name: 'items' })

  // Tracked outside react-hook-form because uploads are transient (object URLs, server temp keys)
  // and don't participate in zod validation. Keyed by the rhf field id so it survives re-renders.
  const [imagesByItemId, setImagesByItemId] = useState<Record<string, UploadedImage[]>>({})

  function removeItem(index: number) {
    const id = fields[index].id
    const imgs = imagesByItemId[id]
    if (imgs) imgs.forEach((img) => URL.revokeObjectURL(img.previewUrl))
    setImagesByItemId((prev) => {
      const { [id]: _drop, ...rest } = prev
      return rest
    })
    remove(index)
  }

  function onSubmit(values: CreateOrderFormValues) {
    mutation.mutate(
      {
        customerName: values.customerName,
        customerLineId: values.customerLineId || undefined,
        customerPhone: values.customerPhone || undefined,
        orderType: values.orderType,
        customerMemo: values.customerMemo || undefined,
        internalMemo: values.internalMemo || undefined,
        koreanTrackingNo: values.koreanTrackingNo || undefined,
        koreanCourier: values.koreanCourier || undefined,
        recipientName: values.recipientName || undefined,
        recipientPhone: values.recipientPhone || undefined,
        postalCode: values.postalCode || undefined,
        addressLine: values.addressLine || undefined,
        country: values.country || undefined,
        items: values.items.map((i, idx) => {
          const tempImageKeys = (imagesByItemId[fields[idx].id] ?? []).map((img) => img.tempKey)
          return {
            productName: i.productName,
            productUrl: i.productUrl || undefined,
            quantity: i.quantity,
            categoryId: i.categoryId,
            attributes: i.attributes && Object.keys(i.attributes).length > 0
              ? pruneEmpty(i.attributes)
              : undefined,
            unitPriceAmount: i.unitPriceAmount || undefined,
            unitPriceCurrency: (i.unitPriceCurrency || undefined) as CurrencyCode | undefined,
            tempImageKeys: tempImageKeys.length > 0 ? tempImageKeys : undefined,
          }
        }),
      },
      {
        onSuccess: (created) => navigate(`/orders/${created.id}`, { replace: true }),
      }
    )
  }

  function getErrorMessage(): string | null {
    const err = mutation.error
    if (!err) return null
    if (err instanceof ApiError) {
      const key = `msg.error.${err.code}`
      const translated = t(key)
      return translated !== key ? translated : err.message
    }
    return t('msg.error.network')
  }

  const errorMsg = getErrorMessage()

  return (
    <div className="p-4 md:p-6 max-w-3xl mx-auto space-y-4">
      <h1 className="text-xl md:text-2xl font-semibold">{t('order.create.title')}</h1>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        {errorMsg && (
          <Alert variant="destructive">
            <AlertDescription>{errorMsg}</AlertDescription>
          </Alert>
        )}

        <Card>
          <CardHeader>
            <CardTitle className="text-base">{t('order.create.customer')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="customerName">{t('field.customer_name.label')}*</Label>
              <Input id="customerName" {...register('customerName')} />
              {errors.customerName && (
                <p className="text-xs text-destructive">{t(errors.customerName.message ?? '')}</p>
              )}
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="customerLineId">{t('field.line_id.label')}</Label>
                <Input
                  id="customerLineId"
                  placeholder={t('field.line_id.placeholder')}
                  {...register('customerLineId')}
                />
                {errors.customerLineId && (
                  <p className="text-xs text-destructive">{t(errors.customerLineId.message ?? '')}</p>
                )}
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="customerPhone">{t('field.phone.label')}</Label>
                <Input id="customerPhone" {...register('customerPhone')} placeholder="+66..." />
              </div>
            </div>
            <p className="text-xs text-muted-foreground">{t('order.create.matching_hint')}</p>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="py-4 space-y-2">
            <div className="text-sm font-medium">{t('order.create.order_type')}</div>
            <div className="flex gap-4">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  value="INDIVIDUAL"
                  className="cursor-pointer"
                  {...register('orderType')}
                />
                <span className="text-sm">{t('order.type.individual')}</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  value="SET"
                  className="cursor-pointer"
                  {...register('orderType')}
                />
                <span className="text-sm">{t('order.type.set')}</span>
              </label>
            </div>
            {isSet && (
              <p className="text-xs text-muted-foreground">
                {t('order.type.set_help')}
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle className="text-base">
              {isSet ? t('order.set_items.title') : t('order.create.items')}
            </CardTitle>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() =>
                append({
                  categoryId: isSet && firstItemCategory
                    ? firstItemCategory
                    : defaultCategoryId,
                  productName: '',
                  productUrl: '',
                  quantity: 1,
                  attributes: {},
                  unitPriceAmount: '',
                  unitPriceCurrency: '',
                })
              }
            >
              <Plus className="h-4 w-4 mr-1" /> {t('order.create.add_item')}
            </Button>
          </CardHeader>
          <CardContent className="space-y-4">
            {fields.map((field, index) => (
              <div key={field.id} className="border rounded-md p-3 space-y-3 bg-background">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-muted-foreground">#{index + 1}</span>
                  {fields.length > 1 && (
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => removeItem(index)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  )}
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="space-y-1.5">
                    <Label>{t('field.category.label')}*</Label>
                    <Select {...register(`items.${index}.categoryId` as const)}>
                      {categories?.map((c) => (
                        <option key={c.id} value={c.id}>
                          {t(c.nameKey)}
                        </option>
                      ))}
                    </Select>
                    {errors.items?.[index]?.categoryId && (
                      <p className="text-xs text-destructive">
                        {t(errors.items[index]?.categoryId?.message ?? '')}
                      </p>
                    )}
                  </div>
                  <div className="col-span-2 space-y-1.5">
                    <Label>{t('field.product_name.label')}*</Label>
                    <Input {...register(`items.${index}.productName` as const)} />
                    {errors.items?.[index]?.productName && (
                      <p className="text-xs text-destructive">
                        {t(errors.items[index]?.productName?.message ?? '')}
                      </p>
                    )}
                  </div>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="col-span-2 space-y-1.5">
                    <Label>{t('field.product_url.label')}</Label>
                    <Input {...register(`items.${index}.productUrl` as const)} />
                  </div>
                  <div className="space-y-1.5">
                    <Label>{t('field.quantity.label')}*</Label>
                    <NumberInput
                      name={`items.${index}.quantity` as const}
                      control={control}
                      min={1}
                    />
                  </div>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div className="col-span-2 space-y-1.5">
                    <Label>{t('field.unit_price.label')}</Label>
                    <NumberInput
                      name={`items.${index}.unitPriceAmount` as const}
                      control={control}
                      decimals={2}
                      placeholder="0"
                    />
                    {errors.items?.[index]?.unitPriceAmount && (
                      <p className="text-xs text-destructive">
                        {t(errors.items[index]?.unitPriceAmount?.message ?? '')}
                      </p>
                    )}
                  </div>
                  <div className="space-y-1.5">
                    <Label>{t('field.currency.label')}</Label>
                    <Select {...register(`items.${index}.unitPriceCurrency` as const)}>
                      <option value="">—</option>
                      {currencyCodes?.map((c) => (
                        <option key={c.code} value={c.code}>
                          {c.name}
                        </option>
                      ))}
                    </Select>
                    {errors.items?.[index]?.unitPriceCurrency && (
                      <p className="text-xs text-destructive">
                        {t(errors.items[index]?.unitPriceCurrency?.message ?? '')}
                      </p>
                    )}
                  </div>
                </div>
                <ItemDynamicFields
                  control={control}
                  register={register}
                  index={index}
                  categories={categories}
                />
                <div className="space-y-1.5">
                  <Label>{t('field.images.label')}</Label>
                  <ImageUploader
                    value={imagesByItemId[field.id] ?? []}
                    onChange={(next) =>
                      setImagesByItemId((prev) => ({ ...prev, [field.id]: next }))
                    }
                  />
                </div>
              </div>
            ))}
            {errors.items?.root && (
              <p className="text-xs text-destructive">{t(errors.items.root.message ?? '')}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">{t('order.create.shipping_address')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="recipientName">{t('field.recipient_name.label')}</Label>
                <Input id="recipientName" {...register('recipientName')} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="recipientPhone">{t('field.recipient_phone.label')}</Label>
                <Input id="recipientPhone" {...register('recipientPhone')} />
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="postalCode">{t('field.postal_code.label')}</Label>
                <Input id="postalCode" {...register('postalCode')} />
              </div>
              <div className="space-y-1.5 sm:col-span-2">
                <Label htmlFor="country">{t('field.country.label')}</Label>
                <Select id="country" {...register('country')}>
                  <option value="">—</option>
                  {COUNTRY_CODES.map((code) => (
                    <option key={code} value={code}>
                      {countryDisplayName(code, i18n.resolvedLanguage)}
                    </option>
                  ))}
                </Select>
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="addressLine">{t('field.address_line.label')}</Label>
              <Textarea id="addressLine" rows={3} {...register('addressLine')} />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">{t('order.create.tracking')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="koreanTrackingNo">{t('field.tracking_no.label')}</Label>
                <Input id="koreanTrackingNo" {...register('koreanTrackingNo')} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="koreanCourier">{t('field.courier.label')}</Label>
                <Select id="koreanCourier" {...register('koreanCourier')}>
                  <option value="">—</option>
                  {courierCodes?.map((c) => (
                    <option key={c.code} value={c.code}>
                      {c.name}
                    </option>
                  ))}
                </Select>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">{t('order.create.memo')}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="customerMemo">{t('field.customer_memo.label')}</Label>
              <Textarea id="customerMemo" rows={2} {...register('customerMemo')} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="internalMemo">{t('field.internal_memo.label')}</Label>
              <Textarea id="internalMemo" rows={2} {...register('internalMemo')} />
            </div>
          </CardContent>
        </Card>

        <div className="flex gap-2">
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? t('btn.saving') : t('btn.save')}
          </Button>
          <Button type="button" variant="outline" onClick={() => navigate(-1)}>
            {t('btn.cancel')}
          </Button>
        </div>
      </form>
    </div>
  )
}

/**
 * Watches the row's selected categoryId and renders the matching field schema.
 * Extracted into its own component so the parent doesn't re-render on every keystroke.
 */
function ItemDynamicFields({
  control,
  register,
  index,
  categories,
}: {
  control: Control<CreateOrderFormValues>
  register: ReturnType<typeof useForm<CreateOrderFormValues>>['register']
  index: number
  categories: CategoryView[] | undefined
}) {
  const categoryId = useWatch({ control, name: `items.${index}.categoryId` })
  const cat = categories?.find((c) => Number(c.id) === Number(categoryId))
  if (!cat || !cat.fields || cat.fields.length === 0) return null
  return (
    <DynamicFields
      fields={cat.fields}
      register={register as any}
      control={control as any}
      pathPrefix={`items.${index}.attributes`}
    />
  )
}

/** Drop blank/undefined values from a nested attributes object before sending to the backend. */
function pruneEmpty(obj: Record<string, unknown>): Record<string, unknown> {
  const result: Record<string, unknown> = {}
  for (const [k, v] of Object.entries(obj)) {
    if (v == null || v === '') continue
    if (typeof v === 'object' && !Array.isArray(v)) {
      const pruned = pruneEmpty(v as Record<string, unknown>)
      if (Object.keys(pruned).length > 0) result[k] = pruned
    } else {
      result[k] = v
    }
  }
  return result
}
