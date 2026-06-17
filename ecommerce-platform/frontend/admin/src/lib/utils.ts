import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** Merge conditional class names, resolving Tailwind conflicts. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** Format a decimal amount + ISO currency code for display. */
export function formatMoney(amount: number | string, currency = 'USD') {
  const value = typeof amount === 'string' ? Number(amount) : amount
  if (Number.isNaN(value)) return String(amount)
  try {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
    }).format(value)
  } catch {
    // Unknown currency code — fall back to a plain number + code.
    return `${value.toFixed(2)} ${currency}`
  }
}

/** Human-friendly relative-ish date for order/notification timestamps. */
export function formatDate(iso: string | undefined) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(d)
}
