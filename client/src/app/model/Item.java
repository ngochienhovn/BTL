package app.model;

public abstract class Item extends Entity {
    private final String type;
    private String name;
    private String description;
    private double startingBid;
    private String imageUrl;
    private final String sellerEmail;

    protected Item(String id, String type, String name, String description, double startingBid, String imageUrl,
            String sellerEmail) {
        super(id);
        this.type = type;
        this.name = name;
        this.description = description;
        this.startingBid = startingBid;
        this.imageUrl = imageUrl;
        this.sellerEmail = sellerEmail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingBid() {
        return startingBid;
    }

    public void setStartingBid(double startingBid) {
        this.startingBid = startingBid;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getSellerEmail() {
        return sellerEmail;
    }

    public String getType() {
        return type;
    }
}
