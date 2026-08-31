// The saved-searches list has no upper bound and no pagination (api.yaml
// returns it whole; contracts/ui.md, "Screen: Saved searches"). Rendering it
// in one pass has no contract backing, so it is windowed: an initial slice
// is shown and more is revealed a step at a time, either by scrolling a
// sentinel into view or via a keyboard-operable "Show more" control.

export const INITIAL_WINDOW_SIZE = 6;
export const WINDOW_STEP = 6;

/** How many rows should be visible initially, never more than `total`. */
export function initialVisibleCount(total: number, initial: number = INITIAL_WINDOW_SIZE): number {
  return Math.min(initial, total);
}

/** Grows the visible count by `step`, capped at `total`. */
export function growVisibleCount(current: number, total: number, step: number = WINDOW_STEP): number {
  return Math.min(current + step, total);
}
