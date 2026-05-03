/**
 * Format a money string (already a fixed-point number from the backend) using Intl.
 * KRW has no decimals; THB and USD use 2.
 * Falls back to "{currency} {amount}" if the runtime can't render the currency.
 */
export function formatMoney(
  amount: string | number,
  currency: string,
  locale: string | undefined,
): string {
  const value = typeof amount === 'string' ? Number(amount) : amount
  if (!Number.isFinite(value)) return `${currency} ${amount}`
  try {
    const fractionDigits = currency === 'KRW' ? 0 : 2
    return new Intl.NumberFormat(locale ?? 'ko', {
      style: 'currency',
      currency,
      maximumFractionDigits: fractionDigits,
      minimumFractionDigits: fractionDigits,
    }).format(value)
  } catch {
    return `${currency} ${value.toLocaleString(locale ?? 'ko')}`
  }
}
