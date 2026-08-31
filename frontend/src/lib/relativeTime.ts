// Coarse, relative age formatting: "today", "yesterday", "3 days ago",
// "2 weeks ago". Never a raw timestamp (contracts/ui.md, ListingCard/Age).

// UTC calendar days, since every timestamp in the domain is UTC
// (domain.md: `createdAt`, `publishedAt` are UTC). Using UTC boundaries
// keeps the bucketing deterministic regardless of the viewer's local
// timezone, at the cost of "today" occasionally not matching the viewer's
// own midnight — an acceptable trade-off for a coarse, relative label.
function startOfUtcDay(date: Date): number {
  return Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
}

export function relativeAge(iso: string, now: Date = new Date()): string {
  const then = new Date(iso);
  const diffDays = Math.round((startOfUtcDay(now) - startOfUtcDay(then)) / 86_400_000);

  if (diffDays <= 0) return 'today';
  if (diffDays === 1) return 'yesterday';
  if (diffDays < 7) return `${diffDays} days ago`;

  const diffWeeks = Math.floor(diffDays / 7);
  if (diffWeeks < 5) return diffWeeks === 1 ? '1 week ago' : `${diffWeeks} weeks ago`;

  const diffMonths = Math.floor(diffDays / 30);
  if (diffMonths < 12) return diffMonths <= 1 ? '1 month ago' : `${diffMonths} months ago`;

  const diffYears = Math.floor(diffDays / 365);
  return diffYears <= 1 ? '1 year ago' : `${diffYears} years ago`;
}

/**
 * A finer-grained, concrete age for staleness banners, e.g. "2 hours ago",
 * "5 minutes ago" — the cache age must be stated concretely, not as a vague
 * marker (contracts/ui.md, "What must be communicated").
 */
export function preciseAge(timestampMs: number, now: Date = new Date()): string {
  const diffMs = Math.max(0, now.getTime() - timestampMs);
  const diffMinutes = Math.floor(diffMs / 60_000);
  if (diffMinutes < 1) return 'just now';
  if (diffMinutes < 60) return `${diffMinutes} minute${diffMinutes === 1 ? '' : 's'} ago`;
  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours < 24) return `${diffHours} hour${diffHours === 1 ? '' : 's'} ago`;
  const diffDays = Math.floor(diffHours / 24);
  return `${diffDays} day${diffDays === 1 ? '' : 's'} ago`;
}
