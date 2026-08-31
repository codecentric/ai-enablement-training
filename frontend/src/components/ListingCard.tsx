import { useState } from 'react';
import { formatPriceAnnouncement } from '../lib/priceLabel';
import { relativeAge } from '../lib/relativeTime';
import type { Listing } from '../lib/types';
import { PriceLabel } from './PriceLabel';

export interface ListingCardProps {
  listing: Listing;
}

/**
 * The unit of the result list. Shows only cover image, title, price,
 * postcode and age (contracts/ui.md, Component: ListingCard). The whole
 * card is one activation target; there are no nested controls inside it.
 */
export function ListingCard({ listing }: ListingCardProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const coverImageId = listing.imageIds[0];
  const showPlaceholder = !coverImageId || imageFailed;
  const age = listing.publishedAt ? relativeAge(listing.publishedAt) : '';
  const priceAnnouncement = formatPriceAnnouncement(listing.priceCents);

  const accessibleLabel = [listing.title, priceAnnouncement, listing.postcode, age && `published ${age}`]
    .filter(Boolean)
    .join(', ');

  return (
    <a className="listing-card" href={`/listings/${listing.id}`} aria-label={accessibleLabel}>
      <span className="listing-card__image" aria-hidden="true">
        {showPlaceholder ? (
          <span className="listing-card__image-placeholder" />
        ) : (
          // Cover image is decorative here: the accessible name is carried
          // entirely by the anchor's aria-label, so alt is empty.
          <img
            src={`/placeholder-images/${coverImageId}`}
            alt=""
            onError={() => setImageFailed(true)}
          />
        )}
      </span>
      <span className="listing-card__body">
        <span className="listing-card__title">{listing.title}</span>
        <span className="listing-card__price">
          <PriceLabel priceCents={listing.priceCents} currency={listing.currency} />
        </span>
        <span className="listing-card__meta">
          {listing.postcode}
          {age ? ` · ${age}` : ''}
        </span>
      </span>
    </a>
  );
}

export function ListingCardPlaceholder() {
  return (
    <span className="listing-card listing-card--placeholder" aria-hidden="true">
      <span className="listing-card__image">
        <span className="listing-card__image-placeholder" />
      </span>
      <span className="listing-card__body">
        <span className="listing-card__title-placeholder" />
        <span className="listing-card__title-placeholder listing-card__title-placeholder--short" />
        <span className="listing-card__price-placeholder" />
      </span>
    </span>
  );
}
