export interface StalenessNoticeProps {
  /** Concrete age, e.g. "2 hours ago" — never a vague marker (contracts/ui.md). */
  ageLabel: string;
  offline?: boolean;
}

/**
 * `StalenessNotice` does not exist in the design system
 * (contracts/ui.md, "Saved searches" / "Offline and staleness"). This is an
 * invented, minimal banner: it states concretely how old the shown data is,
 * and is announced when entered (role="status" + aria-live).
 */
export function StalenessNotice({ ageLabel, offline }: StalenessNoticeProps) {
  return (
    <div className="staleness-notice" role="status" aria-live="polite">
      {offline ? 'Offline — showing data from cache. ' : ''}
      Updated {ageLabel}.
    </div>
  );
}
