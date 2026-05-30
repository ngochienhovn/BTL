package com.ltnc.auction.server.model;

import java.math.BigDecimal;

public class Art extends Item {
    public Art(String name, String description, BigDecimal startingBid, String imageUrl, String sellerEmail) {
        super("Art", name, description, startingBid, imageUrl, sellerEmail);
    }
}
