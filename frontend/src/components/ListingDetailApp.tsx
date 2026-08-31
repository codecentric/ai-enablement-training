import { useEffect, useState } from 'react';
import { ProblemError, ProblemType, getListing, listCategories } from '../lib/api';
import { buildCategoryBreadcrumb } from '../lib/savedSearchSummary';
import { relativeAge } from '../lib/relativeTime';
import type { CategoryTreeNode, Listing, Problem } from '../lib/types';
import { PriceLabel } from './PriceLabel';

export interface ListingDetailAppProps {
  listingId: string;
}

const DESCRIPTION_COLLAPSE_THRESHOLD = 400;

function flattenCategories(tree: CategoryTreeNode[]): { id: string; name: string; parentId: string | null }[] {
  const flat: { id: string; name: string; parentId: string | null }[] = [];
  for (const top of tree) {
    flat.push({ id: top.id, name: top.name, parentId: top.parentId });
    for (const leaf of top.children ?? []) flat.push({ id: leaf.id, name: leaf.name, parentId: leaf.parentId });
  }
  return flat;
}

type Status = 'loading' | 'loaded' | 'not-found' | 'gone' | 'error';

export function ListingDetailApp({ listingId }: ListingDetailAppProps) {
  const [status, setStatus] = useState<Status>('loading');
  const [listing, setListing] = useState<Listing | null>(null);
  const [problem, setProblem] = useState<Problem | null>(null);
  const [categories, setCategories] = useState<CategoryTreeNode[] | null>(null);
  const [failedImageIds, setFailedImageIds] = useState<Set<string>>(new Set());
  const [galleryIndex, setGalleryIndex] = useState(0);
  const [descriptionExpanded, setDescriptionExpanded] = useState(false);

  function load() {
    setStatus('loading');
    setProblem(null);
    getListing(listingId)
      .then((l) => {
        setListing(l);
        setStatus('loaded');
      })
      .catch((err) => {
        if (err instanceof ProblemError && err.problem.type === ProblemType.notFound) {
          setStatus('not-found');
        } else if (err instanceof ProblemError && err.problem.type === ProblemType.listingDeleted) {
          setStatus('gone');
        } else {
          setProblem(err instanceof ProblemError ? err.problem : { type: 'about:blank', title: 'Unknown error', status: 0 });
          setStatus('error');
        }
      });
  }

  useEffect(() => {
    load();
    listCategories()
      .then(setCategories)
      .catch(() => setCategories(null));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [listingId]);

  if (status === 'loading') {
    return (
      <div className="listing-detail listing-detail--loading" aria-busy="true">
        <div className="listing-detail__gallery-skeleton" />
        <p>Loading…</p>
      </div>
    );
  }

  if (status === 'not-found') {
    return (
      <div className="listing-detail__not-found">
        <p>This listing is not available.</p>
        <a href="/">Back to search</a>
      </div>
    );
  }

  if (status === 'gone') {
    return (
      <div className="listing-detail__gone">
        <p>This listing was deleted and is no longer available.</p>
        <a href="/">Back to search</a>
      </div>
    );
  }

  if (status === 'error') {
    return (
      <div className="listing-detail__error" role="alert">
        <p>Something went wrong: {problem?.title}.</p>
        <button type="button" onClick={load}>
          Retry
        </button>
      </div>
    );
  }

  if (!listing) return null;

  const flatCategories = categories ? flattenCategories(categories) : [];
  const breadcrumb = buildCategoryBreadcrumb(listing.categoryId, flatCategories);
  const visibleImageIds = listing.imageIds.filter((id) => !failedImageIds.has(id));
  const currentImageId = visibleImageIds[galleryIndex] ?? visibleImageIds[0];
  const description = listing.description ?? '';
  const isLongDescription = description.length > DESCRIPTION_COLLAPSE_THRESHOLD;
  const shownDescription =
    isLongDescription && !descriptionExpanded ? `${description.slice(0, DESCRIPTION_COLLAPSE_THRESHOLD)}…` : description;

  return (
    <article className="listing-detail">
      {visibleImageIds.length > 0 && (
        <div className="listing-detail__gallery">
          <img
            src={`/placeholder-images/${currentImageId}`}
            alt=""
            onError={() => {
              setFailedImageIds((prev) => new Set(prev).add(currentImageId));
              setGalleryIndex(0);
            }}
          />
          {visibleImageIds.length > 1 && (
            <div className="listing-detail__gallery-nav" role="group" aria-label="Image gallery">
              <button
                type="button"
                onClick={() => setGalleryIndex((i) => (i - 1 + visibleImageIds.length) % visibleImageIds.length)}
              >
                Previous
              </button>
              <span aria-live="polite">
                Image {galleryIndex + 1} of {visibleImageIds.length}
              </span>
              <button type="button" onClick={() => setGalleryIndex((i) => (i + 1) % visibleImageIds.length)}>
                Next
              </button>
            </div>
          )}
        </div>
      )}

      <h1>{listing.title}</h1>
      <p className="listing-detail__price">
        <PriceLabel priceCents={listing.priceCents} currency={listing.currency} />
      </p>

      {breadcrumb && (
        <nav aria-label="Category">
          <span>{breadcrumb}</span>
        </nav>
      )}

      <p className="listing-detail__postcode">{listing.postcode}</p>

      {listing.publishedAt && <p className="listing-detail__published">Published {relativeAge(listing.publishedAt)}</p>}

      <div className="listing-detail__description">
        <p>{shownDescription}</p>
        {isLongDescription && (
          <button type="button" onClick={() => setDescriptionExpanded((v) => !v)}>
            {descriptionExpanded ? 'Show less' : 'Show more'}
          </button>
        )}
      </div>
    </article>
  );
}
