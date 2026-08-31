package com.kiezmarkt.listing.dto;

/**
 * Legal target statuses for {@code POST /listings/{id}/status}. {@code draft}
 * is deliberately excluded: nothing transitions back into {@code draft}, so
 * sending it is an invalid enum value (400) rather than reaching the
 * transition-table check.
 */
public enum StatusChangeTarget {
    published,
    paused,
    expired,
    deleted
}
