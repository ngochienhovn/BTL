package com.ltnc.auction.server.model;

public class Seller extends User {
    public Seller(String fullName, String email, String passwordHash) {
        super(fullName, email, passwordHash, UserRole.SELLER);
    }

    @Override
    public String printInfo() { return "Seller[" + super.printInfo() + "]"; }
}
