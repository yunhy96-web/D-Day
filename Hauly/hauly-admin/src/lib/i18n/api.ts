import axios from 'axios'
import i18n from './index'

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api'
const SUPPORTED = ['ko', 'en', 'th'] as const
const loaded = new Set<string>()

/**
 * Fetch UI strings from the backend i18n_message table for the given language
 * and merge them into the i18next bundle. Backend keys override the bundled JSON.
 *
 * Silently no-ops on network errors — the bundled JSON keeps the UI usable
 * even when the backend is unreachable.
 */
export async function loadBackendMessages(lang: string): Promise<void> {
  if (!SUPPORTED.includes(lang as (typeof SUPPORTED)[number])) return
  if (loaded.has(lang)) return
  try {
    const { data } = await axios.get<Record<string, string>>(
      `${baseURL}/i18n/messages`,
      { params: { lang } }
    )
    // Use addResource per key so that dot-notation keys like "order.list.title"
    // are inserted into the nested resource tree (matching the bundled JSON shape).
    for (const [key, value] of Object.entries(data)) {
      i18n.addResource(lang, 'translation', key, value)
    }
    loaded.add(lang)
  } catch {
    // Network or parse error — bundled JSON remains in effect for this lang.
  }
}

/** Subscribe to language changes and lazy-load each new language on first use. */
export function installBackendI18nBridge(): void {
  // Load the initial language right away.
  void loadBackendMessages(i18n.language)
  i18n.on('languageChanged', (lng) => {
    void loadBackendMessages(lng)
  })
}
