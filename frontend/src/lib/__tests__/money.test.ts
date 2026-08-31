import { describe, expect, it } from 'vitest';
import { centsToEuroParts, centsToEurosInputString, eurosStringToCents } from '../money';

describe('eurosStringToCents — integer digit arithmetic, never parseFloat * 100', () => {
  it('converts whole euros', () => {
    expect(eurosStringToCents('5')).toBe(500);
    expect(eurosStringToCents('0')).toBe(0);
  });

  it('converts euros with two decimal digits', () => {
    expect(eurosStringToCents('19,99')).toBe(1999);
    expect(eurosStringToCents('19.99')).toBe(1999);
  });

  it('pads a single decimal digit', () => {
    expect(eurosStringToCents('19,9')).toBe(1990);
  });

  it('handles large amounts without float drift', () => {
    expect(eurosStringToCents('1299,00')).toBe(129900);
    expect(eurosStringToCents('10000')).toBe(1000000);
  });

  it('returns null for empty input', () => {
    expect(eurosStringToCents('')).toBeNull();
    expect(eurosStringToCents('   ')).toBeNull();
  });

  it('returns null for malformed input', () => {
    expect(eurosStringToCents('abc')).toBeNull();
    expect(eurosStringToCents('19,999')).toBeNull();
    expect(eurosStringToCents('-5')).toBeNull();
  });

  it('never produces the float-rounding artifact 1998 for "19.99"', () => {
    // 19.99 * 100 is 1999.0000000000002 in IEEE-754 double precision; a naive
    // Math.round(parseFloat(x) * 100) can drift. This must always be exact.
    expect(eurosStringToCents('19.99')).toBe(1999);
    expect(eurosStringToCents('0.10')).toBe(10);
    expect(eurosStringToCents('0.20')).toBe(20);
    expect(eurosStringToCents('0.30')).toBe(30);
  });
});

describe('centsToEuroParts — integer division and modulo', () => {
  it('splits euros and cents', () => {
    expect(centsToEuroParts(1999)).toEqual({ euros: 19, cents: 99 });
    expect(centsToEuroParts(100)).toEqual({ euros: 1, cents: 0 });
    expect(centsToEuroParts(0)).toEqual({ euros: 0, cents: 0 });
    expect(centsToEuroParts(129900)).toEqual({ euros: 1299, cents: 0 });
  });
});

describe('centsToEurosInputString', () => {
  it('formats cents back into an editable euro string', () => {
    expect(centsToEurosInputString(1999)).toBe('19,99');
    expect(centsToEurosInputString(100)).toBe('1,00');
    expect(centsToEurosInputString(5)).toBe('0,05');
  });
});
