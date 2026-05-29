package app.model;

public class Seller extends User {
    public Seller(String id, String fullName, String email, String password) {
        super(id, fullName, email, password);
    }

    @Override
    public UserRole getRole() {
        return UserRole.SELLER;
    }
}
