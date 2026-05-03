/**
 * Curated list of countries shown in the shipping-address dropdown.
 * Optimized for the Korea→Southeast Asia proxy buying use case;
 * additional codes can be added as needed.
 */
export const COUNTRY_CODES = [
  'TH', 'KR', 'JP', 'VN', 'SG', 'MY', 'ID', 'PH', 'TW', 'HK', 'CN', 'US',
] as const

export type CountryCode = (typeof COUNTRY_CODES)[number]

/**
 * Returns the localized display name for a country code, e.g.
 * countryDisplayName('TH', 'ko') → '태국', ('en') → 'Thailand', ('th') → 'ไทย'.
 * Falls back to the raw code if Intl.DisplayNames isn't available or the code is unknown.
 */
export function countryDisplayName(code: string, lang: string | undefined): string {
  if (!code) return ''
  try {
    const dn = new Intl.DisplayNames([lang ?? 'ko'], { type: 'region' })
    return dn.of(code) ?? code
  } catch {
    return code
  }
}
