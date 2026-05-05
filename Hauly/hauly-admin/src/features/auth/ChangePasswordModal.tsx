import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { ApiError } from '@/lib/api/types'
import { useChangePassword } from './hooks'

export function ChangePasswordModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { t } = useTranslation()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const mutation = useChangePassword()

  useEffect(() => {
    if (!open) {
      setCurrent('')
      setNext('')
      setConfirm('')
      setLocalError(null)
      setSuccess(false)
      mutation.reset()
    }
    // mutation 객체는 매 렌더마다 새 참조라 deps에 넣으면 무한 루프가 발생함.
    // 닫힘 전이 시점에만 reset하면 충분하므로 open만 의존.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open])

  if (!open) return null

  function submit(e: React.FormEvent) {
    e.preventDefault()
    setLocalError(null)
    if (next.length < 4) {
      setLocalError(t('pw.error.too_short'))
      return
    }
    if (next !== confirm) {
      setLocalError(t('pw.error.mismatch'))
      return
    }
    if (next === current) {
      setLocalError(t('pw.error.same_as_current'))
      return
    }
    mutation.mutate(
      { currentPassword: current, newPassword: next },
      { onSuccess: () => setSuccess(true) },
    )
  }

  const apiErrorMsg =
    mutation.error instanceof ApiError
      ? mutation.error.message
      : mutation.isError
        ? t('pw.error.failed')
        : null

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md bg-background border rounded-lg shadow-lg p-5"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-semibold mb-1">{t('pw.title')}</h2>
        <p className="text-xs text-muted-foreground mb-4">{t('pw.help')}</p>

        {success ? (
          <div className="space-y-3">
            <Alert>
              <AlertDescription>{t('pw.success')}</AlertDescription>
            </Alert>
            <div className="flex justify-end">
              <Button onClick={onClose}>{t('btn.close')}</Button>
            </div>
          </div>
        ) : (
          <form onSubmit={submit} className="space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="cp-current">{t('pw.field.current')}</Label>
              <Input
                id="cp-current"
                type="password"
                autoComplete="current-password"
                value={current}
                onChange={(e) => setCurrent(e.target.value)}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="cp-new">{t('pw.field.new')}</Label>
              <Input
                id="cp-new"
                type="password"
                autoComplete="new-password"
                value={next}
                onChange={(e) => setNext(e.target.value)}
                required
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="cp-confirm">{t('pw.field.confirm')}</Label>
              <Input
                id="cp-confirm"
                type="password"
                autoComplete="new-password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                required
              />
            </div>

            {(localError || apiErrorMsg) && (
              <Alert variant="destructive">
                <AlertDescription>{localError ?? apiErrorMsg}</AlertDescription>
              </Alert>
            )}

            <div className="flex justify-end gap-2 pt-1">
              <Button type="button" variant="outline" onClick={onClose} disabled={mutation.isPending}>
                {t('btn.cancel')}
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? t('pw.btn.changing') : t('pw.btn.change')}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>,
    document.body,
  )
}
