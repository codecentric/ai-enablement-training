import { useCallback, useEffect, useRef, useState } from 'react';
import { ProblemError, ProblemType, listCategories, searchListings } from '../lib/api';
import { describeActiveFilters, filtersToSearchParams, countActiveFilters, parseFiltersFromSearchParams } from '../lib/filters';
import { appendPageDeduped } from '../lib/pagination';
import { buildCategoryBreadcrumb } from '../lib/savedSearchSummary';
import type { CategoryTreeNode, Listing, Problem, SearchFilters } from '../lib/types';
import { FilterPanel } from './FilterPanel';
import { ListingCard, ListingCardPlaceholder } from './ListingCard';
import { LoadMoreControl } from './LoadMoreControl';
import { ResultCountHeader } from './ResultCountHeader';

const PAGE_SIZE = 20;

function flattenCategories(tree: CategoryTreeNode[]): { id: string; name: string; parentId: string | null }[] {
  const flat: { id: string; name: string; parentId: string | null }[] = [];
  for (const top of tree) {
    flat.push({ id: top.id, name: top.name, parentId: top.parentId });
    for (const leaf of top.children ?? []) {
      flat.push({ id: leaf.id, name: leaf.name, parentId: leaf.parentId });
    }
  }
  return flat;
}

function readFiltersFromLocation(): SearchFilters {
  // Runs on the server too (client:load renders this component during SSR),
  // where there is no window. Server render starts from empty filters; the
  // hydrated client re-reads the URL on mount.
  if (typeof window === 'undefined') return {};
  return parseFiltersFromSearchParams(new URLSearchParams(window.location.search));
}

export function SearchResultsApp() {
  const [filters, setFilters] = useState<SearchFilters>(() => readFiltersFromLocation());
  const [queryDraft, setQueryDraft] = useState(filters.q ?? '');

  const [categories, setCategories] = useState<CategoryTreeNode[] | null>(null);
  const [categoriesError, setCategoriesError] = useState(false);

  const [listings, setListings] = useState<Listing[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [applying, setApplying] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<Problem | null>(null);

  // Guards against a stale response from a superseded fetch landing after a
  // newer one (e.g. two filter changes in quick succession).
  const requestIdRef = useRef(0);

  const flatCategories = categories ? flattenCategories(categories) : [];
  const categoryLabel = buildCategoryBreadcrumb(filters.categoryId, flatCategories);

  const runSearch = useCallback(async (activeFilters: SearchFilters, mode: 'initial' | 'applying') => {
    const requestId = ++requestIdRef.current;
    if (mode === 'initial') setInitialLoading(true);
    else setApplying(true);
    setError(null);

    try {
      const page = await searchListings({ ...activeFilters, limit: PAGE_SIZE });
      if (requestIdRef.current !== requestId) return; // superseded
      setListings(page.items);
      setNextCursor(page.nextCursor);
    } catch (err) {
      if (requestIdRef.current !== requestId) return;
      if (err instanceof ProblemError) setError(err.problem);
      else setError({ type: 'about:blank', title: 'Unknown error', status: 0 });
    } finally {
      if (requestIdRef.current === requestId) {
        setInitialLoading(false);
        setApplying(false);
      }
    }
  }, []);

  // Initial load + categories, and re-derive on browser back/forward.
  useEffect(() => {
    listCategories()
      .then(setCategories)
      .catch(() => setCategoriesError(true));

    void runSearch(readFiltersFromLocation(), 'initial');

    function onPopState() {
      const next = readFiltersFromLocation();
      setFilters(next);
      setQueryDraft(next.q ?? '');
      void runSearch(next, 'applying');
    }
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function applyFilters(nextFilters: SearchFilters) {
    setFilters(nextFilters);
    const params = filtersToSearchParams(nextFilters);
    const q = nextFilters.q;
    if (q) params.set('q', q);
    const url = params.toString() ? `${window.location.pathname}?${params.toString()}` : window.location.pathname;
    // Applying a filter is one history entry (contracts/ui.md, URL reflection).
    window.history.pushState({}, '', url);
    void runSearch(nextFilters, 'applying');
  }

  function submitQuery() {
    const trimmed = queryDraft.trim().slice(0, 200);
    const next: SearchFilters = { ...filters };
    if (trimmed) next.q = trimmed;
    else delete next.q;
    applyFilters(next);
  }

  function clearAllFilters() {
    applyFilters(filters.q ? { q: filters.q } : {});
  }

  async function loadMore() {
    if (!nextCursor || loadingMore) return;
    setLoadingMore(true);
    try {
      const page = await searchListings({ ...filters, cursor: nextCursor, limit: PAGE_SIZE });
      setListings((prev) => appendPageDeduped(prev, page.items));
      setNextCursor(page.nextCursor);
    } catch (err) {
      if (err instanceof ProblemError && err.problem.type === ProblemType.invalidCursor) {
        // Cursor expired or malformed: discard it and restart the result
        // set from the current filter state, silently — never a generic
        // error for this case (contracts/ui.md, Pagination).
        setNextCursor(null);
        void runSearch(filters, 'applying');
      } else if (err instanceof ProblemError) {
        setError(err.problem);
      } else {
        setError({ type: 'about:blank', title: 'Unknown error', status: 0 });
      }
    } finally {
      setLoadingMore(false);
    }
  }

  function retry() {
    void runSearch(filters, listings.length > 0 ? 'applying' : 'initial');
  }

  const activeFilterCount = countActiveFilters(filters);
  const isEmpty = !initialLoading && !error && listings.length === 0;

  return (
    <div className="search-results">
      <form
        className="search-query"
        onSubmit={(e) => {
          e.preventDefault();
          submitQuery();
        }}
      >
        <label htmlFor="search-query-input">Search</label>
        <input
          id="search-query-input"
          type="text"
          maxLength={200}
          value={queryDraft}
          onChange={(e) => setQueryDraft(e.target.value)}
          onBlur={submitQuery}
        />
        <button type="submit">Search</button>
      </form>

      <ResultCountHeader
        loadedCount={listings.length}
        activeFilterCount={activeFilterCount}
        isEndOfResults={nextCursor === null && listings.length > 0}
      />

      <div className="search-results__layout">
        <FilterPanel
          filters={filters}
          categories={categories}
          categoriesError={categoriesError}
          applying={applying}
          onApply={applyFilters}
        />

        <div className="search-results__list-area">
          {initialLoading && (
            <ul className="listing-grid" aria-hidden="true">
              {Array.from({ length: PAGE_SIZE }).map((_, i) => (
                <li key={i}>
                  <ListingCardPlaceholder />
                </li>
              ))}
            </ul>
          )}

          {!initialLoading && error && (
            <div className="search-results__error" role="alert">
              <p>Something went wrong: {error.title}.</p>
              <button type="button" onClick={retry}>
                Retry
              </button>
              {listings.length > 0 && (
                <>
                  <p>Showing the results already loaded — they may be incomplete.</p>
                  <ul className={`listing-grid${applying ? ' listing-grid--dimmed' : ''}`}>
                    {listings.map((listing) => (
                      <li key={listing.id}>
                        <ListingCard listing={listing} />
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </div>
          )}

          {!initialLoading && !error && isEmpty && (
            <div className="search-results__empty">
              <p>No results for this search.</p>
              {activeFilterCount > 0 && (
                <>
                  <p>Applied filters:</p>
                  <ul>
                    {describeActiveFilters(filters, categoryLabel).map((f) => (
                      <li key={f}>{f}</li>
                    ))}
                  </ul>
                  <button type="button" onClick={clearAllFilters}>
                    Clear all filters
                  </button>
                </>
              )}
            </div>
          )}

          {!initialLoading && !error && listings.length > 0 && (
            <>
              <ul className={`listing-grid${applying ? ' listing-grid--dimmed' : ''}`}>
                {listings.map((listing) => (
                  <li key={listing.id}>
                    <ListingCard listing={listing} />
                  </li>
                ))}
              </ul>
              <LoadMoreControl
                status={loadingMore ? 'loading-more' : nextCursor === null ? 'end-of-results' : 'idle'}
                onLoadMore={() => void loadMore()}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
