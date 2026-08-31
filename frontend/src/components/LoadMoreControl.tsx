export interface LoadMoreControlProps {
  status: 'idle' | 'loading-more' | 'end-of-results';
  onLoadMore: () => void;
}

/**
 * Web pagination is forward-only via this explicit control — no page
 * numbers (contracts/ui.md, Pagination). Always keyboard-operable.
 */
export function LoadMoreControl({ status, onLoadMore }: LoadMoreControlProps) {
  if (status === 'end-of-results') {
    return (
      <p className="load-more load-more--end" role="status">
        End of results.
      </p>
    );
  }

  return (
    <div className="load-more">
      <button type="button" onClick={onLoadMore} disabled={status === 'loading-more'}>
        {status === 'loading-more' ? 'Loading more…' : 'Load more'}
      </button>
    </div>
  );
}
