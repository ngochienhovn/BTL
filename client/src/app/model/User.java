package app.model;

public abstract class User extends Entity {
    private final String fullName;
    private final String email;
    private String password;

    protected User(String id, String fullName, String email, String password) {
        super(id);
        this.fullName = fullName;
        this.email = email;
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public abstract UserRole getRole();
}
