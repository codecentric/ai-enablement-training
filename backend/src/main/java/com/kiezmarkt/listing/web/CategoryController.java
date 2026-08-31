package com.kiezmarkt.listing.web;

import com.kiezmarkt.listing.dto.CategoryTreeNode;
import com.kiezmarkt.listing.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryTreeNode> list() {
        return categoryService.tree();
    }
}
