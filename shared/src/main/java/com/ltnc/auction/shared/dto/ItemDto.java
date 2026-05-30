package com.ltnc.auction.shared.dto;

public class ItemDto {
    public Long id;
    public String type; // Electronics, Art, Vehicle
    public String name;
    public String description;
    public double startingBid;
    public String imageUrl;
    public String sellerEmail;

    public ItemDto() {}
}
