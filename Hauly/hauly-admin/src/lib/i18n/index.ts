import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

// Bundled JSON acts as a bootstrap fallback: it renders immediately on app start
// (before the backend fetch resolves) and as a safety net when the backend is
// unreachable. The backend bridge in ./api.ts merges /api/i18n/messages on top
// for the active language, so DB rows always win once they arrive.
import ko from './locales/ko.json'
import en from './locales/en.json'
import th from './locales/th.json'

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      ko: { translation: ko },
      en: { translation: en },
      th: { translation: th },
    },
    lng: 'ko',
    fallbackLng: 'ko',
    interpolation: {
      escapeValue: false, // React already escapes
    },
    detection: {
      order: ['localStorage', 'navigator'],
      lookupLocalStorage: 'hauly.lang',
      caches: ['localStorage'],
    },
  })

export default i18n
