// Thin HTTP client for the Kiezmarkt listing service. All calls are
// relative to PUBLIC_KIEZMARKT_API (contracts/api.yaml serves everything
// under a `/v1` context path).
//
// Errors are RFC 9457 problem documents. Callers branch on `problem.type`,
// never on `problem.detail`, which is free text.

import type {
  CategoryTreeNode,
  Listing,
  ListingPage,
  Problem,
  SavedSearch,
  SavedSearchFilters,
  SellerPublic,
} from './types';

export class ProblemError extends Error {
  readonly problem: Problem;

  constructor(problem: Problem) {
    super(problem.title || problem.type);
    this.name = 'ProblemError';
    this.problem = problem;
  }
}

/** Well-known problem `type` URIs this client branches on. */
export const ProblemType = {
  invalidCursor: 'https://kiezmarkt.example/problems/invalid-cursor',
  radiusWithoutPostcode: 'https://kiezmarkt.example/problems/radius-without-postcode',
  notFound: 'https://kiezmarkt.example/problems/not-found',
  listingDeleted: 'https://kiezmarkt.example/problems/listing-deleted',
  illegalStatusTransition: 'https://kiezmarkt.example/problems/illegal-status-transition',
  terminalStatus: 'https://kiezmarkt.example/problems/terminal-status',
  validationFailed: 'https://kiezmarkt.example/problems/validation-failed',
} as const;

export function getApiBase(): string {
  const fromEnv = import.meta.env.PUBLIC_KIEZMARKT_API as string | undefined;
  return fromEnv && fromEnv.length > 0 ? fromEnv : 'http://localhost:8080/v1';
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${getApiBase()}${path}`, init);
  } catch (cause) {
    // Network failure, offline, CORS, etc. — not a problem document, but the
    // caller still needs a uniform shape to branch on.
    throw new ProblemError({
      type: 'about:blank',
      title: 'Network request failed',
      status: 0,
    });
  }

  if (res.status === 204) return undefined as T;

  const contentType = res.headers.get('content-type') ?? '';

  if (!res.ok) {
    if (contentType.includes('application/problem+json')) {
      const problem = (await res.json()) as Problem;
      throw new ProblemError(problem);
    }
    throw new ProblemError({
      type: 'about:blank',
      title: res.statusText || 'Request failed',
      status: res.status,
    });
  }

  if (contentType.includes('application/json')) {
    return (await res.json()) as T;
  }
  return undefined as T;
}

export interface SearchListingsParams {
  q?: string;
  categoryId?: string;
  priceMinCents?: number;
  priceMaxCents?: number;
  postcode?: string;
  radiusKm?: number;
  cursor?: string;
  limit?: number;
}

export function searchListings(params: SearchListingsParams): Promise<ListingPage> {
  const qs = new URLSearchParams();
  if (params.q) qs.set('q', params.q);
  if (params.categoryId) qs.set('categoryId', params.categoryId);
  if (params.priceMinCents !== undefined) qs.set('priceMinCents', String(params.priceMinCents));
  if (params.priceMaxCents !== undefined) qs.set('priceMaxCents', String(params.priceMaxCents));
  if (params.postcode) qs.set('postcode', params.postcode);
  if (params.radiusKm !== undefined) qs.set('radiusKm', String(params.radiusKm));
  if (params.cursor) qs.set('cursor', params.cursor);
  qs.set('limit', String(params.limit ?? 20));
  return request<ListingPage>(`/listings?${qs.toString()}`);
}

export function getListing(id: string): Promise<Listing> {
  return request<Listing>(`/listings/${encodeURIComponent(id)}`);
}

export function listCategories(): Promise<CategoryTreeNode[]> {
  return request<CategoryTreeNode[]>('/categories');
}

export function getSeller(id: string): Promise<SellerPublic> {
  return request<SellerPublic>(`/sellers/${encodeURIComponent(id)}`);
}

export function listSavedSearches(): Promise<SavedSearch[]> {
  return request<SavedSearch[]>('/saved-searches');
}

export interface SavedSearchCreateBody {
  label: string;
  query?: string;
  filters?: SavedSearchFilters;
  notify?: boolean;
}

export function createSavedSearch(body: SavedSearchCreateBody): Promise<SavedSearch> {
  return request<SavedSearch>('/saved-searches', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export interface SavedSearchPatchBody {
  label?: string;
  query?: string;
  filters?: SavedSearchFilters;
  notify?: boolean;
}

export function updateSavedSearch(id: string, patch: SavedSearchPatchBody): Promise<SavedSearch> {
  return request<SavedSearch>(`/saved-searches/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(patch),
  });
}

export function deleteSavedSearch(id: string): Promise<void> {
  return request<void>(`/saved-searches/${encodeURIComponent(id)}`, { method: 'DELETE' });
}
