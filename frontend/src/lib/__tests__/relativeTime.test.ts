import { describe, expect, it } from 'vitest';
import { preciseAge, relativeAge } from '../relativeTime';

describe('relativeAge', () => {
  const now = new Date('2026-08-31T12:00:00Z');

  it('renders today', () => {
    expect(relativeAge('2026-08-31T06:00:00Z', now)).toBe('today');
  });

  it('renders yesterday', () => {
    expect(relativeAge('2026-08-30T23:00:00Z', now)).toBe('yesterday');
  });

  it('renders a small number of days', () => {
    expect(relativeAge('2026-08-28T12:00:00Z', now)).toBe('3 days ago');
  });

  it('renders weeks once past 6 days', () => {
    expect(relativeAge('2026-08-17T12:00:00Z', now)).toBe('2 weeks ago');
  });

  it('never returns a raw timestamp', () => {
    const result = relativeAge('2026-08-28T12:00:00Z', now);
    expect(result).not.toContain('T');
    expect(result).not.toContain('Z');
  });
});

describe('preciseAge', () => {
  const now = new Date('2026-08-31T12:00:00Z');

  it('states a concrete duration, not a vague marker', () => {
    expect(preciseAge(now.getTime() - 2 * 60 * 60 * 1000, now)).toBe('2 hours ago');
    expect(preciseAge(now.getTime() - 5 * 60 * 1000, now)).toBe('5 minutes ago');
  });
});
