package com.kiezmarkt.listing.dto;

import com.kiezmarkt.listing.domain.Category;

import java.util.List;

/**
 * A top-level category with its leaves, as served by {@code GET /categories}.
 * The tree is exactly two levels deep, so {@code children} entries never
 * carry {@code children} of their own — they are plain {@link Category}
 * instances.
 */
public class CategoryTreeNode {

    private final String id;
    private final int legacyId;
    private final String parentId;
    private final String name;
    private final List<Category> children;

    public CategoryTreeNode(String id, int legacyId, String parentId, String name, List<Category> children) {
        this.id = id;
        this.legacyId = legacyId;
        this.parentId = parentId;
        this.name = name;
        this.children = children;
    }

    public String getId() {
        return id;
    }

    public int getLegacyId() {
        return legacyId;
    }

    public String getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
    }

    public List<Category> getChildren() {
        return children;
    }
}
