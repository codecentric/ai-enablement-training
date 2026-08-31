package com.kiezmarkt.listing.web;

import com.kiezmarkt.listing.dto.SellerPublic;
import com.kiezmarkt.listing.service.SellerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sellers")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @GetMapping("/{id}")
    public SellerPublic get(@PathVariable String id) {
        return sellerService.getPublic(id);
    }
}
