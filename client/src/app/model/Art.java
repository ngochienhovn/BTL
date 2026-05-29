package app.model;

public class Art extends Item {
    public Art(String id, String name, String description, double startingBid, String imageUrl, String sellerEmail) {
        super(id, "Art", name, description, startingBid, imageUrl, sellerEmail);
    }
}
