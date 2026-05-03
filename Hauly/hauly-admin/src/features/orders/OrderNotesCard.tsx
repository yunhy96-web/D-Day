import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Pencil, Trash2 } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { useMe } from '@/features/auth/hooks'
import { ApiError } from '@/lib/api/types'
import {
  useCreateOrderNote,
  useDeleteOrderNote,
  useOrderNotes,
  useUpdateOrderNote,
} from './hooks'
import type { OrderNote } from '@/lib/api/orders'

const MAX_LEN = 2000

export function OrderNotesCard({ orderId }: { orderId: number }) {
  const { t, i18n } = useTranslation()
  const { data: me } = useMe()
  const { data: notes, isLoading, isError } = useOrderNotes(orderId)
  const createMutation = useCreateOrderNote(orderId)

  const [draft, setDraft] = useState('')

  function submit() {
    const body = draft.trim()
    if (!body) return
    createMutation.mutate(body, {
      onSuccess: () => setDraft(''),
    })
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">{t('order.detail.notes.title')}</CardTitle>
      </CardHeader>
      <CardContent className="space-y-3">
        <div className="space-y-2">
          <Textarea
            rows={3}
            placeholder={t('order.notes.placeholder')}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            maxLength={MAX_LEN}
          />
          <div className="flex justify-between items-center">
            <span className="text-xs text-muted-foreground">
              {draft.length}/{MAX_LEN}
            </span>
            <Button
              size="sm"
              onClick={submit}
              disabled={createMutation.isPending || !draft.trim()}
            >
              {t('order.notes.add')}
            </Button>
          </div>
          {createMutation.isError && (
            <Alert variant="destructive">
              <AlertDescription>
                {createMutation.error instanceof ApiError
                  ? createMutation.error.message
                  : t('msg.error.unexpected')}
              </AlertDescription>
            </Alert>
          )}
        </div>

        {isLoading && (
          <p className="text-sm text-muted-foreground">{t('msg.loading')}</p>
        )}
        {isError && (
          <Alert variant="destructive">
            <AlertDescription>{t('msg.error.unexpected')}</AlertDescription>
          </Alert>
        )}
        {notes && notes.length === 0 && !isLoading && (
          <p className="text-sm text-muted-foreground">{t('order.notes.empty')}</p>
        )}
        {notes && notes.length > 0 && (
          <ul className="space-y-3">
            {notes.map((n) => (
              <NoteItem
                key={n.id}
                orderId={orderId}
                note={n}
                isAuthor={me?.id === n.authorId}
                lang={i18n.resolvedLanguage}
              />
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

function NoteItem({
  orderId,
  note,
  isAuthor,
  lang,
}: {
  orderId: number
  note: OrderNote
  isAuthor: boolean
  lang: string | undefined
}) {
  const { t } = useTranslation()
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(note.body)
  const updateMutation = useUpdateOrderNote(orderId)
  const deleteMutation = useDeleteOrderNote(orderId)

  function save() {
    const body = draft.trim()
    if (!body || body === note.body) {
      setEditing(false)
      return
    }
    updateMutation.mutate(
      { noteId: note.id, body },
      { onSuccess: () => setEditing(false) },
    )
  }

  function remove() {
    if (!window.confirm(t('order.notes.delete.confirm'))) return
    deleteMutation.mutate(note.id)
  }

  return (
    <li className="border rounded-md p-3 space-y-2">
      <div className="flex items-center justify-between gap-2 text-xs text-muted-foreground">
        <div className="flex items-center gap-2 min-w-0">
          <span className="font-medium text-foreground truncate">{note.authorName}</span>
          <span>·</span>
          <span className="whitespace-nowrap">
            {new Date(note.createdAt).toLocaleString(lang)}
          </span>
          {note.edited && (
            <>
              <span>·</span>
              <span className="italic">{t('order.notes.edited')}</span>
            </>
          )}
        </div>
        {isAuthor && !editing && (
          <div className="flex items-center gap-1 shrink-0">
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7"
              onClick={() => {
                setDraft(note.body)
                setEditing(true)
              }}
              aria-label={t('order.notes.edit')}
            >
              <Pencil className="h-3.5 w-3.5" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              className="h-7 w-7 text-destructive hover:text-destructive"
              onClick={remove}
              disabled={deleteMutation.isPending}
              aria-label={t('order.notes.delete')}
            >
              <Trash2 className="h-3.5 w-3.5" />
            </Button>
          </div>
        )}
      </div>

      {editing ? (
        <div className="space-y-2">
          <Textarea
            rows={3}
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            maxLength={MAX_LEN}
          />
          <div className="flex justify-end gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setEditing(false)}
              disabled={updateMutation.isPending}
            >
              {t('order.notes.cancel')}
            </Button>
            <Button
              size="sm"
              onClick={save}
              disabled={updateMutation.isPending || !draft.trim()}
            >
              {t('order.notes.save')}
            </Button>
          </div>
          {updateMutation.isError && (
            <Alert variant="destructive">
              <AlertDescription>
                {updateMutation.error instanceof ApiError
                  ? updateMutation.error.message
                  : t('msg.error.unexpected')}
              </AlertDescription>
            </Alert>
          )}
        </div>
      ) : (
        <div className="text-sm whitespace-pre-wrap">{note.body}</div>
      )}

      {deleteMutation.isError && (
        <Alert variant="destructive">
          <AlertDescription>
            {deleteMutation.error instanceof ApiError
              ? deleteMutation.error.message
              : t('msg.error.unexpected')}
          </AlertDescription>
        </Alert>
      )}
    </li>
  )
}
