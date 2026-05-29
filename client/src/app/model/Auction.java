package app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private final String id;
    private final String itemId;
    private final String title;
    private final String description;
    private final String category;
    private final double startingBid;
    private double currentBid;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private final String image;
    private final String seller;
    private final List<Bid> bids;
    private AuctionStatus status;
    private String winnerBidder;
    private boolean extendedByAntiSniping;

    public Auction(
            String id,
            String itemId,
            String title,
            String description,
            String category,
            double startingBid,
            double currentBid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String image,
            String seller,
            List<Bid> bids) {
        this.id = id;
        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.startingBid = startingBid;
        this.currentBid = currentBid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.image = image;
        this.seller = seller;
        this.bids = new ArrayList<>(bids);
        this.status = AuctionStatus.OPEN;
    }

    public String getId() {
        return id;
    }
    
    public String getItemId() {
        return itemId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public double getStartingBid() {
        return startingBid;
    }

    public double getCurrentBid() {
        return currentBid;
    }

    public void setCurrentBid(double currentBid) {
        this.currentBid = currentBid;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getImage() {
        return image;
    }

    public String getSeller() {
        return seller;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getWinnerBidder() {
        return winnerBidder;
    }

    public void setWinnerBidder(String winnerBidder) {
        this.winnerBidder = winnerBidder;
    }

    public boolean isExtendedByAntiSniping() {
        return extendedByAntiSniping;
    }

    public void setExtendedByAntiSniping(boolean extendedByAntiSniping) {
        this.extendedByAntiSniping = extendedByAntiSniping;
    }
}
