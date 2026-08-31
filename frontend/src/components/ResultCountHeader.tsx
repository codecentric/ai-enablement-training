export interface ResultCountHeaderProps {
  loadedCount: number;
  activeFilterCount: number;
  isEndOfResults: boolean;
}

/**
 * States how many results are loaded so far and how many filters are
 * active. Never states a total — `ListingPage` carries no count
 * (contracts/ui.md, Screen: Search results / Loaded).
 */
export function ResultCountHeader({ loadedCount, activeFilterCount, isEndOfResults }: ResultCountHeaderProps) {
  const resultsText =
    loadedCount === 0
      ? 'No results loaded'
      : `${loadedCount} result${loadedCount === 1 ? '' : 's'} loaded${isEndOfResults ? ' (all of them)' : ' so far'}`;
  const filtersText = activeFilterCount === 0 ? 'No filters active' : `${activeFilterCount} filter${activeFilterCount === 1 ? '' : 's'} active`;

  return (
    <div className="result-count-header" role="status" aria-live="polite">
      <span>{resultsText}</span>
      <span aria-hidden="true"> · </span>
      <span>{filtersText}</span>
    </div>
  );
}
