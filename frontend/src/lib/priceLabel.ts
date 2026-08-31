// PriceLabel — the pinned acceptance surface. The strings this module
// produces MUST match contracts/ui.md exactly, including the literal
// non-breaking space (U+00A0) between the amount and the currency symbol.
//
// Built from the integer priceCents via integer division/modulo only. No
// floating-point arithmetic anywhere in this module.

import { centsToEuroParts } from './money';

const NBSP = ' ';

const CURRENCY_SYMBOLS: Record<string, string> = {
  EUR: '€',
};

/**
 * The currency symbol for a `Listing.currency` code. Always derived from the
 * field, never hard-coded to "€". Falls back to the raw ISO code for an
 * unknown currency rather than throwing (contracts/ui.md, "Out of contract").
 */
export function currencySymbolFor(currency: string): string {
  return CURRENCY_SYMBOLS[currency] ?? currency;
}

/**
 * The display string for the eye: de-DE grouping, always two decimals, the
 * symbol after the amount separated by a non-breaking space.
 *
 * `priceCents: null` means "Zu verschenken" (posted without a price).
 * `priceCents: 0` means a literal price of zero and is NOT the same thing —
 * see contracts/ui.md, "Why 0 and null differ".
 */
export function formatPriceDisplay(priceCents: number | null, currency: string): string {
  if (priceCents === null) return 'Zu verschenken';

  const { euros, cents } = centsToEuroParts(priceCents);
  // toLocaleString('de-DE') on an integer euro count only performs grouping
  // (thousands '.'); it never introduces rounding error because `euros` is
  // already an exact integer from integer division.
  const euroGrouped = Math.abs(euros).toLocaleString('de-DE');
  const sign = euros < 0 || (euros === 0 && cents < 0) ? '-' : '';
  const centsPadded = Math.abs(cents).toString().padStart(2, '0');

  return `${sign}${euroGrouped},${centsPadded}${NBSP}${currencySymbolFor(currency)}`;
}

/**
 * The accessible announcement: spoken words, not the display string read
 * literally. Whole amounts drop the cents. `null` is announced as the same
 * phrase the eye sees — never an empty string, "null", "undefined", "NaN",
 * a bare number, or a lone currency symbol.
 */
export function formatPriceAnnouncement(priceCents: number | null): string {
  if (priceCents === null) return 'Zu verschenken';

  const { euros, cents } = centsToEuroParts(priceCents);
  if (cents === 0) return `${euros} Euro`;
  return `${euros} Euro ${Math.abs(cents)} Cent`;
}
