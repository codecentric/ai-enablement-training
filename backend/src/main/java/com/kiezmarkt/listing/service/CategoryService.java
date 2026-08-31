package com.kiezmarkt.listing.service;

import com.kiezmarkt.listing.domain.Category;
import com.kiezmarkt.listing.dto.CategoryTreeNode;
import com.kiezmarkt.listing.error.CategoryNotALeafException;
import com.kiezmarkt.listing.error.CategoryNotFoundException;
import com.kiezmarkt.listing.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** The full two-level tree, per {@code GET /categories}. */
    public List<CategoryTreeNode> tree() {
        return categoryRepository.findAll().stream()
                .filter(Category::isTopLevel)
                .sorted(Comparator.comparing(Category::getId))
                .map(top -> new CategoryTreeNode(
                        top.getId(),
                        top.getLegacyId(),
                        top.getParentId(),
                        top.getName(),
                        leavesOf(top.getId())))
                .toList();
    }

    private List<Category> leavesOf(String topLevelId) {
        return categoryRepository.findAll().stream()
                .filter(c -> topLevelId.equals(c.getParentId()))
                .sorted(Comparator.comparing(Category::getId))
                .toList();
    }

    /** Leaf ids under a top-level category, for widening a search filter. */
    public List<String> leafIdsUnder(String topLevelId) {
        return leavesOf(topLevelId).stream().map(Category::getId).toList();
    }

    public boolean exists(String categoryId) {
        return categoryRepository.findById(categoryId).isPresent();
    }

    public boolean isTopLevel(String categoryId) {
        return categoryRepository.findById(categoryId).map(Category::isTopLevel).orElse(false);
    }

    /** Validates a listing's category: must exist and must be a leaf (invariant 4). */
    public void requireLeafCategory(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        if (category.isTopLevel()) {
            throw new CategoryNotALeafException(categoryId);
        }
    }

    /** Validates a filter category (search / saved search): any known category, leaf or top-level. */
    public void requireKnownCategory(String categoryId) {
        if (!exists(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }
    }
}
