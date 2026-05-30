package com.ltnc.auction.server.model;

public class Bidder extends User {
    public Bidder(String fullName, String email, String passwordHash) {
        super(fullName, email, passwordHash, UserRole.BIDDER);
    }

    @Override
    public String printInfo() { return "Bidder[" + super.printInfo() + "]"; }
}
