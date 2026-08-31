package com.kiezmarkt.listing.service;

import com.kiezmarkt.listing.domain.Seller;
import com.kiezmarkt.listing.dto.SellerPublic;
import com.kiezmarkt.listing.error.NotFoundException;
import com.kiezmarkt.listing.error.SellerNotFoundException;
import com.kiezmarkt.listing.repository.SellerRepository;
import org.springframework.stereotype.Service;

@Service
public class SellerService {

    private final SellerRepository sellerRepository;

    public SellerService(SellerRepository sellerRepository) {
        this.sellerRepository = sellerRepository;
    }

    /** {@code GET /sellers/{id}}: the public projection only, never {@code email}. */
    public SellerPublic getPublic(String id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No seller with id " + id + "."));
        return new SellerPublic(seller.getId(), seller.getDisplayName(), seller.isCommercial());
    }

    public void requireExists(String sellerId) {
        if (!sellerRepository.existsById(sellerId)) {
            throw new SellerNotFoundException(sellerId);
        }
    }
}
