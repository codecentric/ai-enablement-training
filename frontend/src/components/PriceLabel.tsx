import { formatPriceAnnouncement, formatPriceDisplay } from '../lib/priceLabel';

export interface PriceLabelProps {
  priceCents: number | null;
  currency: string;
}

/**
 * Renders the pinned display string for the eye, and carries a separate
 * accessible announcement (spoken words, not the display string read
 * literally) via `aria-label`, which overrides how assistive technology
 * announces the element's text content.
 */
export function PriceLabel({ priceCents, currency }: PriceLabelProps) {
  const display = formatPriceDisplay(priceCents, currency);
  const announcement = formatPriceAnnouncement(priceCents);
  return (
    <span className="price-label" aria-label={announcement}>
      {display}
    </span>
  );
}
