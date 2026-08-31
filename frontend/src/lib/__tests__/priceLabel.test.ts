import { describe, expect, it } from 'vitest';
import { currencySymbolFor, formatPriceAnnouncement, formatPriceDisplay } from '../priceLabel';

const NBSP = ' ';

describe('formatPriceDisplay — pinned display strings (contracts/ui.md)', () => {
  const cases: Array<[number | null, string]> = [
    [1, `0,01${NBSP}€`],
    [5, `0,05${NBSP}€`],
    [99, `0,99${NBSP}€`],
    [100, `1,00${NBSP}€`],
    [2500, `25,00${NBSP}€`],
    [129900, `1.299,00${NBSP}€`],
    [1000000, `10.000,00${NBSP}€`],
    [0, `0,00${NBSP}€`],
    [null, 'Zu verschenken'],
  ];

  it.each(cases)('priceCents %j -> %j', (priceCents, expected) => {
    expect(formatPriceDisplay(priceCents, 'EUR')).toBe(expected);
  });

  it('contains a literal U+00A0 (not a regular space) before the symbol', () => {
    const display = formatPriceDisplay(2500, 'EUR');
    const symbolIndex = display.indexOf('€');
    expect(display.charCodeAt(symbolIndex - 1)).toBe(0x00a0);
    expect(display).not.toContain(' €'); // regular space + symbol must not appear
  });

  it('0 and null are distinct', () => {
    expect(formatPriceDisplay(0, 'EUR')).not.toBe(formatPriceDisplay(null, 'EUR'));
    expect(formatPriceDisplay(0, 'EUR')).toBe(`0,00${NBSP}€`);
    expect(formatPriceDisplay(null, 'EUR')).toBe('Zu verschenken');
  });

  it('derives the symbol from currency, never hard-coding it', () => {
    expect(formatPriceDisplay(1000, 'EUR')).toContain('€');
  });

  it('falls back to the raw ISO code for an unknown currency without throwing', () => {
    expect(() => formatPriceDisplay(1000, 'XYZ')).not.toThrow();
    expect(formatPriceDisplay(1000, 'XYZ')).toBe(`10,00${NBSP}XYZ`);
  });

  it('does not crash on a negative priceCents (undefined rendering, but no throw)', () => {
    expect(() => formatPriceDisplay(-150, 'EUR')).not.toThrow();
  });
});

describe('formatPriceAnnouncement — pinned accessibility announcements (contracts/ui.md)', () => {
  const cases: Array<[number | null, string]> = [
    [2500, '25 Euro'],
    [129900, '1299 Euro'],
    [1999, '19 Euro 99 Cent'],
    [0, '0 Euro'],
    [null, 'Zu verschenken'],
  ];

  it.each(cases)('priceCents %j -> %j', (priceCents, expected) => {
    expect(formatPriceAnnouncement(priceCents)).toBe(expected);
  });

  it('drops cents for whole amounts', () => {
    expect(formatPriceAnnouncement(2500)).not.toContain('Cent');
  });

  it('never produces an empty, "null", "undefined", "NaN", or bare-number announcement for null', () => {
    const announcement = formatPriceAnnouncement(null);
    expect(announcement).not.toBe('');
    expect(announcement.toLowerCase()).not.toContain('null');
    expect(announcement.toLowerCase()).not.toContain('undefined');
    expect(announcement.toLowerCase()).not.toContain('nan');
    expect(/^\d+$/.test(announcement)).toBe(false);
  });

  it('never produces a lone currency symbol or empty announcement for any priceCents', () => {
    for (const priceCents of [0, 1, 99, 2500, 129900, null]) {
      const announcement = formatPriceAnnouncement(priceCents);
      expect(announcement.trim().length).toBeGreaterThan(0);
      expect(announcement).not.toBe('€');
    }
  });
});

describe('currencySymbolFor', () => {
  it('maps EUR to €', () => {
    expect(currencySymbolFor('EUR')).toBe('€');
  });

  it('falls back to the raw code for an unknown currency', () => {
    expect(currencySymbolFor('USD')).toBe('USD');
  });
});
