import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { ApiError } from '@/lib/api/types'
import { useChangePassword } from './hooks'

export function ChangePasswordModal({ open, onClose }: { open: boolean; onClose: () => void }) {
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
  }, [open, mutation])

  if (!open) return null

  function submit(e: React.FormEvent) {
    e.preventDefault()
    setLocalError(null)
    if (next.length < 12) {
      setLocalError('새 비밀번호는 최소 12자 이상이어야 합니다.')
      return
    }
    if (next !== confirm) {
      setLocalError('새 비밀번호가 일치하지 않습니다.')
      return
    }
    if (next === current) {
      setLocalError('새 비밀번호가 현재 비밀번호와 같습니다.')
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
        ? '비밀번호 변경에 실패했습니다.'
        : null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-md bg-background border rounded-lg shadow-lg p-5"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-semibold mb-1">비밀번호 변경</h2>
        <p className="text-xs text-muted-foreground mb-4">
          현재 비밀번호 확인 후 새 비밀번호로 교체합니다. 최소 12자.
        </p>

        {success ? (
          <div className="space-y-3">
            <Alert>
              <AlertDescription>비밀번호가 변경되었습니다.</AlertDescription>
            </Alert>
            <div className="flex justify-end">
              <Button onClick={onClose}>닫기</Button>
            </div>
          </div>
        ) : (
          <form onSubmit={submit} className="space-y-3">
            <div className="space-y-1.5">
              <Label htmlFor="cp-current">현재 비밀번호</Label>
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
              <Label htmlFor="cp-new">새 비밀번호 (12자 이상)</Label>
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
              <Label htmlFor="cp-confirm">새 비밀번호 확인</Label>
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
                취소
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? '변경 중…' : '변경'}
              </Button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}
