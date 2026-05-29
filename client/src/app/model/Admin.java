package app.model;

public class Admin extends User {
    public Admin(String id, String fullName, String email, String password) {
        super(id, fullName, email, password);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }
}
