import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/utils'

const LANGUAGES = [
  { code: 'ko', key: 'lang.ko' },
  { code: 'en', key: 'lang.en' },
  { code: 'th', key: 'lang.th' },
] as const

export function LanguageSwitcher({ className }: { className?: string }) {
  const { t, i18n } = useTranslation()
  return (
    <div className={cn('flex gap-1', className)}>
      {LANGUAGES.map(({ code, key }) => (
        <button
          key={code}
          type="button"
          onClick={() => i18n.changeLanguage(code)}
          className={cn(
            'text-xs px-2 py-1 rounded transition-colors',
            i18n.resolvedLanguage === code
              ? 'font-semibold text-primary'
              : 'text-muted-foreground hover:text-foreground'
          )}
        >
          {t(key)}
        </button>
      ))}
    </div>
  )
}
