import { useEffect, useState } from 'react'
import { ChevronLeft, ChevronRight, X } from 'lucide-react'
import { useTranslation } from 'react-i18next'

interface Props {
  urls: string[]
}

export function ImageGallery({ urls }: Props) {
  const { t } = useTranslation()
  const [openAt, setOpenAt] = useState<number | null>(null)

  useEffect(() => {
    if (openAt == null) return
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') setOpenAt(null)
      else if (e.key === 'ArrowRight') setOpenAt((i) => (i == null ? i : (i + 1) % urls.length))
      else if (e.key === 'ArrowLeft')
        setOpenAt((i) => (i == null ? i : (i - 1 + urls.length) % urls.length))
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [openAt, urls.length])

  if (urls.length === 0) return null

  return (
    <>
      <div className="flex flex-wrap gap-2">
        {urls.map((url, i) => (
          <button
            key={url}
            type="button"
            onClick={() => setOpenAt(i)}
            className="w-20 h-20 rounded-md border overflow-hidden bg-muted hover:opacity-80"
            aria-label={t('image.aria.zoom', { n: i + 1 })}
          >
            <img
              src={url}
              alt=""
              loading="lazy"
              decoding="async"
              className="w-full h-full object-cover"
            />
          </button>
        ))}
      </div>

      {openAt != null && (
        <div
          className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center"
          onClick={() => setOpenAt(null)}
        >
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation()
              setOpenAt(null)
            }}
            className="absolute top-4 right-4 text-white p-2 rounded-full hover:bg-white/10"
            aria-label={t('btn.close')}
          >
            <X className="h-6 w-6" />
          </button>
          {urls.length > 1 && (
            <>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  setOpenAt((i) => (i == null ? i : (i - 1 + urls.length) % urls.length))
                }}
                className="absolute left-4 text-white p-2 rounded-full hover:bg-white/10"
                aria-label={t('btn.prev')}
              >
                <ChevronLeft className="h-8 w-8" />
              </button>
              <button
                type="button"
                onClick={(e) => {
                  e.stopPropagation()
                  setOpenAt((i) => (i == null ? i : (i + 1) % urls.length))
                }}
                className="absolute right-4 text-white p-2 rounded-full hover:bg-white/10"
                aria-label={t('btn.next')}
              >
                <ChevronRight className="h-8 w-8" />
              </button>
            </>
          )}
          <img
            src={urls[openAt]}
            alt=""
            onClick={(e) => e.stopPropagation()}
            className="max-w-[90vw] max-h-[90vh] object-contain"
          />
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 text-white text-xs bg-black/50 rounded-full px-3 py-1">
            {openAt + 1} / {urls.length}
          </div>
        </div>
      )}
    </>
  )
}
