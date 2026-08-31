// Domain types. Field names match domain.md and api.yaml exactly — not close, the same.

export type ListingStatus = 'draft' | 'published' | 'paused' | 'expired' | 'deleted';

export interface Listing {
  id: string;
  sellerId: string;
  title: string;
  description: string;
  categoryId: string;
  /**
   * Declared non-null by api.yaml, but the import feed delivers `null` for
   * listings posted without a price ("Zu verschenken"). Treat this as
   * `number | null` everywhere in the client — trusting the declared type
   * crashes on real data.
   */
  priceCents: number | null;
  currency: string;
  status: ListingStatus;
  postcode: string;
  createdAt: string;
  publishedAt: string | null;
  imageIds: string[];
}

export interface ListingPage {
  items: Listing[];
  nextCursor: string | null;
}

export interface SellerPublic {
  id: string;
  displayName: string;
  isCommercial: boolean;
}

export interface Category {
  id: string;
  legacyId: number;
  parentId: string | null;
  name: string;
}

export interface CategoryTreeNode extends Category {
  children?: Category[];
}

export interface SavedSearchFilters {
  categoryId?: string;
  priceMinCents?: number;
  priceMaxCents?: number;
  postcode?: string;
  radiusKm?: number;
}

export interface SavedSearch {
  id: string;
  sellerId: string;
  label: string;
  query: string;
  filters: SavedSearchFilters;
  notify: boolean;
  lastMatchedAt: string | null;
}

export interface Problem {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
}

/** The filters a search-results query string can carry. Exactly SavedSearch['filters'] plus the free-text query. */
export interface SearchFilters {
  q?: string;
  categoryId?: string;
  priceMinCents?: number;
  priceMaxCents?: number;
  postcode?: string;
  radiusKm?: number;
}
