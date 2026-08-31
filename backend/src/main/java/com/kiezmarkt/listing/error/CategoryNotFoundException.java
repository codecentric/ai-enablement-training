package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class CategoryNotFoundException extends ProblemException {
    public CategoryNotFoundException(String categoryId) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, ProblemTypes.CATEGORY_NOT_FOUND, "Category not found",
                "No category with id " + categoryId + ".");
    }
}
