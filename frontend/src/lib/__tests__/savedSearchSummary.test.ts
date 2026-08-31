import { describe, expect, it } from 'vitest';
import { buildCategoryBreadcrumb, buildSavedSearchSummary } from '../savedSearchSummary';
import type { SavedSearch } from '../types';

const categories = [
  { id: 'elektronik', name: 'Elektronik', parentId: null },
  { id: 'elektronik.audio', name: 'Audio', parentId: 'elektronik' },
];

function savedSearch(overrides: Partial<SavedSearch>): SavedSearch {
  return {
    id: 'ss_1',
    sellerId: 'sel_1',
    label: 'Label',
    query: '',
    filters: {},
    notify: false,
    lastMatchedAt: null,
    ...overrides,
  };
}

describe('buildCategoryBreadcrumb', () => {
  it('joins parent and leaf with a slash', () => {
    expect(buildCategoryBreadcrumb('elektronik.audio', categories)).toBe('Elektronik/Audio');
  });

  it('returns just the name for a top-level category', () => {
    expect(buildCategoryBreadcrumb('elektronik', categories)).toBe('Elektronik');
  });

  it('returns undefined when no category is set', () => {
    expect(buildCategoryBreadcrumb(undefined, categories)).toBeUndefined();
  });
});

describe('buildSavedSearchSummary', () => {
  it('composes query, category, price bound and postcode/radius using PriceLabel formatting', () => {
    const search = savedSearch({
      query: 'Verstärker',
      filters: { categoryId: 'elektronik.audio', priceMaxCents: 20000, postcode: '10245', radiusKm: 10 },
    });
    const summary = buildSavedSearchSummary(search, buildCategoryBreadcrumb('elektronik.audio', categories));
    expect(summary).toBe('Verstärker, Elektronik/Audio, up to 200,00 €, 10245 within 10 km');
  });

  it('handles every filter member being absent', () => {
    const search = savedSearch({ query: '' });
    expect(buildSavedSearchSummary(search)).toBe('All listings');
  });

  it('handles a priceMaxCents of 0 distinctly from an unset bound', () => {
    const search = savedSearch({ filters: { priceMaxCents: 0 } });
    expect(buildSavedSearchSummary(search)).toContain('up to 0,00 €');
  });
});
