package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class CategoryNotALeafException extends ProblemException {
    public CategoryNotALeafException(String categoryId) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, ProblemTypes.CATEGORY_NOT_A_LEAF, "Category is not a leaf",
                categoryId + " has children and cannot carry listings directly.");
    }
}
