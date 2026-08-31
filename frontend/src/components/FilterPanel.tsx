import { useEffect, useState } from 'react';
import { centsToEurosInputString, eurosStringToCents } from '../lib/money';
import { isValidPostcode, isValidPriceRange } from '../lib/filters';
import type { CategoryTreeNode } from '../lib/types';
import type { SearchFilters } from '../lib/types';

const RADIUS_STEPS = [2, 5, 10, 20, 50, 100];

export interface FilterPanelProps {
  filters: SearchFilters;
  categories: CategoryTreeNode[] | null;
  categoriesError: boolean;
  applying: boolean;
  onApply: (next: SearchFilters) => void;
}

/**
 * Filters are exactly the members of `SavedSearch.filters` (contracts/ui.md,
 * Component: FilterPanel). The panel renders from `filters`, which the
 * caller derives from the URL — the URL is the single source of truth.
 * Discrete controls (category, radius) apply on change; text controls
 * (price, postcode) commit on blur, not on every keystroke.
 */
export function FilterPanel({ filters, categories, categoriesError, applying, onApply }: FilterPanelProps) {
  const [priceMinDraft, setPriceMinDraft] = useState(
    filters.priceMinCents !== undefined ? centsToEurosInputString(filters.priceMinCents) : '',
  );
  const [priceMaxDraft, setPriceMaxDraft] = useState(
    filters.priceMaxCents !== undefined ? centsToEurosInputString(filters.priceMaxCents) : '',
  );
  const [postcodeDraft, setPostcodeDraft] = useState(filters.postcode ?? '');
  const [priceError, setPriceError] = useState<string | null>(null);
  const [postcodeError, setPostcodeError] = useState<string | null>(null);

  // Filters can also change from outside (back button, a pasted URL) — keep
  // the drafts in sync so the panel never shows a stale in-memory copy.
  useEffect(() => {
    setPriceMinDraft(filters.priceMinCents !== undefined ? centsToEurosInputString(filters.priceMinCents) : '');
    setPriceMaxDraft(filters.priceMaxCents !== undefined ? centsToEurosInputString(filters.priceMaxCents) : '');
    setPostcodeDraft(filters.postcode ?? '');
  }, [filters.priceMinCents, filters.priceMaxCents, filters.postcode]);

  function commitCategory(categoryId: string) {
    const next: SearchFilters = { ...filters };
    if (categoryId === '') delete next.categoryId;
    else next.categoryId = categoryId;
    onApply(next);
  }

  function commitPrice() {
    const minCents = priceMinDraft.trim() === '' ? undefined : eurosStringToCents(priceMinDraft);
    const maxCents = priceMaxDraft.trim() === '' ? undefined : eurosStringToCents(priceMaxDraft);

    if (priceMinDraft.trim() !== '' && minCents === null) {
      setPriceError('Enter a euro amount, e.g. 19,99.');
      return;
    }
    if (priceMaxDraft.trim() !== '' && maxCents === null) {
      setPriceError('Enter a euro amount, e.g. 19,99.');
      return;
    }
    if (!isValidPriceRange(minCents ?? undefined, maxCents ?? undefined)) {
      setPriceError('Price from must not be greater than price to.');
      return;
    }

    setPriceError(null);
    const next: SearchFilters = { ...filters };
    if (minCents === undefined || minCents === null) delete next.priceMinCents;
    else next.priceMinCents = minCents;
    if (maxCents === undefined || maxCents === null) delete next.priceMaxCents;
    else next.priceMaxCents = maxCents;
    onApply(next);
  }

  function commitPostcode() {
    const trimmed = postcodeDraft.trim();
    if (trimmed === '') {
      setPostcodeError(null);
      const next: SearchFilters = { ...filters };
      delete next.postcode;
      delete next.radiusKm; // radius requires postcode — never send it alone
      onApply(next);
      return;
    }
    if (!isValidPostcode(trimmed)) {
      setPostcodeError('Postcode must be exactly 5 digits.');
      return;
    }
    setPostcodeError(null);
    const next: SearchFilters = { ...filters, postcode: trimmed };
    onApply(next);
  }

  function commitRadius(radiusKm: string) {
    const next: SearchFilters = { ...filters };
    if (radiusKm === '') delete next.radiusKm;
    else next.radiusKm = Number(radiusKm);
    onApply(next);
  }

  const hasValidPostcode = filters.postcode !== undefined && isValidPostcode(filters.postcode);

  return (
    <fieldset className="filter-panel" disabled={applying} aria-busy={applying}>
      <legend>Filters</legend>

      <div className="filter-field">
        <label htmlFor="filter-category">Category</label>
        <select
          id="filter-category"
          value={filters.categoryId ?? ''}
          disabled={categoriesError || categories === null}
          onChange={(e) => commitCategory(e.target.value)}
        >
          <option value="">All categories</option>
          {(categories ?? []).map((top) => (
            <optgroup key={top.id} label={top.name}>
              <option value={top.id}>{top.name} (all)</option>
              {(top.children ?? []).map((leaf) => (
                <option key={leaf.id} value={leaf.id}>
                  {leaf.name}
                </option>
              ))}
            </optgroup>
          ))}
        </select>
        {categoriesError && (
          <p className="filter-field__error">Categories could not be loaded. Other filters still work.</p>
        )}
      </div>

      <div className="filter-field">
        <label htmlFor="filter-price-min">Price from (€)</label>
        <input
          id="filter-price-min"
          type="text"
          inputMode="decimal"
          value={priceMinDraft}
          onChange={(e) => setPriceMinDraft(e.target.value)}
          onBlur={commitPrice}
          aria-describedby={priceError ? 'filter-price-error' : undefined}
          aria-invalid={priceError ? true : undefined}
        />
      </div>

      <div className="filter-field">
        <label htmlFor="filter-price-max">Price to (€)</label>
        <input
          id="filter-price-max"
          type="text"
          inputMode="decimal"
          value={priceMaxDraft}
          onChange={(e) => setPriceMaxDraft(e.target.value)}
          onBlur={commitPrice}
          aria-describedby={priceError ? 'filter-price-error' : undefined}
          aria-invalid={priceError ? true : undefined}
        />
      </div>
      {priceError && (
        <p id="filter-price-error" className="filter-field__error" role="alert">
          {priceError}
        </p>
      )}

      <div className="filter-field">
        <label htmlFor="filter-postcode">Postcode</label>
        <input
          id="filter-postcode"
          type="text"
          inputMode="numeric"
          maxLength={5}
          value={postcodeDraft}
          onChange={(e) => setPostcodeDraft(e.target.value)}
          onBlur={commitPostcode}
          aria-describedby={postcodeError ? 'filter-postcode-error' : undefined}
          aria-invalid={postcodeError ? true : undefined}
        />
        {postcodeError && (
          <p id="filter-postcode-error" className="filter-field__error" role="alert">
            {postcodeError}
          </p>
        )}
      </div>

      <div className="filter-field">
        <label htmlFor="filter-radius">Radius (km)</label>
        <select
          id="filter-radius"
          value={filters.radiusKm !== undefined ? String(filters.radiusKm) : ''}
          disabled={!hasValidPostcode}
          onChange={(e) => commitRadius(e.target.value)}
        >
          <option value="">No radius</option>
          {RADIUS_STEPS.map((km) => (
            <option key={km} value={km}>
              {km} km
            </option>
          ))}
        </select>
        {!hasValidPostcode && <p className="filter-field__hint">Enter a valid postcode to filter by radius.</p>}
      </div>
    </fieldset>
  );
}
