const MONEY = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR' });

export function formatMoney(value: number | null | undefined): string {
  return MONEY.format(value ?? 0);
}

/** 2026-03-16 as "16 Mar 2026" (the value is a calendar date, so no time zone conversion is applied). */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  const [year, month, day] = iso.substring(0, 10).split('-').map(Number);
  return new Date(year, month - 1, day).toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

/** The calendar date of a date-time in ISO format, as "16 Mar 2026". */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) {
    return '';
  }
  return new Date(iso).toLocaleString('en-GB', { dateStyle: 'medium', timeStyle: 'short' });
}

export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** "Ada Adams" as "AA"; at most two letters. */
export function initialsOf(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('');
}
