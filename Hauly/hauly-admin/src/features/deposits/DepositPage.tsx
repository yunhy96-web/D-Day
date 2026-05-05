import { useState, type FormEvent } from 'react'
import { createPortal } from 'react-dom'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'

import {
  useDepositSummary,
  useDepositTransactions,
  useRecordDepositAdjustment,
} from './hooks'
import { useIsAdmin } from '@/features/auth/hooks'
import { formatMoney } from '@/features/orders/money'
import type { DepositTransactionKind } from '@/lib/api/deposits'
import { ApiError } from '@/lib/api/types'

const KIND_TONE: Record<DepositTransactionKind, 'success' | 'danger' | 'warning' | 'neutral'> = {
  TOP_UP: 'success',
  PURCHASE: 'danger',
  REFUND: 'warning',
  ADJUSTMENT: 'neutral',
}

export default function DepositPage() {
  const { t, i18n } = useTranslation()
  const isAdmin = useIsAdmin()
  const summary = useDepositSummary()
  const txs = useDepositTransactions({ size: 50 })
  const [adjustOpen, setAdjustOpen] = useState(false)

  const balance = summary.data?.balanceKrw ?? '0'
  const balanceNum = Number(balance)
  const isNegative = Number.isFinite(balanceNum) && balanceNum < 0

  return (
    <div className="p-4 md:p-6 space-y-4 max-w-4xl mx-auto">
      <div className="flex items-center justify-between">
        <h1 className="text-xl md:text-2xl font-semibold">{t('menu.deposits')}</h1>
        {isAdmin && (
          <Button size="sm" onClick={() => setAdjustOpen(true)}>
            {t('deposit.adjust.button')}
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('deposit.balance.label')}</CardTitle>
        </CardHeader>
        <CardContent>
          {summary.isLoading ? (
            <p className="text-sm text-muted-foreground">{t('msg.loading')}</p>
          ) : (
            <>
              <p
                className={
                  isNegative
                    ? 'text-3xl font-semibold text-destructive'
                    : 'text-3xl font-semibold'
                }
              >
                {formatMoney(balance, 'KRW', i18n.resolvedLanguage)}
              </p>
              {isNegative && (
                <p className="text-xs text-muted-foreground mt-2">
                  {t('deposit.negative_hint')}
                </p>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">{t('deposit.title')}</CardTitle>
        </CardHeader>
        <CardContent>
          {txs.isLoading && (
            <p className="text-sm text-muted-foreground">{t('msg.loading')}</p>
          )}
          {txs.data && txs.data.content.length === 0 && (
            <p className="text-sm text-muted-foreground">{t('deposit.tx.empty')}</p>
          )}
          {txs.data && txs.data.content.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-sm min-w-[640px]">
                <thead className="text-xs uppercase text-muted-foreground border-b">
                  <tr>
                    <th className="text-left py-2 px-2">{t('deposit.tx.col.created_at')}</th>
                    <th className="text-left py-2 px-2">{t('deposit.tx.col.kind')}</th>
                    <th className="text-right py-2 px-2">{t('deposit.tx.col.amount')}</th>
                    <th className="text-left py-2 px-2">{t('deposit.tx.col.order')}</th>
                    <th className="text-left py-2 px-2">{t('deposit.tx.col.note')}</th>
                    <th className="text-left py-2 px-2">{t('deposit.tx.col.actor')}</th>
                  </tr>
                </thead>
                <tbody>
                  {txs.data.content.map((tx) => {
                    const amount = Number(tx.amountKrw)
                    const positive = Number.isFinite(amount) && amount >= 0
                    return (
                      <tr key={tx.id} className="border-b">
                        <td className="py-2 px-2 text-muted-foreground whitespace-nowrap">
                          {new Date(tx.createdAt).toLocaleString(i18n.resolvedLanguage)}
                        </td>
                        <td className="py-2 px-2">
                          <Badge tone={KIND_TONE[tx.kind]}>
                            {t(`deposit.tx.kind.${tx.kind.toLowerCase()}`)}
                          </Badge>
                        </td>
                        <td
                          className={
                            'py-2 px-2 text-right font-mono ' +
                            (positive ? 'text-emerald-700' : 'text-destructive')
                          }
                        >
                          {positive ? '+' : ''}
                          {formatMoney(tx.amountKrw, 'KRW', i18n.resolvedLanguage)}
                        </td>
                        <td className="py-2 px-2 font-mono text-xs">
                          {tx.relatedOrderId ? (
                            <Link
                              to={`/orders/${tx.relatedOrderId}`}
                              className="text-primary hover:underline"
                            >
                              {tx.relatedOrderNo ?? `#${tx.relatedOrderId}`}
                            </Link>
                          ) : (
                            '-'
                          )}
                        </td>
                        <td className="py-2 px-2 text-xs whitespace-pre-wrap">
                          {tx.note ?? '-'}
                        </td>
                        <td className="py-2 px-2 text-xs text-muted-foreground">
                          {tx.createdByName ?? '-'}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {adjustOpen && <AdjustModal onClose={() => setAdjustOpen(false)} />}
    </div>
  )
}

function AdjustModal({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation()
  // 부호와 절대값을 분리해서 모바일 키패드에 - 키가 없어도 토글 버튼으로 음수 입력 가능.
  const [sign, setSign] = useState<'+' | '-'>('+')
  const [amount, setAmount] = useState('')
  const [note, setNote] = useState('')
  const mutation = useRecordDepositAdjustment()

  function submit(e: FormEvent) {
    e.preventDefault()
    // 콤마/공백 제거 — input은 숫자 키패드라 보통 안 들어오지만 보호용.
    const digitsOnly = amount.replace(/[^\d]/g, '')
    if (!digitsOnly || !note.trim()) return
    const parsed = Number(digitsOnly)
    if (!Number.isFinite(parsed) || parsed === 0) return
    const signed = sign === '-' ? `-${digitsOnly}` : digitsOnly
    mutation.mutate({ amountKrw: signed, note: note.trim() }, { onSuccess: onClose })
  }

  function errorMessage(): string {
    if (!(mutation.error instanceof ApiError)) return t('msg.error.unexpected')
    if (mutation.error.message === 'deposit_amount_invalid') {
      return t('msg.error.deposit_amount_invalid')
    }
    return mutation.error.message
  }

  // Portal로 body에 직접 마운트 — sticky 헤더 등 부모 stacking context의 영향을 받지 않게.
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
        <div className="text-base font-semibold">{t('deposit.adjust.title')}</div>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">
            {t('deposit.adjust.amount')}
          </label>
          <div className="flex gap-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setSign((s) => (s === '+' ? '-' : '+'))}
              aria-label={sign === '+' ? '+ → −' : '− → +'}
              className={
                'shrink-0 h-10 w-12 text-lg font-bold ' +
                (sign === '-'
                  ? 'bg-destructive/10 text-destructive border-destructive/40 hover:bg-destructive/20'
                  : 'bg-emerald-50 text-emerald-700 border-emerald-300 hover:bg-emerald-100')
              }
            >
              {sign === '+' ? '+' : '−'}
            </Button>
            <Input
              type="text"
              inputMode="numeric"
              pattern="[0-9]*"
              value={amount}
              onChange={(e) => setAmount(e.target.value.replace(/[^\d]/g, ''))}
              placeholder="100000"
              required
            />
          </div>
          <p className="text-[11px] text-muted-foreground">
            {t('deposit.adjust.amount.help')}
          </p>
        </div>
        <div className="space-y-1">
          <label className="text-xs text-muted-foreground">
            {t('deposit.adjust.note')}
          </label>
          <Textarea
            rows={3}
            value={note}
            onChange={(e) => setNote(e.target.value)}
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
          <Button type="submit" size="sm" disabled={mutation.isPending}>
            {mutation.isPending ? t('msg.loading') : t('deposit.adjust.confirm')}
          </Button>
        </div>
      </form>
    </div>,
    document.body,
  )
}
