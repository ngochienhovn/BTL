package server.model;

public class User extends Entity {

    protected String fullname;
    protected String email;
    protected String passwordHash;
    protected UserRole role;

    public User(String fullname, String email, String passwordHash, UserRole role) {
        this. fullname = fullname;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public String printInfo() {
        return String.format("User :[id=%d, fullname=%s, email=%s, role=%s]", id, fullname, email, role);")
    }
}
