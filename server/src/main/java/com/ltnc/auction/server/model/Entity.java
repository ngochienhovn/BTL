package com.ltnc.auction.server.model;

public abstract class Entity {
    protected Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public abstract String printInfo();
}
