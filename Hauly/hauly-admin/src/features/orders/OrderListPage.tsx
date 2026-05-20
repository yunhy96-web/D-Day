import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Search } from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select } from '@/components/ui/select'
import { useOrders } from './hooks'
import { useCommonCodeGroup } from './commonCodeHooks'
import { useIsAdmin } from '@/features/auth/hooks'
import {
  findLabel,
  fulfillmentTone,
  paymentTone,
  useFulfillmentLabels,
  usePaymentLabels,
} from './statusLabels'
import { useDebounced } from './useDebounced'
import { formatMoney } from './money'
import { ImageLightbox } from './ImageLightbox'
import type { OrderSortOption } from '@/lib/api/orders'

const SORT_OPTIONS: Array<{ value: OrderSortOption; labelKey: string }> = [
  { value: 'createdAt-desc', labelKey: 'order.list.sort.created_desc' },
  { value: 'createdAt-asc', labelKey: 'order.list.sort.created_asc' },
  { value: 'fulfillmentStatus-asc', labelKey: 'order.list.sort.fulfillment' },
  { value: 'productName-asc', labelKey: 'order.list.sort.product_name' },
]

export default function OrderListPage() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const isAdmin = useIsAdmin()
  const [query, setQuery] = useState('')
  const [sort, setSort] = useState<OrderSortOption>('createdAt-desc')
  const [lightboxUrl, setLightboxUrl] = useState<string | null>(null)
  const debouncedQuery = useDebounced(query, 300)

  // Memoize so the query key stays referentially stable across re-renders —
  // otherwise a parent re-render storm (e.g. from a browser extension mutating
  // the DOM) creates a fresh QueryObserver each time.
  const queryParams = useMemo(
    () => ({ q: debouncedQuery || undefined, sort }),
    [debouncedQuery, sort],
  )
  const { data, isLoading, isError, refetch, isFetching } = useOrders(queryParams)
  const { data: fulfillmentLabels } = useFulfillmentLabels()
  const { data: paymentLabels } = usePaymentLabels()
  const { data: courierLabels } = useCommonCodeGroup('COURIER_KR')

  return (
    <div className="p-4 md:p-6 space-y-4">
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-xl md:text-2xl font-semibold">{t('menu.orders')}</h1>
        {isAdmin && (
          <Link
            to="/orders/new"
            className="inline-flex items-center justify-center rounded-md bg-primary px-3 h-9 text-sm font-medium text-primary-foreground hover:bg-primary/90 whitespace-nowrap"
          >
            + {t('menu.orders_new')}
          </Link>
        )}
      </div>

      <Card>
        <CardHeader className="space-y-3">
          <CardTitle className="text-base">{t('order.list.title')}</CardTitle>
          <div className="flex flex-col sm:flex-row gap-2 sm:gap-3 sm:items-center">
            <div className="relative flex-1 sm:max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={t('order.list.search.placeholder')}
                className="pl-9"
              />
            </div>
            <div className="flex items-center gap-2">
              <span className="text-xs text-muted-foreground whitespace-nowrap">
                {t('order.list.sort.label')}
              </span>
              <Select
                value={sort}
                onChange={(e) => setSort(e.target.value as OrderSortOption)}
                className="flex-1 sm:w-44 sm:flex-none"
              >
                {SORT_OPTIONS.map((o) => (
                  <option key={o.value} value={o.value}>
                    {t(o.labelKey)}
                  </option>
                ))}
              </Select>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading && <p className="text-sm text-muted-foreground">{t('msg.loading')}</p>}
          {isError && (
            <div className="space-y-2">
              <p className="text-sm text-destructive">{t('msg.error.unexpected')}</p>
              <Button variant="outline" size="sm" onClick={() => refetch()}>
                {t('btn.reload')}
              </Button>
            </div>
          )}
          {data && data.content.length === 0 && !isFetching && (
            <p className="text-sm text-muted-foreground">{t('order.list.empty')}</p>
          )}
          {data && data.content.length > 0 && (
            <>
              {/* Desktop / tablet: table */}
              <div className="hidden md:block overflow-x-auto">
                <table className="w-full text-sm">
                  <thead className="text-xs uppercase text-muted-foreground border-b">
                    <tr>
                      <th className="text-left py-2 px-2">{t('order.col.no')}</th>
                      <th className="text-left py-2 px-2">{t('order.col.customer')}</th>
                      <th className="text-left py-2 px-2">{t('order.col.product')}</th>
                      <th className="text-left py-2 px-2">{t('order.col.amount')}</th>
                      <th className="text-left py-2 px-2">{t('order.col.tracking')}</th>
                      <th className="text-left py-2 px-2">{t('order.col.fulfillment')}</th>
                      <th className="text-left py-2 px-2">{t('order.col.payment')}</th>
                      <th className="text-left py-2 px-2">{t('order.col.created_at')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.content.map((o) => (
                      <tr
                        key={o.id}
                        className="border-b hover:bg-muted/40 cursor-pointer"
                        onClick={() => navigate(`/orders/${o.id}`)}
                      >
                        <td className="py-2 px-2 font-mono text-xs">
                          <div className="flex items-center gap-1.5">
                            <Link
                              to={`/orders/${o.id}`}
                              className="text-primary hover:underline"
                              onClick={(e) => e.stopPropagation()}
                            >
                              {o.orderNo}
                            </Link>
                            {o.orderType === 'SET' && (
                              <span className="text-[10px] font-sans font-medium px-1.5 py-0.5 rounded bg-purple-100 text-purple-700">
                                {t('badge.set')}
                              </span>
                            )}
                          </div>
                        </td>
                        <td className="py-2 px-2">
                          <div>{o.customerName}</div>
                          {o.shippingAddressLabel && (
                            <div className="text-[10px] text-muted-foreground truncate max-w-[160px]">
                              → {o.shippingAddressLabel}
                            </div>
                          )}
                        </td>
                        <td className="py-2 px-2 max-w-[280px]">
                          <div className="flex items-start gap-2">
                            {o.firstImageUrl ? (
                              <button
                                type="button"
                                onClick={(e) => {
                                  e.stopPropagation()
                                  setLightboxUrl(o.firstImageUrl)
                                }}
                                className="w-10 h-10 shrink-0 rounded border overflow-hidden bg-muted hover:opacity-80"
                                aria-label={t('image.aria.zoom', { n: 1 })}
                              >
                                <img
                                  src={o.firstImageUrl}
                                  alt=""
                                  loading="lazy"
                                  decoding="async"
                                  className="w-full h-full object-cover"
                                />
                              </button>
                            ) : (
                              <div className="w-10 h-10 shrink-0 rounded border bg-muted/30" />
                            )}
                            <div className="min-w-0">
                              <div className="truncate" title={o.firstProductName ?? ''}>
                                {o.firstProductName ?? <span className="text-muted-foreground">—</span>}
                              </div>
                              {o.itemCount > 1 && (
                                <div className="text-[10px] text-muted-foreground">
                                  + {o.itemCount - 1} {t('order.col.items.more')}
                                </div>
                              )}
                            </div>
                          </div>
                        </td>
                        <td className="py-2 px-2 whitespace-nowrap">
                          {Object.keys(o.totalsByCurrency).length === 0 && !o.paidAmountKrw && !o.netProfitKrw ? (
                            <span className="text-muted-foreground">—</span>
                          ) : (
                            <div className="flex flex-col">
                              {Object.entries(o.totalsByCurrency).map(([cur, amount]) => (
                                <span key={cur} className="text-xs">
                                  <span className="text-muted-foreground">{t('field.expected.short')}:</span>{' '}
                                  {formatMoney(amount, cur, i18n.resolvedLanguage)}
                                </span>
                              ))}
                              {o.paidAmountKrw && (
                                <span className="text-[11px] text-emerald-700 font-medium">
                                  {t('field.actual.short')}: {formatMoney(o.paidAmountKrw, 'KRW', i18n.resolvedLanguage)}
                                </span>
                              )}
                              {o.netProfitKrw != null && (
                                <span className={
                                  Number(o.netProfitKrw) >= 0
                                    ? 'text-[11px] text-blue-700 font-medium'
                                    : 'text-[11px] text-red-700 font-medium'
                                }>
                                  {t('field.income.short')}: {formatMoney(o.netProfitKrw, 'KRW', i18n.resolvedLanguage)}
                                  {o.netProfitThb != null && (
                                    <span className="ml-1 font-normal text-muted-foreground">
                                      ≈ {formatMoney(o.netProfitThb, 'THB', i18n.resolvedLanguage)}
                                    </span>
                                  )}
                                </span>
                              )}
                            </div>
                          )}
                        </td>
                        <td className="py-2 px-2 text-xs">
                          {o.koreanCourier || o.koreanTrackingNo ? (
                            <div className="flex flex-col">
                              {o.koreanCourier && (
                                <span>{findLabel(courierLabels, o.koreanCourier)}</span>
                              )}
                              {o.koreanTrackingNo && (
                                <span className="font-mono text-[11px] text-muted-foreground truncate max-w-[140px]">
                                  {o.koreanTrackingNo}
                                </span>
                              )}
                            </div>
                          ) : (
                            <span className="text-muted-foreground">—</span>
                          )}
                        </td>
                        <td className="py-2 px-2">
                          <Badge tone={fulfillmentTone(o.fulfillmentStatus)}>
                            {findLabel(fulfillmentLabels, o.fulfillmentStatus)}
                          </Badge>
                        </td>
                        <td className="py-2 px-2">
                          <Badge tone={paymentTone(o.paymentStatus)}>
                            {findLabel(paymentLabels, o.paymentStatus)}
                          </Badge>
                        </td>
                        <td className="py-2 px-2 text-muted-foreground">
                          {new Date(o.createdAt).toLocaleString(i18n.resolvedLanguage)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Mobile: card list */}
              <ul className="md:hidden space-y-2">
                {data.content.map((o) => (
                  <li
                    key={o.id}
                    onClick={() => navigate(`/orders/${o.id}`)}
                    className="border rounded-md p-3 bg-background hover:bg-muted/30 cursor-pointer space-y-2"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono text-xs text-primary">{o.orderNo}</span>
                        {o.orderType === 'SET' && (
                          <span className="text-[10px] font-medium px-1.5 py-0.5 rounded bg-purple-100 text-purple-700">
                            {t('badge.set')}
                          </span>
                        )}
                      </div>
                      <span className="text-[10px] text-muted-foreground">
                        {new Date(o.createdAt).toLocaleDateString(i18n.resolvedLanguage)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-sm font-medium truncate">{o.customerName}</span>
                      <span className="text-xs text-muted-foreground whitespace-nowrap">
                        {o.itemCount} {t('order.col.items')}
                      </span>
                    </div>
                    {o.shippingAddressLabel && (
                      <div className="text-[11px] text-muted-foreground truncate">
                        {t('order.col.shipping_to')}: {o.shippingAddressLabel}
                      </div>
                    )}
                    {(o.firstProductName || o.firstImageUrl) && (
                      <div className="flex items-start gap-2">
                        {o.firstImageUrl && (
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation()
                              setLightboxUrl(o.firstImageUrl)
                            }}
                            className="w-12 h-12 shrink-0 rounded border overflow-hidden bg-muted hover:opacity-80"
                            aria-label={t('image.aria.zoom', { n: 1 })}
                          >
                            <img
                              src={o.firstImageUrl}
                              alt=""
                              loading="lazy"
                              decoding="async"
                              className="w-full h-full object-cover"
                            />
                          </button>
                        )}
                        {o.firstProductName && (
                          <div className="text-xs truncate min-w-0" title={o.firstProductName}>
                            {o.firstProductName}
                            {o.itemCount > 1 && (
                              <span className="text-muted-foreground">
                                {' '}+ {o.itemCount - 1} {t('order.col.items.more')}
                              </span>
                            )}
                          </div>
                        )}
                      </div>
                    )}
                    {(o.koreanCourier || o.koreanTrackingNo) && (
                      <div className="text-[11px] text-muted-foreground flex items-center gap-1.5 flex-wrap">
                        {o.koreanCourier && (
                          <span>{findLabel(courierLabels, o.koreanCourier)}</span>
                        )}
                        {o.koreanTrackingNo && (
                          <span className="font-mono truncate">{o.koreanTrackingNo}</span>
                        )}
                      </div>
                    )}
                    {(Object.keys(o.totalsByCurrency).length > 0 || o.paidAmountKrw || o.netProfitKrw != null) && (
                      <div className="text-xs flex flex-wrap gap-x-3 gap-y-0.5">
                        {Object.entries(o.totalsByCurrency).map(([cur, amount]) => (
                          <span key={cur}>
                            <span className="text-muted-foreground">{t('field.expected.short')}:</span>{' '}
                            {formatMoney(amount, cur, i18n.resolvedLanguage)}
                          </span>
                        ))}
                        {o.paidAmountKrw && (
                          <span className="text-emerald-700 font-medium">
                            {t('field.actual.short')}: {formatMoney(o.paidAmountKrw, 'KRW', i18n.resolvedLanguage)}
                          </span>
                        )}
                        {o.netProfitKrw != null && (
                          <span className={Number(o.netProfitKrw) >= 0 ? 'text-blue-700 font-medium' : 'text-red-700 font-medium'}>
                            {t('field.income.short')}: {formatMoney(o.netProfitKrw, 'KRW', i18n.resolvedLanguage)}
                            {o.netProfitThb != null && (
                              <span className="ml-1 font-normal text-muted-foreground">
                                ≈ {formatMoney(o.netProfitThb, 'THB', i18n.resolvedLanguage)}
                              </span>
                            )}
                          </span>
                        )}
                      </div>
                    )}
                    <div className="flex flex-wrap gap-1.5">
                      <Badge tone={fulfillmentTone(o.fulfillmentStatus)}>
                        {findLabel(fulfillmentLabels, o.fulfillmentStatus)}
                      </Badge>
                      <Badge tone={paymentTone(o.paymentStatus)}>
                        {findLabel(paymentLabels, o.paymentStatus)}
                      </Badge>
                    </div>
                  </li>
                ))}
              </ul>
            </>
          )}
        </CardContent>
      </Card>

      <ImageLightbox url={lightboxUrl} onClose={() => setLightboxUrl(null)} />
    </div>
  )
}
