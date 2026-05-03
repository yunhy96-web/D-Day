import { useRef, useState, useCallback } from 'react'
import imageCompression from 'browser-image-compression'
import { ImagePlus, X, Loader2 } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { uploadTempImage, deleteTempImage } from '@/lib/api/uploads'

const MAX_IMAGES = 5
/** Compress anything larger than this (in MB) before upload. */
const COMPRESS_THRESHOLD_MB = 5
const ACCEPT = 'image/jpeg,image/png,image/webp,image/heic,image/heif'

export interface UploadedImage {
  tempKey: string
  url: string
  /** Local object URL for thumbnail before the upload finishes. */
  previewUrl: string
}

interface Props {
  /** Currently uploaded images (controlled). */
  value: UploadedImage[]
  onChange: (next: UploadedImage[]) => void
}

/**
 * Per-item image uploader. Picks files, compresses any over 5MB on the client
 * (max 2000px, jpeg quality 0.85), uploads each to the temp store, and exposes
 * the resulting tempKeys via {@code onChange}. Up to 5 images per item.
 */
export function ImageUploader({ value, onChange }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [busy, setBusy] = useState(0)
  const [error, setError] = useState<string | null>(null)

  const handleFiles = useCallback(
    async (files: FileList | null) => {
      if (!files || files.length === 0) return
      setError(null)

      const remaining = MAX_IMAGES - value.length
      if (remaining <= 0) {
        setError(`최대 ${MAX_IMAGES}장까지 업로드 가능합니다.`)
        return
      }

      const picked = Array.from(files).slice(0, remaining)
      setBusy((n) => n + picked.length)

      const uploaded: UploadedImage[] = []
      for (const raw of picked) {
        try {
          const file =
            raw.size > COMPRESS_THRESHOLD_MB * 1024 * 1024
              ? await imageCompression(raw, {
                  maxSizeMB: COMPRESS_THRESHOLD_MB,
                  maxWidthOrHeight: 2000,
                  initialQuality: 0.85,
                  useWebWorker: true,
                })
              : raw
          const previewUrl = URL.createObjectURL(file)
          const { tempKey, url } = await uploadTempImage(file)
          uploaded.push({ tempKey, url, previewUrl })
        } catch (e: any) {
          setError(`업로드 실패: ${raw.name} (${e?.message ?? 'unknown'})`)
        } finally {
          setBusy((n) => n - 1)
        }
      }
      if (uploaded.length > 0) onChange([...value, ...uploaded])
    },
    [value, onChange],
  )

  async function remove(idx: number) {
    const img = value[idx]
    onChange(value.filter((_, i) => i !== idx))
    URL.revokeObjectURL(img.previewUrl)
    try {
      await deleteTempImage(img.tempKey)
    } catch {
      // best-effort: orphaned temps are cleaned by the 24h cron
    }
  }

  const canAddMore = value.length + busy < MAX_IMAGES

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-2">
        {value.map((img, i) => (
          <div key={img.tempKey} className="relative w-20 h-20 rounded-md border overflow-hidden bg-muted">
            <img
              src={img.previewUrl || img.url}
              alt=""
              className="w-full h-full object-cover"
            />
            <button
              type="button"
              onClick={() => remove(i)}
              className="absolute top-0.5 right-0.5 bg-black/60 text-white rounded-full p-0.5 hover:bg-black/80"
              aria-label="삭제"
            >
              <X className="h-3 w-3" />
            </button>
          </div>
        ))}
        {Array.from({ length: busy }).map((_, i) => (
          <div
            key={`busy-${i}`}
            className="w-20 h-20 rounded-md border bg-muted flex items-center justify-center"
          >
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        ))}
        {canAddMore && (
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="w-20 h-20 flex flex-col items-center justify-center"
            onClick={() => inputRef.current?.click()}
          >
            <ImagePlus className="h-5 w-5" />
            <span className="text-[10px] mt-0.5">{value.length + busy}/{MAX_IMAGES}</span>
          </Button>
        )}
      </div>
      <input
        ref={inputRef}
        type="file"
        accept={ACCEPT}
        multiple
        className="hidden"
        onChange={(e) => {
          handleFiles(e.target.files)
          e.target.value = ''
        }}
      />
      {error && <p className="text-xs text-destructive">{error}</p>}
    </div>
  )
}
