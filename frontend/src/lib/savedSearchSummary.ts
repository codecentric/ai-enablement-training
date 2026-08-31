// Human-readable summary of a saved search's query + filters, e.g.
// "Verstärker, Elektronik/Audio, up to 200,00 €, 10245 within 10 km"
// (contracts/ui.md, SavedSearchRow). Prices use PriceLabel's display format.

import { formatPriceDisplay } from './priceLabel';
import type { SavedSearch } from './types';

export function buildSavedSearchSummary(search: SavedSearch, categoryBreadcrumb?: string): string {
  const parts: string[] = [];

  if (search.query && search.query.trim() !== '') parts.push(search.query);
  if (categoryBreadcrumb) parts.push(categoryBreadcrumb);

  const { priceMinCents, priceMaxCents, postcode, radiusKm } = search.filters;

  if (priceMinCents !== undefined && priceMaxCents !== undefined) {
    parts.push(`${formatPriceDisplay(priceMinCents, 'EUR')} to ${formatPriceDisplay(priceMaxCents, 'EUR')}`);
  } else if (priceMaxCents !== undefined) {
    parts.push(`up to ${formatPriceDisplay(priceMaxCents, 'EUR')}`);
  } else if (priceMinCents !== undefined) {
    parts.push(`from ${formatPriceDisplay(priceMinCents, 'EUR')}`);
  }

  if (postcode && radiusKm !== undefined) {
    parts.push(`${postcode} within ${radiusKm} km`);
  } else if (postcode) {
    parts.push(postcode);
  }

  return parts.length > 0 ? parts.join(', ') : 'All listings';
}

export function buildCategoryBreadcrumb(
  categoryId: string | undefined,
  categories: { id: string; name: string; parentId: string | null }[],
): string | undefined {
  if (!categoryId) return undefined;
  const byId = new Map(categories.map((c) => [c.id, c]));
  const node = byId.get(categoryId);
  if (!node) return undefined;
  if (node.parentId === null) return node.name;
  const parent = byId.get(node.parentId);
  return parent ? `${parent.name}/${node.name}` : node.name;
}
