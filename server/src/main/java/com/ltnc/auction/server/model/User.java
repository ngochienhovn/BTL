package com.ltnc.auction.server.model;

<<<<<<< HEAD
public abstract class User extends Entity {
    protected String fullName;
    protected String email;
    protected String passwordHash;
    protected UserRole role;

    public User(String fullName, String email, String passwordHash, UserRole role) {
=======
public class User {
    private Long id;
    private String fullName;
    private String email;
    private String passwordHash;
    private UserRole role;

    public User() {
    }

    public User(Long id, String fullName, String email, String passwordHash, UserRole role) {
        this.id = id;
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

<<<<<<< HEAD
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
=======
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
>>>>>>> a4a9980ce3593461ea601ec5d280f231fec24645
    }
}
