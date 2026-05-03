import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

import { getDashboardSummary } from '@/lib/api/orders'
import { formatMoney } from '@/features/orders/money'
import {
  fulfillmentTone,
  findLabel,
  useFulfillmentLabels,
} from '@/features/orders/statusLabels'

export default function DashboardPage() {
  const { t, i18n } = useTranslation()
  const { data, isLoading, isError } = useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: getDashboardSummary,
  })
  const { data: fulfillmentLabels } = useFulfillmentLabels()

  return (
    <div className="container mx-auto py-8 px-4 space-y-4">
      <h1 className="text-2xl font-bold">{t('dashboard.title')}</h1>

      {isLoading && (
        <p className="text-sm text-muted-foreground">{t('msg.loading')}</p>
      )}
      {isError && (
        <p className="text-sm text-destructive">{t('msg.error.unexpected')}</p>
      )}

      {data && (
        <div className="grid gap-4 md:grid-cols-3">
          <Card className="md:col-span-1">
            <CardHeader>
              <CardTitle className="text-base">{t('dashboard.totals.title')}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {Object.keys(data.totalsByCurrency).length === 0 ? (
                <p className="text-sm text-muted-foreground">
                  {t('dashboard.totals.empty')}
                </p>
              ) : (
                <ul className="space-y-1 text-sm">
                  {Object.entries(data.totalsByCurrency).map(([cur, amount]) => (
                    <li key={cur} className="flex items-baseline justify-between">
                      <span className="font-medium text-muted-foreground">{cur}</span>
                      <span className="font-mono">
                        {formatMoney(amount, cur, i18n.resolvedLanguage)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          <Card className="md:col-span-1">
            <CardHeader>
              <CardTitle className="text-base">{t('dashboard.orders.title')}</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-semibold">{data.totalOrderCount}</p>
            </CardContent>
          </Card>

          <Card className="md:col-span-1">
            <CardHeader>
              <CardTitle className="text-base">{t('dashboard.by_status.title')}</CardTitle>
            </CardHeader>
            <CardContent>
              <ul className="space-y-1 text-sm">
                {Object.entries(data.ordersByFulfillmentStatus)
                  .filter(([, count]) => (count ?? 0) > 0)
                  .map(([status, count]) => (
                    <li key={status} className="flex items-center justify-between gap-2">
                      <Badge tone={fulfillmentTone(status as any)}>
                        {findLabel(fulfillmentLabels, status)}
                      </Badge>
                      <span className="font-mono">{count}</span>
                    </li>
                  ))}
                {Object.values(data.ordersByFulfillmentStatus).every((c) => !c) && (
                  <li className="text-muted-foreground">—</li>
                )}
              </ul>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  )
}
