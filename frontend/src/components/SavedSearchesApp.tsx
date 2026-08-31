import { useCallback, useEffect, useRef, useState } from 'react';
import { ProblemError, deleteSavedSearch, listCategories, listSavedSearches, updateSavedSearch } from '../lib/api';
import { buildCategoryBreadcrumb } from '../lib/savedSearchSummary';
import { preciseAge } from '../lib/relativeTime';
import type { CategoryTreeNode, SavedSearch } from '../lib/types';
import { growVisibleCount, initialVisibleCount } from '../lib/windowing';
import { SavedSearchRow, SavedSearchRowPlaceholder } from './SavedSearchRow';
import { StalenessNotice } from './StalenessNotice';

const CACHE_KEY = 'kiezmarkt:savedSearches:cache';

interface Cache {
  data: SavedSearch[];
  fetchedAt: number;
}

function readCache(): Cache | null {
  try {
    const raw = localStorage.getItem(CACHE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as Cache;
  } catch {
    return null;
  }
}

function writeCache(data: SavedSearch[]): void {
  try {
    localStorage.setItem(CACHE_KEY, JSON.stringify({ data, fetchedAt: Date.now() }));
  } catch {
    // Storage unavailable (private mode, quota) — caching is a convenience,
    // not a requirement, so fail silently.
  }
}

function flattenCategories(tree: CategoryTreeNode[]): { id: string; name: string; parentId: string | null }[] {
  const flat: { id: string; name: string; parentId: string | null }[] = [];
  for (const top of tree) {
    flat.push({ id: top.id, name: top.name, parentId: top.parentId });
    for (const leaf of top.children ?? []) flat.push({ id: leaf.id, name: leaf.name, parentId: leaf.parentId });
  }
  return flat;
}

type Status = 'loading' | 'loaded' | 'empty' | 'error' | 'offline-cache' | 'offline-no-cache';

export function SavedSearchesApp() {
  const [status, setStatus] = useState<Status>('loading');
  const [searches, setSearches] = useState<SavedSearch[] | null>(null);
  const [cacheFetchedAt, setCacheFetchedAt] = useState<number | null>(null);
  const [errorTitle, setErrorTitle] = useState<string | null>(null);
  const [categories, setCategories] = useState<CategoryTreeNode[] | null>(null);
  const [isOffline, setIsOffline] = useState(() => typeof navigator !== 'undefined' && !navigator.onLine);
  // The list has no contractual upper bound (contracts/ui.md, "Screen: Saved
  // searches"), so it is windowed rather than rendered in one pass: only
  // `visibleCount` rows are mounted, and more is revealed a step at a time.
  const [visibleCount, setVisibleCount] = useState(() => initialVisibleCount(0));
  const sentinelRef = useRef<HTMLLIElement | null>(null);

  const flatCategories = categories ? flattenCategories(categories) : [];

  const load = useCallback(() => {
    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      const cache = readCache();
      if (cache) {
        setSearches(cache.data);
        setCacheFetchedAt(cache.fetchedAt);
        setStatus('offline-cache');
        setVisibleCount(initialVisibleCount(cache.data.length));
      } else {
        setStatus('offline-no-cache');
      }
      return;
    }

    setStatus('loading');
    listSavedSearches()
      .then((data) => {
        writeCache(data);
        setSearches(data);
        setStatus(data.length === 0 ? 'empty' : 'loaded');
        setVisibleCount(initialVisibleCount(data.length));
      })
      .catch((err) => {
        const cache = readCache();
        if (cache) {
          setSearches(cache.data);
          setCacheFetchedAt(cache.fetchedAt);
          setVisibleCount(initialVisibleCount(cache.data.length));
        }
        setErrorTitle(err instanceof ProblemError ? err.problem.title : 'Something went wrong');
        setStatus('error');
      });
  }, []);

  useEffect(() => {
    load();
    listCategories()
      .then(setCategories)
      .catch(() => setCategories(null));

    function onOnline() {
      setIsOffline(false);
      load();
    }
    function onOffline() {
      setIsOffline(true);
    }
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, [load]);

  // Reveals more rows as the sentinel at the end of the rendered window
  // scrolls into view. A keyboard-operable "Show more" button sits right
  // alongside it in the markup below for users who can't or don't scroll.
  useEffect(() => {
    const total = searches?.length ?? 0;
    if (visibleCount >= total) return;
    const node = sentinelRef.current;
    if (!node || typeof IntersectionObserver === 'undefined') return;
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        setVisibleCount((current) => growVisibleCount(current, total));
      }
    });
    observer.observe(node);
    return () => observer.disconnect();
  }, [searches, visibleCount]);

  async function handleRename(id: string, label: string): Promise<boolean> {
    if (isOffline) return false;
    try {
      const updated = await updateSavedSearch(id, { label });
      setSearches((prev) => {
        const next = prev ? prev.map((s) => (s.id === id ? updated : s)) : prev;
        if (next) writeCache(next);
        return next;
      });
      return true;
    } catch {
      return false;
    }
  }

  async function handleToggleNotify(id: string, next: boolean): Promise<boolean> {
    if (isOffline) return false;
    try {
      const updated = await updateSavedSearch(id, { notify: next });
      setSearches((prev) => {
        const nextList = prev ? prev.map((s) => (s.id === id ? updated : s)) : prev;
        if (nextList) writeCache(nextList);
        return nextList;
      });
      return true;
    } catch {
      return false;
    }
  }

  async function handleDelete(id: string): Promise<boolean> {
    if (isOffline) return false;
    try {
      await deleteSavedSearch(id);
      setSearches((prev) => {
        const nextList = prev ? prev.filter((s) => s.id !== id) : prev;
        if (nextList) writeCache(nextList);
        return nextList;
      });
      return true;
    } catch {
      return false;
    }
  }

  if (status === 'loading') {
    return (
      <ul className="saved-searches-list" aria-busy="true">
        {Array.from({ length: 4 }).map((_, i) => (
          <SavedSearchRowPlaceholder key={i} />
        ))}
      </ul>
    );
  }

  if (status === 'offline-no-cache') {
    return (
      <div className="saved-searches__offline" role="status" aria-live="polite">
        <p>You&apos;re offline and no saved searches are cached yet. Connect to load them.</p>
      </div>
    );
  }

  if (status === 'empty') {
    return (
      <div className="saved-searches__empty">
        <p>You have no saved searches yet. Save a search from Search results to get notified about new matches.</p>
        <a href="/">Go to Search results</a>
      </div>
    );
  }

  const rows = searches ?? [];
  const visibleRows = rows.slice(0, Math.min(visibleCount, rows.length));
  const hasMore = visibleRows.length < rows.length;

  return (
    <div className="saved-searches">
      {status === 'error' && (
        <div className="saved-searches__error" role="alert">
          <p>Something went wrong: {errorTitle}.</p>
          <button type="button" onClick={load}>
            Retry
          </button>
        </div>
      )}

      {(status === 'offline-cache' || (status === 'error' && rows.length > 0)) && cacheFetchedAt !== null && (
        <StalenessNotice ageLabel={preciseAge(cacheFetchedAt)} offline={status === 'offline-cache'} />
      )}

      <ul className="saved-searches-list">
        {visibleRows.map((search) => (
          <SavedSearchRow
            key={search.id}
            search={search}
            categoryBreadcrumb={buildCategoryBreadcrumb(search.filters.categoryId, flatCategories)}
            isOffline={isOffline || status === 'offline-cache'}
            onRename={handleRename}
            onDelete={handleDelete}
            onToggleNotify={handleToggleNotify}
          />
        ))}
        {hasMore && (
          <li className="saved-searches-list__more" ref={sentinelRef}>
            <button
              type="button"
              onClick={() => setVisibleCount((current) => growVisibleCount(current, rows.length))}
            >
              Show more saved searches
            </button>
          </li>
        )}
      </ul>
    </div>
  );
}
