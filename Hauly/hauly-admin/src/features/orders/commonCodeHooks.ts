import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { fetchCommonCodes } from '@/lib/api/commonCodes'

/**
 * Fetch a common-code group (e.g. LENS_WEAR_CYCLE) for select-type dynamic fields.
 * Cached per (groupCode, lang) — re-fetched on language switch via the apiClient
 * Accept-Language interceptor.
 */
export function useCommonCodeGroup(groupCode: string | null) {
  const { i18n } = useTranslation()
  const lang = i18n.resolvedLanguage ?? 'ko'
  return useQuery({
    queryKey: ['common-codes', groupCode, lang],
    queryFn: () => fetchCommonCodes(groupCode!),
    enabled: !!groupCode,
    staleTime: 5 * 60 * 1000,
  })
}
