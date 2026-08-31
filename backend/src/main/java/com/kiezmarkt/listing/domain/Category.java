package com.kiezmarkt.listing.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A category node. A two-level tree: top-level nodes have {@code parentId
 * == null}; leaves have a {@code parentId} and carry listings. Every node
 * also carries {@code legacyId}, the pre-2024 numeric id.
 */
public class Category {

    private String id;
    private int legacyId;
    private String parentId;
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getLegacyId() {
        return legacyId;
    }

    public void setLegacyId(int legacyId) {
        this.legacyId = legacyId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonIgnore
    public boolean isTopLevel() {
        return parentId == null;
    }
}
