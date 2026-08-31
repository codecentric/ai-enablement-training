import { describe, expect, it } from 'vitest';
import { growVisibleCount, initialVisibleCount } from '../windowing';

describe('initialVisibleCount', () => {
  it('caps the initial window at the total when the list is short', () => {
    expect(initialVisibleCount(3)).toBe(3);
  });

  it('uses the default window size when the list is long enough', () => {
    expect(initialVisibleCount(12)).toBe(6);
  });

  it('honours a custom initial size', () => {
    expect(initialVisibleCount(12, 4)).toBe(4);
  });

  it('handles an empty list', () => {
    expect(initialVisibleCount(0)).toBe(0);
  });
});

describe('growVisibleCount', () => {
  it('grows by the step', () => {
    expect(growVisibleCount(6, 12)).toBe(12);
  });

  it('caps growth at the total rather than overshooting', () => {
    expect(growVisibleCount(10, 12)).toBe(12);
  });

  it('is a no-op once everything is already visible', () => {
    expect(growVisibleCount(12, 12)).toBe(12);
  });

  it('honours a custom step', () => {
    expect(growVisibleCount(0, 12, 5)).toBe(5);
  });
});
