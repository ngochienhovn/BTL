package com.ltnc.auction.server.model;

import java.math.BigDecimal;

public class ItemFactory {
    public static Item create(String type, String name, String description,
                              BigDecimal startingBid, String imageUrl, String sellerEmail) {
        return switch (type == null ? "" : type.toLowerCase()) {
            case "art" -> new Art(name, description, startingBid, imageUrl, sellerEmail);
            case "vehicle" -> new Vehicle(name, description, startingBid, imageUrl, sellerEmail);
            default -> new Electronics(type != null ? type : "Electronics", name, description, startingBid, imageUrl, sellerEmail);
        };
    }
}
