import { Fragment, useEffect, useState, type FormEvent } from 'react'
import { createPortal } from 'react-dom'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft, Trash2, X } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'

import {
  useAddPurchaseProofs,
  useChangeFulfillmentStatus,
  useUpdateFinancials,
  useUpdatePaidAmount,
  useUpdateTracking,
  useChangePaymentStatus,
  useDeleteOrder,
  useForceFulfillmentStatus,
  useForcePaymentStatus,
  useOrder,
} from './hooks'
import { useCategories } from './categoryHooks'
import { useCommonCodeGroup } from './commonCodeHooks'
import { formatMoney } from './money'
import { ImageGallery } from './ImageGallery'
import { ImageUploader, type UploadedImage } from './ImageUploader'
import { countryDisplayName } from './countries'
import { OrderNotesCard } from './OrderNotesCard'
import { useMe } from '@/features/auth/hooks'
import {
  findLabel,
  fulfillmentTone,
  paymentTone,
  useFulfillmentLabels,
  usePaymentLabels,
} from './statusLabels'
import type { FulfillmentStatus, PaymentStatus } from '@/lib/api/orders'
import { ApiError } from '@/lib/api/types'

const POST_PURCHASE_STATUSES = new Set<FulfillmentStatus>([
  'PURCHASED',
  'SHIPPED_TO_AGENT',
  'COMPLETED',
])

export default function OrderDetailPage() {
  const { id } = useParams<{ id: string }>()
  const orderId = id ? Number(id) : null
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()

  const { data: order, isLoading, isError } = useOrder(orderId)
  const { data: categories } = useCategories()
  const { data: courierCodes } = useCommonCodeGroup('COURIER_KR')
  const { data: me } = useMe()
  const deleteMutation = useDeleteOrder()
  const [trackingEditOpen, setTrackingEditOpen] = useState(false)
  const [addProofOpen, setAddProofOpen] = useState(false)
  const [financialsEditOpen, setFinancialsEditOpen] = useState(false)
  const [paidEditOpen, setPaidEditOpen] = useState(false)
  const categoryName = (id: number | null) =>
    id == null ? '-' : t(categories?.find((c) => c.id === id)?.nameKey ?? '-')
  const courierLabel = (code: string | null) =>
    !code ? '-' : courierCodes?.find((c) => c.code === code)?.name ?? code

  if (isLoading) {
    return <div className="p-6 text-sm text-muted-foreground">{t('msg.loading')}</div>
  }
  if (isError || !order) {
    return (
      <div className="p-6 space-y-3">
        <Alert variant="destructive">
          <AlertDescription>{t('msg.error.unexpected')}</AlertDescription>
        </Alert>
        <Button variant="outline" size="sm" onClick={() => navigate('/orders')}>
          <ArrowLeft className="h-4 w-4 mr-1" /> {t('menu.orders')}
        </Button>
      </div>
    )
  }

  return (
    <div className="p-4 md:p-6 max-w-4xl mx-auto space-y-4">
      <div className="flex items-center gap-2">
        <Button variant="ghost" size="sm" onClick={() => navigate('/orders')}>
          <ArrowLeft className="h-4 w-4 mr-1" /> {t('menu.orders')}
        </Button>
        <h1 className="text-lg md:text-2xl font-semibold ml-2 truncate">{order.orderNo}</h1>
        {order.orderType === 'SET' && (
          <span className="text-xs font-medium px-2 py-0.5 rounded bg-purple-100 text-purple-700 whitespace-nowrap">
            {t('badge.set_order')}
          </span>
        )}
        {me?.role === 'ADMIN' && (
          <Button
            variant="destructive"
            size="sm"
            className="ml-auto"
            disabled={deleteMutation.isPending}
            onClick={() => {
              const ok = window.confirm(
                t('order.delete.confirm', { orderNo: order.orderNo }),
              )
              if (!ok) return
              deleteMutation.mutate(order.id, {
                onSuccess: () => navigate('/orders', { replace: true }),
              })
            }}
          >
            <Trash2 className="h-4 w-4 mr-1" />
            {deleteMutation.isPending ? t('btn.deleting') : t('btn.delete')}
          </Button>
        )}
      </div>

      {deleteMutation.isError && (
        <Alert variant="destructive">
          <AlertDescription>
            {deleteMutation.error instanceof ApiError
              ? t('order.delete.error.with_code', { code: deleteMutation.error.code, message: deleteMutation.error.message })
              : t('order.delete.error.generic')}
          </AlertDescription>
        </Alert>
      )}

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        <StatusCard
          orderId={order.id}
          dimension="FULFILLMENT"
          current={order.fulfillmentStatus}
          allowed={order.allowedFulfillmentNext}
          isAdmin={me?.role === 'ADMIN'}
        />
        <StatusCard
          orderId={order.id}
          dimension="PAYMENT"
          current={order.paymentStatus}
          allowed={order.allowedPaymentNext}
          isAdmin={me?.role === 'ADMIN'}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('order.detail.customer')}</CardTitle>
        </CardHeader>
        <CardContent className="text-sm space-y-1">
          <KV label={t('field.customer_name.label')} value={order.customerName} />
          <KV
            label={t('field.line_id.label')}
            value={order.customerLineId ?? '-'}
            href={isUrl(order.customerLineId) ? order.customerLineId! : undefined}
          />
          <KV label={t('field.phone.label')} value={order.customerPhone ?? '-'} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">
            {order.orderType === 'SET' ? t('order.set_items.title') : t('order.detail.items')}
          </CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <table className="w-full text-sm min-w-[640px]">
            <thead className="text-xs uppercase text-muted-foreground border-b">
              <tr>
                <th className="text-left py-2 px-2">#</th>
                <th className="text-left py-2 px-2">{t('field.category.label')}</th>
                <th className="text-left py-2 px-2">{t('field.product_name.label')}</th>
                <th className="text-left py-2 px-2">{t('field.product_url.label')}</th>
                <th className="text-left py-2 px-2">{t('field.quantity.label')}</th>
                <th className="text-left py-2 px-2">{t('field.unit_price.label')}</th>
              </tr>
            </thead>
            <tbody>
              {order.items.map((item, idx) => (
                <Fragment key={item.id}>
                  <tr className="border-b">
                    <td className="py-2 px-2 text-muted-foreground">{idx + 1}</td>
                    <td className="py-2 px-2">{categoryName(item.categoryId)}</td>
                    <td className="py-2 px-2">{item.productName}</td>
                    <td className="py-2 px-2">
                      {item.productUrl ? (
                        <a
                          href={item.productUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="text-primary hover:underline truncate inline-block max-w-xs"
                        >
                          {item.productUrl}
                        </a>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td className="py-2 px-2">{item.quantity}</td>
                    <td className="py-2 px-2 whitespace-nowrap">
                      {item.unitPriceAmount && item.unitPriceCurrency
                        ? formatMoney(item.unitPriceAmount, item.unitPriceCurrency, i18n.resolvedLanguage)
                        : '-'}
                    </td>
                  </tr>
                  {item.attributes && Object.keys(item.attributes).length > 0 && (
                    <tr className="border-b bg-muted/30">
                      <td colSpan={6} className="py-2 px-2 text-xs">
                        <AttributesView attrs={item.attributes} />
                      </td>
                    </tr>
                  )}
                  {item.requestImageUrls && item.requestImageUrls.length > 0 && (
                    <tr className="border-b">
                      <td colSpan={6} className="py-2 px-2">
                        <ImageGallery urls={item.requestImageUrls} />
                      </td>
                    </tr>
                  )}
                </Fragment>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>

      {(order.shippingAddressLabel || order.recipientName || order.recipientPhone || order.postalCode || order.addressLine || order.country) && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">{t('order.detail.shipping_address')}</CardTitle>
          </CardHeader>
          <CardContent className="text-sm space-y-1">
            {order.shippingAddressLabel && (
              <KV label={t('field.shipping_label.label')} value={order.shippingAddressLabel} />
            )}
            {order.recipientName && (
              <KV label={t('field.recipient_name.label')} value={order.recipientName} />
            )}
            {order.recipientPhone && (
              <KV label={t('field.recipient_phone.label')} value={order.recipientPhone} />
            )}
            {order.postalCode && (
              <KV label={t('field.postal_code.label')} value={order.postalCode} />
            )}
            {order.country && (
              <KV
                label={t('field.country.label')}
                value={countryDisplayName(order.country, i18n.resolvedLanguage)}
              />
            )}
            {order.addressLine && (
              <div className="flex gap-2">
                <span className="w-24 text-muted-foreground shrink-0">
                  {t('field.address_line.label')}
                </span>
                <span className="whitespace-pre-wrap">{order.addressLine}</span>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-2 space-y-0">
          <CardTitle className="text-base">{t('order.detail.tracking')}</CardTitle>
          {me?.role === 'ADMIN' && (
            <Button variant="outline" size="sm" onClick={() => setTrackingEditOpen(true)}>
              {t('order.tracking.edit.button')}
            </Button>
          )}
        </CardHeader>
        <CardContent className="text-sm space-y-1">
          <KV label={t('field.tracking_no.label')} value={order.koreanTrackingNo ?? '-'} />
          <KV label={t('field.courier.label')} value={courierLabel(order.koreanCourier)} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between gap-2 space-y-0">
          <CardTitle className="text-base">{t('order.detail.financials.title')}</CardTitle>
          {me?.role === 'ADMIN' && (
            <Button variant="outline" size="sm" onClick={() => setFinancialsEditOpen(true)}>
              {t('order.detail.financials.edit')}
            </Button>
          )}
        </CardHeader>
        <CardContent className="text-sm space-y-1">
          {POST_PURCHASE_STATUSES.has(order.fulfillmentStatus) && (
            <div className="flex gap-2 items-center">
              <span className="w-24 text-muted-foreground">{t('field.paid_amount.label')}</span>
              <span className="flex-1">
                {order.paidAmountKrw
                  ? formatMoney(order.paidAmountKrw, 'KRW', i18n.resolvedLanguage)
                  : '—'}
              </span>
              {me?.role === 'ADMIN' && (
                <Button variant="outline" size="sm" onClick={() => setPaidEditOpen(true)}>
                  {order.paidAmountKrw
                    ? t('order.detail.paid_amount.edit')
                    : t('order.detail.paid_amount.input')}
                </Button>
              )}
            </div>
          )}
          <KV
            label={t('field.customer_revenue.label')}
            value={fmtAmtCur(order.customerRevenueAmount, order.customerRevenueCurrency, i18n.resolvedLanguage)}
          />
          <KV
            label={t('field.logistics_kr_th.label')}
            value={fmtAmtCur(order.logisticsKrToThAmount, order.logisticsKrToThCurrency, i18n.resolvedLanguage)}
          />
          <KV
            label={t('field.logistics_th_dom.label')}
            value={fmtAmtCur(order.logisticsThDomesticAmount, order.logisticsThDomesticCurrency, i18n.resolvedLanguage)}
          />
          <KV
            label={t('field.krw_per_thb.label')}
            value={order.krwPerThb ? Number(order.krwPerThb).toLocaleString(i18n.resolvedLanguage) : '—'}
          />
          <div className="pt-2 mt-2 border-t flex gap-2">
            <span className="w-24 text-muted-foreground">{t('order.detail.profit.label')}</span>
            <span className={
              order.netProfitKrw == null
                ? 'text-muted-foreground italic'
                : Number(order.netProfitKrw) >= 0
                  ? 'font-semibold text-emerald-700'
                  : 'font-semibold text-red-700'
            }>
              {order.netProfitKrw == null
                ? hasTHB(order) && !order.krwPerThb
                  ? t('order.detail.profit.missing_fx')
                  : t('order.detail.profit.incomplete')
                : (
                  <>
                    {formatMoney(order.netProfitKrw, 'KRW', i18n.resolvedLanguage)}
                    {order.krwPerThb && Number(order.krwPerThb) > 0 && (
                      <span className="ml-2 font-normal text-xs text-muted-foreground">
                        ≈ {formatMoney(Number(order.netProfitKrw) / Number(order.krwPerThb), 'THB', i18n.resolvedLanguage)}
                      </span>
                    )}
                  </>
                )}
            </span>
          </div>
        </CardContent>
      </Card>

      {(POST_PURCHASE_STATUSES.has(order.fulfillmentStatus) || order.purchaseProofUrls.length > 0) && (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between gap-2 space-y-0">
            <CardTitle className="text-base">{t('order.detail.proof.title')}</CardTitle>
            {me?.role === 'ADMIN' && POST_PURCHASE_STATUSES.has(order.fulfillmentStatus) && (
              <Button variant="outline" size="sm" onClick={() => setAddProofOpen(true)}>
                {t('order.proof.add.button')}
              </Button>
            )}
          </CardHeader>
          <CardContent>
            {order.purchaseProofUrls.length > 0 ? (
              <ImageGallery urls={order.purchaseProofUrls} />
            ) : (
              <p className="text-sm text-muted-foreground">—</p>
            )}
          </CardContent>
        </Card>
      )}

      {(order.customerMemo || order.internalMemo) && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">{t('order.detail.memo')}</CardTitle>
          </CardHeader>
          <CardContent className="text-sm space-y-2">
            {order.customerMemo && (
              <div>
                <div className="text-xs text-muted-foreground">{t('field.customer_memo.label')}</div>
                <div className="whitespace-pre-wrap">{order.customerMemo}</div>
              </div>
            )}
            {order.internalMemo && (
              <div>
                <div className="text-xs text-muted-foreground">{t('field.internal_memo.label')}</div>
                <div className="whitespace-pre-wrap">{order.internalMemo}</div>
              </div>
            )}
          </CardContent>
        </Card>
      )}

      <OrderNotesCard orderId={order.id} />

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('order.detail.history')}</CardTitle>
        </CardHeader>
        <CardContent>
          <ol className="space-y-2 text-sm">
            {order.history.map((entry) => (
              <li key={entry.id} className="border-l-2 border-muted pl-3">
                <div className="text-xs text-muted-foreground">
                  {new Date(entry.createdAt).toLocaleString(i18n.resolvedLanguage)} ·{' '}
                  {entry.dimension === 'FULFILLMENT'
                    ? t('order.col.fulfillment')
                    : t('order.col.payment')}
                </div>
                <div className="flex items-center gap-1.5">
                  <span>
                    {entry.fromCode ? `${entry.fromCode} → ` : ''}
                    <span className="font-medium">{entry.toCode}</span>
                  </span>
                  {entry.forced && (
                    <span className="text-[10px] font-medium px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 uppercase">
                      {t('order.history.forced_badge')}
                    </span>
                  )}
                </div>
                {entry.note && <div className="text-xs text-muted-foreground">{entry.note}</div>}
              </li>
            ))}
          </ol>
        </CardContent>
      </Card>

      {trackingEditOpen && (
        <EditTrackingModal
          orderId={order.id}
          currentCourier={order.koreanCourier}
          currentTrackingNo={order.koreanTrackingNo}
          courierCodes={courierCodes ?? []}
          onClose={() => setTrackingEditOpen(false)}
        />
      )}

      {addProofOpen && (
        <AddProofModal orderId={order.id} onClose={() => setAddProofOpen(false)} />
      )}

      {financialsEditOpen && (
        <FinancialsEditModal order={order} onClose={() => setFinancialsEditOpen(false)} />
      )}

      {paidEditOpen && (
        <PaidAmountEditModal
          orderId={order.id}
          currentPaid={order.paidAmountKrw}
          onClose={() => setPaidEditOpen(false)}
        />
      )}
    </div>
  )
}

function fmtAmtCur(
  amount: string | null,
  currency: 'KRW' | 'THB' | null,
  lang: string | undefined,
): string {
  if (!amount || !currency) return '—'
  return formatMoney(amount, currency, lang)
}

function hasTHB(order: import('@/lib/api/orders').OrderDetail): boolean {
  return (
    order.customerRevenueCurrency === 'THB' ||
    order.logisticsKrToThCurrency === 'THB' ||
    order.logisticsThDomesticCurrency === 'THB'
  )
}

function KV({ label, value, href }: { label: string; value: string; href?: string }) {
  return (
    <div className="flex gap-2">
      <span className="w-24 text-muted-foreground">{label}</span>
      {href ? (
        <a
          href={href}
          target="_blank"
          rel="noreferrer"
          className="text-primary hover:underline truncate max-w-md"
        >
          {value}
        </a>
      ) : (
        <span>{value}</span>
      )}
    </div>
  )
}

function isUrl(v: string | null | undefined): boolean {
  return !!v && /^https?:\/\//i.test(v)
}

/**
 * Renders a flat key/value listing of category-specific attributes.
 * Nested groups (e.g. left_eye.power) are flattened with a "."-joined key.
 */
function AttributesView({ attrs }: { attrs: Record<string, unknown> }) {
  const flat = flatten(attrs)
  if (flat.length === 0) return null
  return (
    <div className="flex flex-wrap gap-x-4 gap-y-1">
      {flat.map(([k, v]) => (
        <span key={k}>
          <span className="text-muted-foreground">{k}:</span> {String(v)}
        </span>
      ))}
    </div>
  )
}

function flatten(obj: Record<string, unknown>, prefix = ''): Array<[string, unknown]> {
  const out: Array<[string, unknown]> = []
  for (const [k, v] of Object.entries(obj)) {
    if (v == null || v === '') continue
    const path = prefix ? `${prefix}.${k}` : k
    if (typeof v === 'object' && !Array.isArray(v)) {
      out.push(...flatten(v as Record<string, unknown>, path))
    } else {
      out.push([path, v])
    }
  }
  return out
}

function StatusCard({
  orderId,
  dimension,
  current,
  allowed,
  isAdmin,
}: {
  orderId: number
  dimension: 'FULFILLMENT' | 'PAYMENT'
  current: FulfillmentStatus | PaymentStatus
  allowed: (FulfillmentStatus | PaymentStatus)[]
  isAdmin: boolean
}) {
  const { t } = useTranslation()
  const [note, setNote] = useState('')
  const [forceOpen, setForceOpen] = useState(false)
  const [purchasedOpen, setPurchasedOpen] = useState(false)
  const fulfillment = useChangeFulfillmentStatus(orderId)
  const payment = useChangePaymentStatus(orderId)
  const { data: fulfillmentLabels } = useFulfillmentLabels()
  const { data: paymentLabels } = usePaymentLabels()

  const isFulfillment = dimension === 'FULFILLMENT'
  const tone = isFulfillment
    ? fulfillmentTone(current as FulfillmentStatus)
    : paymentTone(current as PaymentStatus)
  const labels = isFulfillment ? fulfillmentLabels : paymentLabels
  const currentLabel = findLabel(labels, current)
  const mutation = isFulfillment ? fulfillment : payment

  function transition(target: string) {
    // PURCHASED requires a paid amount input — open a modal instead of firing the mutation.
    if (isFulfillment && target === 'PURCHASED') {
      setPurchasedOpen(true)
      return
    }
    if (isFulfillment) {
      fulfillment.mutate(
        { target: target as FulfillmentStatus, note: note || undefined },
        { onSuccess: () => setNote('') }
      )
    } else {
      payment.mutate(
        { target: target as PaymentStatus, note: note || undefined },
        { onSuccess: () => setNote('') }
      )
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">
          {isFulfillment ? t('order.col.fulfillment') : t('order.col.payment')}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div>
          <Badge tone={tone}>{currentLabel}</Badge>
        </div>
        {isAdmin && allowed.length > 0 ? (
          <>
            <Textarea
              rows={2}
              placeholder={t('order.detail.note_placeholder')}
              value={note}
              onChange={(e) => setNote(e.target.value)}
            />
            <div className="flex flex-wrap gap-2">
              {allowed.map((target) => (
                <Button
                  key={target}
                  variant="outline"
                  size="sm"
                  disabled={mutation.isPending}
                  onClick={() => transition(target)}
                >
                  → {findLabel(labels, target)}
                </Button>
              ))}
            </div>
            {mutation.isError && (
              <Alert variant="destructive">
                <AlertDescription>
                  {mutation.error instanceof ApiError
                    ? mutation.error.message
                    : t('msg.error.unexpected')}
                </AlertDescription>
              </Alert>
            )}
          </>
        ) : isAdmin ? (
          <p className="text-xs text-muted-foreground">{t('order.detail.terminal')}</p>
        ) : null}
        {isAdmin && (
          <button
            type="button"
            onClick={() => setForceOpen(true)}
            className="text-xs text-muted-foreground hover:text-foreground hover:underline"
          >
            {t('order.status.force.button')}
          </button>
        )}
      </CardContent>
      {isAdmin && forceOpen && (
        <ForceStatusModal
          orderId={orderId}
          dimension={dimension}
          current={current}
          onClose={() => setForceOpen(false)}
        />
      )}
      {isFulfillment && purchasedOpen && (
        <PurchasedModal
          note={note}
          mutation={fulfillment}
          onClose={() => setPurchasedOpen(false)}
          onSuccess={() => {
            setNote('')
            setPurchasedOpen(false)
          }}
        />
      )}
    </Card>
  )
}

function PurchasedModal({
  note,
  mutation,
  onClose,
  onSuccess,
}: {
  note: string
  mutation: ReturnType<typeof useChangeFulfillmentStatus>
  onClose: () => void
  onSuccess: () => void
}) {
  const { t } = useTranslation()
  const [paid, setPaid] = useState('')
  const [proofImages, setProofImages] = useState<UploadedImage[]>([])

  function submit(e: FormEvent) {
    e.preventDefault()
    const parsed = Number(paid)
    if (!Number.isFinite(parsed) || parsed <= 0) return
    mutation.mutate(
      {
        target: 'PURCHASED',
        note: note || undefined,
        paidAmountKrw: paid.trim(),
        proofTempKeys: proofImages.map((i) => i.tempKey),
      },
      { onSuccess },
    )
  }

  function errorMessage(): string {
    if (!(mutation.error instanceof ApiError)) return t('msg.error.unexpected')
    if (mutation.error.message === 'paid_amount_required') {
      return t('msg.error.paid_amount_required')
    }
    return mutation.error.message
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <form
        onClick={(e) => e.stopPropagation()}
        onSubmit={submit}
        className="bg-background rounded-md w-full max-w-md p-4 space-y-3"
      >
        <div className="text-base font-semibold">{t('order.purchased.modal.title')}</div>
        <p className="text-xs text-muted-foreground">{t('order.purchased.modal.help')}</p>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">
            {t('order.purchased.modal.paid')}
          </label>
          <Input
            type="text"
            inputMode="numeric"
            value={paid}
            onChange={(e) => setPaid(e.target.value.replace(/[^\d]/g, ''))}
            required
            autoFocus
          />
        </div>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">
            {t('order.purchased.modal.proof')}
          </label>
          <ImageUploader value={proofImages} onChange={setProofImages} />
        </div>
        {mutation.isError && (
          <Alert variant="destructive">
            <AlertDescription>{errorMessage()}</AlertDescription>
          </Alert>
        )}
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="outline" size="sm" onClick={onClose}>
            {t('btn.cancel')}
          </Button>
          <Button type="submit" size="sm" disabled={mutation.isPending || !paid}>
            {mutation.isPending ? t('msg.loading') : t('order.purchased.modal.confirm')}
          </Button>
        </div>
      </form>
    </div>,
    document.body,
  )
}

function ForceStatusModal({
  orderId,
  dimension,
  current,
  onClose,
}: {
  orderId: number
  dimension: 'FULFILLMENT' | 'PAYMENT'
  current: FulfillmentStatus | PaymentStatus
  onClose: () => void
}) {
  const { t } = useTranslation()
  const isFulfillment = dimension === 'FULFILLMENT'
  const { data: fulfillmentLabels } = useFulfillmentLabels()
  const { data: paymentLabels } = usePaymentLabels()
  const labels = isFulfillment ? fulfillmentLabels : paymentLabels
  const targets = (labels ?? []).filter((c) => c.code !== current)
  const [target, setTarget] = useState('')
  const [reason, setReason] = useState('')
  const fulfillmentMutation = useForceFulfillmentStatus(orderId)
  const paymentMutation = useForcePaymentStatus(orderId)
  const mutation = isFulfillment ? fulfillmentMutation : paymentMutation

  // Once labels load, default the target select to the first non-current option.
  const firstTargetCode = targets[0]?.code
  useEffect(() => {
    if (!target && firstTargetCode) setTarget(firstTargetCode)
  }, [target, firstTargetCode])

  function submit(e: FormEvent) {
    e.preventDefault()
    if (!target) return
    if (!reason.trim()) return
    if (isFulfillment) {
      fulfillmentMutation.mutate(
        { target: target as FulfillmentStatus, reason: reason.trim() },
        { onSuccess: onClose },
      )
    } else {
      paymentMutation.mutate(
        { target: target as PaymentStatus, reason: reason.trim() },
        { onSuccess: onClose },
      )
    }
  }

  function errorMessage(): string {
    if (!(mutation.error instanceof ApiError)) return t('msg.error.unexpected')
    const msg = mutation.error.message
    if (msg === 'force_reason_required') return t('msg.error.force_reason_required')
    if (msg === 'force_same_status') return t('msg.error.force_same_status')
    return msg
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <form
        onClick={(e) => e.stopPropagation()}
        onSubmit={submit}
        className="bg-background rounded-md w-full max-w-md p-4 space-y-3"
      >
        <div className="text-base font-semibold">{t('order.status.force.title')}</div>
        <p className="text-xs text-muted-foreground">{t('order.status.force.help')}</p>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">
            {t('order.status.force.target')}
          </label>
          <Select value={target} onChange={(e) => setTarget(e.target.value)}>
            {targets.map((c) => (
              <option key={c.code} value={c.code}>
                {c.name}
              </option>
            ))}
          </Select>
        </div>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">
            {t('order.status.force.reason')}
          </label>
          <Textarea
            rows={3}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            required
          />
        </div>
        {mutation.isError && (
          <Alert variant="destructive">
            <AlertDescription>{errorMessage()}</AlertDescription>
          </Alert>
        )}
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="outline" size="sm" onClick={onClose}>
            {t('btn.cancel')}
          </Button>
          <Button
            type="submit"
            variant="destructive"
            size="sm"
            disabled={mutation.isPending || !reason.trim() || !target}
          >
            {mutation.isPending ? t('msg.loading') : t('order.status.force.confirm')}
          </Button>
        </div>
      </form>
    </div>,
    document.body,
  )
}

function EditTrackingModal({
  orderId,
  currentCourier,
  currentTrackingNo,
  courierCodes,
  onClose,
}: {
  orderId: number
  currentCourier: string | null
  currentTrackingNo: string | null
  courierCodes: { code: string; name: string }[]
  onClose: () => void
}) {
  const { t } = useTranslation()
  const [courier, setCourier] = useState(currentCourier ?? '')
  const [trackingNo, setTrackingNo] = useState(currentTrackingNo ?? '')
  const mutation = useUpdateTracking(orderId)

  function submit(e: FormEvent) {
    e.preventDefault()
    mutation.mutate(
      { courier: courier.trim() || null, trackingNo: trackingNo.trim() || null },
      { onSuccess: onClose },
    )
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <form
        onClick={(e) => e.stopPropagation()}
        onSubmit={submit}
        className="bg-background rounded-md w-full max-w-md p-4 space-y-3"
      >
        <div className="text-base font-semibold">{t('order.tracking.edit.title')}</div>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">{t('field.courier.label')}</label>
          <Select value={courier} onChange={(e) => setCourier(e.target.value)}>
            <option value="">—</option>
            {courierCodes.map((c) => (
              <option key={c.code} value={c.code}>
                {c.name}
              </option>
            ))}
          </Select>
        </div>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">{t('field.tracking_no.label')}</label>
          <Input
            type="text"
            value={trackingNo}
            onChange={(e) => setTrackingNo(e.target.value)}
            maxLength={500}
          />
        </div>
        {mutation.isError && (
          <Alert variant="destructive">
            <AlertDescription>
              {mutation.error instanceof ApiError ? mutation.error.message : t('msg.error.unexpected')}
            </AlertDescription>
          </Alert>
        )}
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="outline" size="sm" onClick={onClose}>
            {t('btn.cancel')}
          </Button>
          <Button type="submit" size="sm" disabled={mutation.isPending}>
            {mutation.isPending ? t('msg.loading') : t('order.tracking.edit.save')}
          </Button>
        </div>
      </form>
    </div>,
    document.body,
  )
}

function AddProofModal({ orderId, onClose }: { orderId: number; onClose: () => void }) {
  const { t } = useTranslation()
  const [images, setImages] = useState<UploadedImage[]>([])
  const mutation = useAddPurchaseProofs(orderId)

  function submit(e: FormEvent) {
    e.preventDefault()
    if (images.length === 0) return
    mutation.mutate(
      images.map((i) => i.tempKey),
      { onSuccess: onClose },
    )
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <form
        onClick={(e) => e.stopPropagation()}
        onSubmit={submit}
        className="bg-background rounded-md w-full max-w-md p-4 space-y-3"
      >
        <div className="text-base font-semibold">{t('order.proof.add.title')}</div>
        <ImageUploader value={images} onChange={setImages} />
        {mutation.isError && (
          <Alert variant="destructive">
            <AlertDescription>
              {mutation.error instanceof ApiError ? mutation.error.message : t('msg.error.unexpected')}
            </AlertDescription>
          </Alert>
        )}
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="outline" size="sm" onClick={onClose}>
            {t('btn.cancel')}
          </Button>
          <Button type="submit" size="sm" disabled={mutation.isPending || images.length === 0}>
            {mutation.isPending ? t('msg.loading') : t('btn.save')}
          </Button>
        </div>
      </form>
    </div>,
    document.body,
  )
}

function FinancialsEditModal({
  order,
  onClose,
}: {
  order: import('@/lib/api/orders').OrderDetail
  onClose: () => void
}) {
  const { t } = useTranslation()
  const mutation = useUpdateFinancials(order.id)
  const [revAmt, setRevAmt] = useState(order.customerRevenueAmount != null ? String(order.customerRevenueAmount) : '')
  const [revCur, setRevCur] = useState<'KRW' | 'THB' | ''>(order.customerRevenueCurrency ?? '')
  const [logKrAmt, setLogKrAmt] = useState(order.logisticsKrToThAmount != null ? String(order.logisticsKrToThAmount) : '')
  const [logKrCur, setLogKrCur] = useState<'KRW' | 'THB' | ''>(order.logisticsKrToThCurrency ?? '')
  const [logThAmt, setLogThAmt] = useState(order.logisticsThDomesticAmount != null ? String(order.logisticsThDomesticAmount) : '')
  const [logThCur, setLogThCur] = useState<'KRW' | 'THB' | ''>(order.logisticsThDomesticCurrency ?? '')
  const [fx, setFx] = useState(order.krwPerThb != null ? String(order.krwPerThb) : '')
  const [fxMode, setFxMode] = useState<'KRW_PER_THB' | 'THB_PER_KRW'>('KRW_PER_THB')
  const [localError, setLocalError] = useState<string | null>(null)

  function clampFraction(v: string, fraction: number): string {
    const n = Number(v)
    if (!Number.isFinite(n)) return v
    return n.toFixed(fraction).replace(/0+$/, '').replace(/\.$/, '')
  }

  function invertFx(v: string): string {
    const n = Number(v)
    if (!Number.isFinite(n) || n === 0) return v
    return clampFraction(String(1 / n), 4)
  }

  function switchFxMode(next: 'KRW_PER_THB' | 'THB_PER_KRW') {
    if (next === fxMode) return
    if (fx.trim()) setFx(invertFx(fx.trim()))
    setFxMode(next)
  }

  const anyTHB = revCur === 'THB' || logKrCur === 'THB' || logThCur === 'THB'

  function isBlankOrZero(v: string): boolean {
    const trimmed = v.trim()
    if (!trimmed) return true
    const n = Number(trimmed)
    return Number.isFinite(n) && n === 0
  }

  function pair(amt: string, cur: 'KRW' | 'THB' | '') {
    if (isBlankOrZero(amt)) return [null, null] as const
    if (!cur) {
      throw new Error('amount_currency_mismatch')
    }
    return [amt.trim(), cur] as const
  }

  function submit(e: FormEvent) {
    e.preventDefault()
    setLocalError(null)
    try {
      const [ra, rc] = pair(revAmt, revCur)
      const [la, lc] = pair(logKrAmt, logKrCur)
      const [ta, tc] = pair(logThAmt, logThCur)
      mutation.mutate(
        {
          customerRevenueAmount: ra,
          customerRevenueCurrency: rc,
          logisticsKrToThAmount: la,
          logisticsKrToThCurrency: lc,
          logisticsThDomesticAmount: ta,
          logisticsThDomesticCurrency: tc,
          krwPerThb: isBlankOrZero(fx)
            ? null
            : (fxMode === 'KRW_PER_THB' ? clampFraction(fx.trim(), 4) : invertFx(fx.trim())),
        },
        { onSuccess: onClose },
      )
    } catch (err) {
      if (err instanceof Error && err.message === 'amount_currency_mismatch') {
        setLocalError(t('msg.error.amount_currency_mismatch'))
      } else {
        setLocalError(t('msg.error.unexpected'))
      }
    }
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <form
        onClick={(e) => e.stopPropagation()}
        onSubmit={submit}
        className="bg-background rounded-md w-full max-w-md p-4 space-y-3 max-h-[90vh] overflow-y-auto"
      >
        <div className="text-base font-semibold">{t('order.detail.financials.edit')}</div>

        <FinancialPair
          label={t('field.customer_revenue.label')}
          amount={revAmt}
          currency={revCur}
          onAmount={setRevAmt}
          onCurrency={setRevCur}
        />
        <FinancialPair
          label={t('field.logistics_kr_th.label')}
          amount={logKrAmt}
          currency={logKrCur}
          onAmount={setLogKrAmt}
          onCurrency={setLogKrCur}
        />
        <FinancialPair
          label={t('field.logistics_th_dom.label')}
          amount={logThAmt}
          currency={logThCur}
          onAmount={setLogThAmt}
          onCurrency={setLogThCur}
        />
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">{t('field.krw_per_thb.label')}</label>
          <div className="flex gap-1">
            <Button
              type="button"
              size="sm"
              variant={fxMode === 'KRW_PER_THB' ? 'default' : 'outline'}
              onClick={() => switchFxMode('KRW_PER_THB')}
            >
              {t('field.fx.dir.krw_per_thb')}
            </Button>
            <Button
              type="button"
              size="sm"
              variant={fxMode === 'THB_PER_KRW' ? 'default' : 'outline'}
              onClick={() => switchFxMode('THB_PER_KRW')}
            >
              {t('field.fx.dir.thb_per_krw')}
            </Button>
          </div>
          <div className="flex gap-2">
            <Input
              type="text"
              inputMode="decimal"
              value={fx}
              onChange={(e) => setFx(e.target.value.replace(/[^0-9.]/g, ''))}
              placeholder={fxMode === 'KRW_PER_THB' ? '38.5' : '0.026'}
              className="flex-1"
            />
            <Button
              type="button"
              variant="ghost"
              size="icon"
              aria-label={t('aria.field_reset')}
              disabled={!fx.trim()}
              onClick={() => setFx('')}
            >
              <X className="h-4 w-4" />
            </Button>
          </div>
          {anyTHB && !fx.trim() && (
            <p className="text-xs text-amber-600">
              {t('order.detail.financials.fx_required_hint')}
            </p>
          )}
        </div>

        {localError && (
          <Alert variant="destructive">
            <AlertDescription>{localError}</AlertDescription>
          </Alert>
        )}
        {mutation.isError && (
          <Alert variant="destructive">
            <AlertDescription>
              {mutation.error instanceof ApiError ? mutation.error.message : t('msg.error.unexpected')}
            </AlertDescription>
          </Alert>
        )}
        <div className="flex justify-between gap-2 pt-1">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => {
              setRevAmt(''); setRevCur('')
              setLogKrAmt(''); setLogKrCur('')
              setLogThAmt(''); setLogThCur('')
              setFx('')
              setLocalError(null)
            }}
          >
            {t('btn.reset_all')}
          </Button>
          <div className="flex gap-2">
            <Button type="button" variant="outline" size="sm" onClick={onClose}>
              {t('btn.cancel')}
            </Button>
            <Button type="submit" size="sm" disabled={mutation.isPending}>
              {mutation.isPending ? t('msg.loading') : t('btn.save')}
            </Button>
          </div>
        </div>
      </form>
    </div>,
    document.body,
  )
}

function FinancialPair({
  label,
  amount,
  currency,
  onAmount,
  onCurrency,
}: {
  label: string
  amount: string
  currency: 'KRW' | 'THB' | ''
  onAmount: (v: string) => void
  onCurrency: (v: 'KRW' | 'THB' | '') => void
}) {
  const { t } = useTranslation()
  const hasValue = amount.trim() !== '' || currency !== ''
  return (
    <div className="space-y-1">
      <label className="text-xs text-muted-foreground">{label}</label>
      <div className="flex gap-2">
        <Input
          type="text"
          inputMode="decimal"
          value={amount}
          onChange={(e) => onAmount(e.target.value.replace(/[^0-9.]/g, ''))}
          placeholder="0"
          className="flex-1"
        />
        <Select
          value={currency}
          onChange={(e) => onCurrency(e.target.value as 'KRW' | 'THB' | '')}
          className="w-24"
        >
          <option value="">—</option>
          <option value="KRW">KRW</option>
          <option value="THB">THB</option>
        </Select>
        <Button
          type="button"
          variant="ghost"
          size="icon"
          aria-label={t('aria.field_reset')}
          disabled={!hasValue}
          onClick={() => { onAmount(''); onCurrency('') }}
        >
          <X className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}

function PaidAmountEditModal({
  orderId,
  currentPaid,
  onClose,
}: {
  orderId: number
  currentPaid: string | number | null
  onClose: () => void
}) {
  const { t } = useTranslation()
  const [paid, setPaid] = useState(currentPaid != null ? String(currentPaid) : '')
  const mutation = useUpdatePaidAmount(orderId)

  function submit(e: FormEvent) {
    e.preventDefault()
    if (!paid.trim()) return
    mutation.mutate(paid.trim(), { onSuccess: onClose })
  }

  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
      onClick={onClose}
    >
      <form
        onClick={(e) => e.stopPropagation()}
        onSubmit={submit}
        className="bg-background rounded-md w-full max-w-md p-4 space-y-3"
      >
        <div className="text-base font-semibold">
          {currentPaid ? t('order.detail.paid_amount.edit') : t('order.detail.paid_amount.input')}
        </div>
        {currentPaid && (
          <p className="text-xs text-muted-foreground">{t('msg.deposit.paid_amount_correction')}</p>
        )}
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">{t('field.paid_amount.label')} (KRW)</label>
          <Input
            type="text"
            inputMode="numeric"
            value={paid}
            onChange={(e) => setPaid(e.target.value.replace(/[^\d]/g, ''))}
            autoFocus
          />
        </div>
        {mutation.isError && (
          <Alert variant="destructive">
            <AlertDescription>
              {mutation.error instanceof ApiError ? mutation.error.message : t('msg.error.unexpected')}
            </AlertDescription>
          </Alert>
        )}
        <div className="flex justify-end gap-2 pt-1">
          <Button type="button" variant="outline" size="sm" onClick={onClose}>
            {t('btn.cancel')}
          </Button>
          <Button type="submit" size="sm" disabled={mutation.isPending || !paid.trim()}>
            {mutation.isPending ? t('msg.loading') : t('btn.save')}
          </Button>
        </div>
      </form>
    </div>,
    document.body,
  )
}
