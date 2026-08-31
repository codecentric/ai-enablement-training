import { useEffect, useRef, useState } from 'react';
import { buildSavedSearchSummary } from '../lib/savedSearchSummary';
import { relativeAge } from '../lib/relativeTime';
import type { SavedSearch } from '../lib/types';

export interface SavedSearchRowProps {
  search: SavedSearch;
  categoryBreadcrumb?: string;
  /**
   * Data is stale/offline, but per contracts/ui.md ("Offline and
   * staleness") the controls stay enabled regardless — only the mutation
   * itself is refused, with an explanation, when actually attempted.
   */
  isOffline?: boolean;
  onRename: (id: string, label: string) => Promise<boolean>;
  onDelete: (id: string) => Promise<boolean>;
  onToggleNotify: (id: string, next: boolean) => Promise<boolean>;
}

const OFFLINE_EXPLANATION = 'Connect to make changes. Nothing was saved.';

/**
 * One row: label, human-readable summary, notify toggle, last match
 * (contracts/ui.md, `SavedSearchRow`). Rename rejects empty. Delete is
 * confirmed first. The notify toggle applies optimistically and reverts on
 * failure, stating the failure rather than silently keeping the user's
 * choice.
 */
export function SavedSearchRow({
  search,
  categoryBreadcrumb,
  isOffline,
  onRename,
  onDelete,
  onToggleNotify,
}: SavedSearchRowProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [labelDraft, setLabelDraft] = useState(search.label);
  const [renameError, setRenameError] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [notifyOptimistic, setNotifyOptimistic] = useState(search.notify);
  const [notifyError, setNotifyError] = useState<string | null>(null);
  const notifyMutationInFlight = useRef(false);

  // Resync the optimistic notify value when the underlying search changes
  // (e.g. a fresh fetch reconciles it), but never while a toggle the user
  // just made is still in flight — that would clobber the optimistic value
  // the row is showing before the request has even settled.
  useEffect(() => {
    if (notifyMutationInFlight.current) return;
    setNotifyOptimistic(search.notify);
  }, [search.id, search.notify]);

  const summary = buildSavedSearchSummary(search, categoryBreadcrumb);
  const lastMatchText = search.lastMatchedAt ? `Last match ${relativeAge(search.lastMatchedAt)}` : 'No matches yet';

  async function submitRename() {
    const trimmed = labelDraft.trim();
    if (trimmed === '') {
      setRenameError('Label cannot be empty.');
      return;
    }
    setRenameError(null);
    const ok = await onRename(search.id, trimmed);
    if (ok) {
      setIsEditing(false);
    } else {
      setRenameError(isOffline ? OFFLINE_EXPLANATION : 'Could not rename — try again.');
    }
  }

  async function handleToggleNotify() {
    const next = !notifyOptimistic;
    setNotifyOptimistic(next);
    setNotifyError(null);
    notifyMutationInFlight.current = true;
    const ok = await onToggleNotify(search.id, next);
    notifyMutationInFlight.current = false;
    if (!ok) {
      setNotifyOptimistic(!next); // revert to the previous state
      setNotifyError(isOffline ? OFFLINE_EXPLANATION : 'Could not update notifications — try again.');
    }
  }

  async function confirmDelete() {
    setDeleteError(null);
    const ok = await onDelete(search.id);
    if (!ok) {
      setConfirmingDelete(false);
      setDeleteError(isOffline ? OFFLINE_EXPLANATION : 'Could not delete — try again.');
    }
    // On success the parent removes this row; nothing more to do here.
  }

  return (
    <li className="saved-search-row">
      {isEditing ? (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            void submitRename();
          }}
        >
          <label htmlFor={`rename-${search.id}`}>Rename saved search</label>
          <input
            id={`rename-${search.id}`}
            type="text"
            value={labelDraft}
            onChange={(e) => setLabelDraft(e.target.value)}
            aria-invalid={renameError ? true : undefined}
          />
          <button type="submit">Save</button>
          <button
            type="button"
            onClick={() => {
              setIsEditing(false);
              setLabelDraft(search.label);
              setRenameError(null);
            }}
          >
            Cancel
          </button>
          {renameError && <p role="alert">{renameError}</p>}
        </form>
      ) : (
        <div className="saved-search-row__label">
          <span className="saved-search-row__label-text">{search.label}</span>
          <button type="button" onClick={() => setIsEditing(true)}>
            Rename
          </button>
        </div>
      )}

      <p className="saved-search-row__summary">{summary}</p>
      <p className="saved-search-row__last-match">{lastMatchText}</p>

      <div className="saved-search-row__notify">
        <button
          type="button"
          role="switch"
          aria-checked={notifyOptimistic}
          onClick={() => void handleToggleNotify()}
        >
          Notify: {notifyOptimistic ? 'On' : 'Off'}
        </button>
        {notifyError && (
          <span role="alert" className="saved-search-row__error">
            {notifyError}
          </span>
        )}
      </div>

      <div className="saved-search-row__delete">
        {confirmingDelete ? (
          <>
            <span>Delete this saved search? This cannot be undone.</span>
            <button type="button" onClick={() => void confirmDelete()}>
              Yes, delete
            </button>
            <button type="button" onClick={() => setConfirmingDelete(false)}>
              Cancel
            </button>
          </>
        ) : (
          <button
            type="button"
            onClick={() => {
              setDeleteError(null);
              setConfirmingDelete(true);
            }}
          >
            Delete
          </button>
        )}
        {deleteError && (
          <p role="alert" className="saved-search-row__error">
            {deleteError}
          </p>
        )}
      </div>
    </li>
  );
}

export function SavedSearchRowPlaceholder() {
  return (
    <li className="saved-search-row saved-search-row--placeholder" aria-hidden="true">
      <span className="saved-search-row__label-placeholder" />
      <span className="saved-search-row__summary-placeholder" />
    </li>
  );
}
