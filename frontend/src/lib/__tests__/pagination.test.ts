import { describe, expect, it } from 'vitest';
import { appendPageDeduped } from '../pagination';

interface Item {
  id: string;
  label: string;
}

describe('appendPageDeduped', () => {
  it('appends a fresh page in order', () => {
    const existing: Item[] = [{ id: 'a', label: 'A' }];
    const incoming: Item[] = [
      { id: 'b', label: 'B' },
      { id: 'c', label: 'C' },
    ];
    expect(appendPageDeduped(existing, incoming)).toEqual([
      { id: 'a', label: 'A' },
      { id: 'b', label: 'B' },
      { id: 'c', label: 'C' },
    ]);
  });

  it('drops an id already present, keeping the first occurrence', () => {
    const existing: Item[] = [{ id: 'a', label: 'first' }];
    const incoming: Item[] = [
      { id: 'a', label: 'duplicate-should-be-dropped' },
      { id: 'b', label: 'B' },
    ];
    const result = appendPageDeduped(existing, incoming);
    expect(result).toEqual([
      { id: 'a', label: 'first' },
      { id: 'b', label: 'B' },
    ]);
  });

  it('drops duplicates within the incoming page itself', () => {
    const existing: Item[] = [];
    const incoming: Item[] = [
      { id: 'a', label: 'first' },
      { id: 'a', label: 'second-should-be-dropped' },
    ];
    expect(appendPageDeduped(existing, incoming)).toEqual([{ id: 'a', label: 'first' }]);
  });

  it('tolerates a page returning fewer than the requested limit without special-casing it', () => {
    const existing: Item[] = [{ id: 'a', label: 'A' }];
    const incoming: Item[] = [{ id: 'b', label: 'B' }]; // fewer than e.g. limit=20
    expect(appendPageDeduped(existing, incoming)).toHaveLength(2);
  });

  it('handles an empty incoming page (end of results) without error', () => {
    const existing: Item[] = [{ id: 'a', label: 'A' }];
    expect(appendPageDeduped(existing, [])).toEqual(existing);
  });
});
