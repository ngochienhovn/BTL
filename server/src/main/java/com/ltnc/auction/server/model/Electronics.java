package com.ltnc.auction.server.model;

import java.math.BigDecimal;

public class Electronics extends Item {
    public Electronics(String type, String name, String description, BigDecimal startingBid, String imageUrl, String sellerEmail) {
        super(type, name, description, startingBid, imageUrl, sellerEmail);
    }
}
