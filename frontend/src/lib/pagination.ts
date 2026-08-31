// Cursor pagination is not guaranteed to be stable across pages
// (contracts/ui.md, "Ordering across pages is not guaranteed"). The client
// compensates by de-duplicating appended pages by `Listing.id`, keeping the
// first occurrence, and never keying list items by index.

export interface Identified {
  id: string;
}

/**
 * Appends `incoming` to `existing`, de-duplicating by `id` and keeping the
 * first occurrence of any id that appears in both. Safe to call with a page
 * that returned fewer than `limit` items — that is not an end signal, only
 * `nextCursor: null` is.
 */
export function appendPageDeduped<T extends Identified>(existing: T[], incoming: T[]): T[] {
  const seenIds = new Set(existing.map((item) => item.id));
  const appended: T[] = [];
  for (const item of incoming) {
    if (seenIds.has(item.id)) continue;
    seenIds.add(item.id);
    appended.push(item);
  }
  return [...existing, ...appended];
}
