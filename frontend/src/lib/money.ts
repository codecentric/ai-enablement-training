// Money is always an integer number of cents (domain.md invariant 3). No
// floating-point arithmetic anywhere in this module.

/**
 * Converts a user-typed euro string (e.g. "19,99", "19.99", "5", "") into
 * integer cents using integer arithmetic on the parsed digits — never
 * `Math.round(parseFloat(x) * 100)`, which turns "19.99" into 1998 cents
 * often enough to matter (see contracts/ui.md, "Money in the filter inputs").
 *
 * Returns `null` when the input is empty or not a valid non-negative euro
 * amount (at most 2 decimal digits).
 */
export function eurosStringToCents(raw: string): number | null {
  const trimmed = raw.trim();
  if (trimmed === '') return null;
  const match = /^(\d+)(?:[.,](\d{1,2}))?$/.exec(trimmed);
  if (!match) return null;
  const euros = parseInt(match[1], 10);
  const centsFraction = (match[2] ?? '').padEnd(2, '0');
  const cents = parseInt(centsFraction, 10);
  return euros * 100 + cents;
}

/**
 * Splits an integer number of cents into whole euros and the remaining
 * cents, via integer division and modulo. Never touches a float.
 */
export function centsToEuroParts(priceCents: number): { euros: number; cents: number } {
  const euros = Math.trunc(priceCents / 100);
  const cents = priceCents % 100;
  return { euros, cents };
}

/**
 * Formats integer cents back into a euro string for an input field, e.g.
 * `1999 -> "19,99"`. Built from the integer parts, not a float division.
 */
export function centsToEurosInputString(priceCents: number): string {
  const { euros, cents } = centsToEuroParts(priceCents);
  return `${euros},${Math.abs(cents).toString().padStart(2, '0')}`;
}
