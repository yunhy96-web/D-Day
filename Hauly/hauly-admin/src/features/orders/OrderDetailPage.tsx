import { Fragment, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft, Trash2 } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'

import {
  useChangeFulfillmentStatus,
  useChangePaymentStatus,
  useDeleteOrder,
  useOrder,
} from './hooks'
import { useCategories } from './categoryHooks'
import { useCommonCodeGroup } from './commonCodeHooks'
import { formatMoney } from './money'
import { ImageGallery } from './ImageGallery'
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
    <div className="p-6 max-w-4xl mx-auto space-y-4">
      <div className="flex items-center gap-2">
        <Button variant="ghost" size="sm" onClick={() => navigate('/orders')}>
          <ArrowLeft className="h-4 w-4 mr-1" /> {t('menu.orders')}
        </Button>
        <h1 className="text-2xl font-semibold ml-2">{order.orderNo}</h1>
        {me?.role === 'ADMIN' && (
          <Button
            variant="destructive"
            size="sm"
            className="ml-auto"
            disabled={deleteMutation.isPending}
            onClick={() => {
              const ok = window.confirm(
                `주문 ${order.orderNo} 을(를) 영구 삭제합니다.\n\n` +
                `⚠ 이 작업은 복구할 수 없습니다.\n` +
                `주문 항목, 상태 변경 이력까지 모두 함께 삭제됩니다.\n\n` +
                `정말 삭제하시겠습니까?`,
              )
              if (!ok) return
              deleteMutation.mutate(order.id, {
                onSuccess: () => navigate('/orders', { replace: true }),
              })
            }}
          >
            <Trash2 className="h-4 w-4 mr-1" />
            {deleteMutation.isPending ? '삭제 중…' : '삭제'}
          </Button>
        )}
      </div>

      {deleteMutation.isError && (
        <Alert variant="destructive">
          <AlertDescription>
            {deleteMutation.error instanceof ApiError
              ? `삭제에 실패했습니다 (${deleteMutation.error.code}): ${deleteMutation.error.message}`
              : '삭제에 실패했습니다. 잠시 후 다시 시도해 주세요.'}
          </AlertDescription>
        </Alert>
      )}

      <div className="grid grid-cols-2 gap-3">
        <StatusCard
          orderId={order.id}
          dimension="FULFILLMENT"
          current={order.fulfillmentStatus}
          allowed={order.allowedFulfillmentNext}
        />
        <StatusCard
          orderId={order.id}
          dimension="PAYMENT"
          current={order.paymentStatus}
          allowed={order.allowedPaymentNext}
        />
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('order.detail.customer')}</CardTitle>
        </CardHeader>
        <CardContent className="text-sm space-y-1">
          <KV label={t('field.customer_name.label')} value={order.customerName} />
          <KV label={t('field.line_id.label')} value={order.customerLineId ?? '-'} />
          <KV label={t('field.phone.label')} value={order.customerPhone ?? '-'} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('order.detail.items')}</CardTitle>
        </CardHeader>
        <CardContent>
          <table className="w-full text-sm">
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

      {(order.koreanTrackingNo || order.koreanCourier) && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">{t('order.detail.tracking')}</CardTitle>
          </CardHeader>
          <CardContent className="text-sm space-y-1">
            <KV label={t('field.tracking_no.label')} value={order.koreanTrackingNo ?? '-'} />
            <KV label={t('field.courier.label')} value={courierLabel(order.koreanCourier)} />
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
                <div>
                  {entry.fromCode ? `${entry.fromCode} → ` : ''}
                  <span className="font-medium">{entry.toCode}</span>
                </div>
                {entry.note && <div className="text-xs text-muted-foreground">{entry.note}</div>}
              </li>
            ))}
          </ol>
        </CardContent>
      </Card>
    </div>
  )
}

function KV({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-2">
      <span className="w-24 text-muted-foreground">{label}</span>
      <span>{value}</span>
    </div>
  )
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
}: {
  orderId: number
  dimension: 'FULFILLMENT' | 'PAYMENT'
  current: FulfillmentStatus | PaymentStatus
  allowed: (FulfillmentStatus | PaymentStatus)[]
}) {
  const { t } = useTranslation()
  const [note, setNote] = useState('')
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
        {allowed.length > 0 ? (
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
        ) : (
          <p className="text-xs text-muted-foreground">{t('order.detail.terminal')}</p>
        )}
      </CardContent>
    </Card>
  )
}
