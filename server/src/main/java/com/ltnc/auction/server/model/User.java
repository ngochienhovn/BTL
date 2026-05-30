package com.ltnc.auction.server.model;

public abstract class User extends Entity {
    protected String fullName;
    protected String email;
    protected String passwordHash;
    protected UserRole role;

    public User(String fullName, String email, String passwordHash, UserRole role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    @Override
    public String printInfo() {
        return String.format("User[id=%d, name=%s, email=%s, role=%s]", id, fullName, email, role);
    }
}
