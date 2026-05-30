package com.ltnc.auction.server.model;

import java.math.BigDecimal;

public class Vehicle extends Item {
    public Vehicle(String name, String description, BigDecimal startingBid, String imageUrl, String sellerEmail) {
        super("Vehicle", name, description, startingBid, imageUrl, sellerEmail);
    }
}
