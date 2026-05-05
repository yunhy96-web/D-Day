import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { useTranslation } from 'react-i18next'

/** Single-image full-screen preview. Pass null to keep closed. */
export function ImageLightbox({ url, onClose }: { url: string | null; onClose: () => void }) {
  const { t } = useTranslation()

  useEffect(() => {
    if (url == null) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [url, onClose])

  if (url == null) return null

  return createPortal(
    <div
      className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center"
      onClick={onClose}
    >
      <button
        type="button"
        onClick={(e) => {
          e.stopPropagation()
          onClose()
        }}
        className="absolute top-4 right-4 text-white p-2 rounded-full hover:bg-white/10"
        aria-label={t('btn.close')}
      >
        <X className="h-6 w-6" />
      </button>
      <img
        src={url}
        alt=""
        onClick={(e) => e.stopPropagation()}
        className="max-w-[90vw] max-h-[90vh] object-contain"
      />
    </div>,
    document.body,
  )
}
