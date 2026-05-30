package app.model;

public class Bidder extends User {
    public Bidder(String id, String fullName, String email, String password) {
        super(id, fullName, email, password);
    }

    @Override
    public UserRole getRole() {
        return UserRole.BIDDER;
    }
}
