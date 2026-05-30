package com.ltnc.auction.server.model;

public class Admin extends User {
    public Admin(String fullName, String email, String passwordHash) {
        super(fullName, email, passwordHash, UserRole.ADMIN);
    }

    @Override
    public String printInfo() { return "Admin[" + super.printInfo() + "]"; }
}
