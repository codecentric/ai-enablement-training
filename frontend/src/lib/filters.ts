// FilterPanel <-> URL reflection. The filters are exactly the members of
// `SavedSearch.filters` in domain.md, plus the free-text `q`
// (contracts/ui.md, "Component: FilterPanel").
//
// The URL is the single source of truth for filter state: parse it to
// render the panel, serialize the panel back to it. Clearing a filter
// removes its parameter rather than writing an empty value. An unknown or
// malformed parameter is dropped rather than causing an error.

import { formatPriceDisplay } from './priceLabel';
import type { SearchFilters } from './types';

const CATEGORY_ID_PATTERN = /^[a-z0-9]+(\.[a-z0-9]+)?$/;
const POSTCODE_PATTERN = /^\d{5}$/;
const NON_NEGATIVE_INT_PATTERN = /^\d+$/;

export function parseFiltersFromSearchParams(params: URLSearchParams): SearchFilters {
  const filters: SearchFilters = {};

  const q = params.get('q');
  if (q && q.trim() !== '') filters.q = q.slice(0, 200);

  const categoryId = params.get('categoryId');
  if (categoryId && CATEGORY_ID_PATTERN.test(categoryId)) filters.categoryId = categoryId;

  const priceMinCents = params.get('priceMinCents');
  if (priceMinCents !== null && NON_NEGATIVE_INT_PATTERN.test(priceMinCents)) {
    filters.priceMinCents = parseInt(priceMinCents, 10);
  }

  const priceMaxCents = params.get('priceMaxCents');
  if (priceMaxCents !== null && NON_NEGATIVE_INT_PATTERN.test(priceMaxCents)) {
    filters.priceMaxCents = parseInt(priceMaxCents, 10);
  }

  const postcode = params.get('postcode');
  if (postcode && POSTCODE_PATTERN.test(postcode)) filters.postcode = postcode;

  // radiusKm requires postcode. A URL that carries radiusKm without a valid
  // postcode is malformed for our purposes; drop radiusKm rather than ever
  // sending `radius-without-postcode` to the backend.
  const radiusKm = params.get('radiusKm');
  if (radiusKm !== null && NON_NEGATIVE_INT_PATTERN.test(radiusKm) && filters.postcode) {
    filters.radiusKm = parseInt(radiusKm, 10);
  }

  return filters;
}

export function filtersToSearchParams(filters: SearchFilters): URLSearchParams {
  const params = new URLSearchParams();
  if (filters.q) params.set('q', filters.q);
  if (filters.categoryId) params.set('categoryId', filters.categoryId);
  if (filters.priceMinCents !== undefined) params.set('priceMinCents', String(filters.priceMinCents));
  if (filters.priceMaxCents !== undefined) params.set('priceMaxCents', String(filters.priceMaxCents));
  if (filters.postcode) params.set('postcode', filters.postcode);
  if (filters.radiusKm !== undefined && filters.postcode) params.set('radiusKm', String(filters.radiusKm));
  return params;
}

export function countActiveFilters(filters: SearchFilters): number {
  let count = 0;
  if (filters.categoryId) count++;
  if (filters.priceMinCents !== undefined) count++;
  if (filters.priceMaxCents !== undefined) count++;
  if (filters.postcode) count++;
  if (filters.radiusKm !== undefined) count++;
  return count;
}

export function isValidPostcode(postcode: string): boolean {
  return POSTCODE_PATTERN.test(postcode);
}

export function isValidPriceRange(minCents?: number, maxCents?: number): boolean {
  if (minCents === undefined || maxCents === undefined) return true;
  return minCents <= maxCents;
}

/**
 * Human-readable descriptions of the currently applied filters, for the
 * Empty state ("must name which filters are currently applied") and any
 * "clear all" affordance.
 */
export function describeActiveFilters(filters: SearchFilters, categoryLabel?: string): string[] {
  const items: string[] = [];
  if (filters.categoryId) items.push(`Category: ${categoryLabel ?? filters.categoryId}`);
  if (filters.priceMinCents !== undefined) items.push(`Price from: ${formatPriceDisplay(filters.priceMinCents, 'EUR')}`);
  if (filters.priceMaxCents !== undefined) items.push(`Price to: ${formatPriceDisplay(filters.priceMaxCents, 'EUR')}`);
  if (filters.postcode) items.push(`Postcode: ${filters.postcode}`);
  if (filters.radiusKm !== undefined) items.push(`Radius: ${filters.radiusKm} km`);
  return items;
}
