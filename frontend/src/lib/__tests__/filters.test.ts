import { describe, expect, it } from 'vitest';
import {
  countActiveFilters,
  describeActiveFilters,
  filtersToSearchParams,
  isValidPostcode,
  isValidPriceRange,
  parseFiltersFromSearchParams,
} from '../filters';

const NBSP = ' ';

describe('parseFiltersFromSearchParams', () => {
  it('reconstructs a full filter set from a URL query string', () => {
    const params = new URLSearchParams(
      'q=plattenspieler&categoryId=elektronik.audio&priceMinCents=0&priceMaxCents=30000&postcode=10437&radiusKm=10',
    );
    expect(parseFiltersFromSearchParams(params)).toEqual({
      q: 'plattenspieler',
      categoryId: 'elektronik.audio',
      priceMinCents: 0,
      priceMaxCents: 30000,
      postcode: '10437',
      radiusKm: 10,
    });
  });

  it('ignores an unknown or malformed parameter rather than erroring', () => {
    const params = new URLSearchParams('postcode=abc&somethingUnknown=1&priceMinCents=notanumber');
    expect(parseFiltersFromSearchParams(params)).toEqual({});
  });

  it('drops radiusKm when postcode is absent, so radius-without-postcode never reaches the backend', () => {
    const params = new URLSearchParams('radiusKm=10');
    expect(parseFiltersFromSearchParams(params)).toEqual({});
  });

  it('drops radiusKm when postcode is malformed', () => {
    const params = new URLSearchParams('postcode=abc&radiusKm=10');
    expect(parseFiltersFromSearchParams(params).radiusKm).toBeUndefined();
  });

  it('round-trips through filtersToSearchParams', () => {
    const original = new URLSearchParams(
      'categoryId=haushalt.moebel&priceMaxCents=0&postcode=12047&radiusKm=5',
    );
    const filters = parseFiltersFromSearchParams(original);
    const rebuilt = filtersToSearchParams(filters);
    expect(rebuilt.toString()).toBe(original.toString());
  });
});

describe('filtersToSearchParams', () => {
  it('omits a filter entirely when unset, never writing an empty value', () => {
    const params = filtersToSearchParams({ categoryId: 'elektronik.audio' });
    expect(params.has('postcode')).toBe(false);
    expect(params.toString()).toBe('categoryId=elektronik.audio');
  });

  it('two equivalent filter sets serialize to string-identical URLs', () => {
    const a = filtersToSearchParams({ postcode: '10437', radiusKm: 10 });
    const b = filtersToSearchParams({ radiusKm: 10, postcode: '10437' });
    expect(a.toString()).toBe(b.toString());
  });
});

describe('countActiveFilters', () => {
  it('counts only the five SavedSearch.filters members, not q', () => {
    expect(countActiveFilters({ q: 'plattenspieler' })).toBe(0);
    expect(
      countActiveFilters({
        categoryId: 'elektronik.audio',
        priceMinCents: 0,
        priceMaxCents: 30000,
        postcode: '10437',
        radiusKm: 10,
      }),
    ).toBe(5);
  });
});

describe('isValidPostcode', () => {
  it('requires exactly 5 digits, preserving leading zeros', () => {
    expect(isValidPostcode('10437')).toBe(true);
    expect(isValidPostcode('01234')).toBe(true);
    expect(isValidPostcode('1234')).toBe(false);
    expect(isValidPostcode('123456')).toBe(false);
    expect(isValidPostcode('abcde')).toBe(false);
  });
});

describe('isValidPriceRange', () => {
  it('rejects priceMinCents > priceMaxCents', () => {
    expect(isValidPriceRange(30000, 10000)).toBe(false);
  });

  it('accepts an equal or ascending range, or a partial range', () => {
    expect(isValidPriceRange(10000, 10000)).toBe(true);
    expect(isValidPriceRange(0, 30000)).toBe(true);
    expect(isValidPriceRange(undefined, 30000)).toBe(true);
    expect(isValidPriceRange(10000, undefined)).toBe(true);
  });
});

describe('describeActiveFilters', () => {
  it('names each active filter using PriceLabel formatting for money', () => {
    const items = describeActiveFilters({ priceMaxCents: 20000, postcode: '10245' });
    expect(items).toEqual([`Price to: 200,00${NBSP}€`, 'Postcode: 10245']);
  });

  it('returns an empty list when no filters are active', () => {
    expect(describeActiveFilters({ q: 'plattenspieler' })).toEqual([]);
  });
});
