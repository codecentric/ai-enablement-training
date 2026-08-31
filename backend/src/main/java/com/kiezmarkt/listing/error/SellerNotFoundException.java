package com.kiezmarkt.listing.error;

import org.springframework.http.HttpStatus;

public class SellerNotFoundException extends ProblemException {
    public SellerNotFoundException(String sellerId) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, ProblemTypes.SELLER_NOT_FOUND, "Seller not found",
                "No seller with id " + sellerId + ".");
    }
}
