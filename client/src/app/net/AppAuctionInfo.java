package app.net;

import java.util.List;

public class AppAuctionInfo {
    public Long id;
    public Long itemId;
    public String title;
    public String description;
    public String category;
    public double startingBid;
    public double currentBid;
    public String startTime;
    public String endTime;
    public String status;
    public String sellerEmail;
    public String winnerEmail;
    public String imageUrl;
    public List<AppBidInfo> bids;
}
