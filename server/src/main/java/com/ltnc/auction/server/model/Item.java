package com.ltnc.auction.server.model;

import java.math.BigDecimal;

public class Item {
    private Long id;
    private Long sellerId;
    private String sellerEmail;
    private String type;
    private String name;
    private String description;
    private BigDecimal startingBid;
    private String imageUrl;

    public Item() {
    }

    public Item(Long id, Long sellerId, String sellerEmail, String type, String name, String description,
                BigDecimal startingBid, String imageUrl) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerEmail = sellerEmail;
        this.type = type;
        this.name = name;
        this.description = description;
        this.startingBid = startingBid;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public void setSellerId(Long sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public void setSellerEmail(String sellerEmail) {
        this.sellerEmail = sellerEmail;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getStartingBid() {
        return startingBid;
    }

    public void setStartingBid(BigDecimal startingBid) {
        this.startingBid = startingBid;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
