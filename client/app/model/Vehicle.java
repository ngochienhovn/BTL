package app.model;

public class Vehicle extends Item {
    public Vehicle(String id, String name, String description, double startingBid, String imageUrl,
            String sellerEmail) {
        super(id, "Vehicle", name, description, startingBid, imageUrl, sellerEmail);
    }
}
