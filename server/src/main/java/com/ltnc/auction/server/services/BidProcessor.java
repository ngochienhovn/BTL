package com.ltnc.auction.server.service;

import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.dao.BidDAO;
import com.ltnc.auction.server.dao.UserDAO;
import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.server.model.AutoBidConfig;
import com.ltnc.auction.server.model.BidTransaction;
import com.ltnc.auction.server.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles complex bidding rules like Anti-Sniping and Auto-Bidding.
 * Refactored to be stateless, making it easier to test and maintain.
 */
public class BidProcessor {

    /**
     * Thuật toán Anti-Sniping: kéo dài phiên nếu bid sát giờ kết thúc.
     *
     * @return true nếu đã gia hạn endTime
     */
    public boolean maybeApplyAntiSniping(Auction auction, AuctionDAO auctionDAO) {
        if (auction.getEndTime() == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        long remainingSeconds = ChronoUnit.SECONDS.between(now, auction.getEndTime());

        if (remainingSeconds >= 0 && remainingSeconds <= 30) {
            LocalDateTime newEnd = auction.getEndTime().plusSeconds(60);
            auction.setEndTime(newEnd);
            auctionDAO.updateEndTime(auction.getId(), newEnd);
            return true;
        }
        return false;
    }

    /**
     * Thuật toán Auto-Bid Chain: xử lý chuỗi đặt giá tự động.
     */
    public void processAutoBids(Auction auction, String initialLastBidderEmail,
            ConcurrentHashMap<Long, PriorityQueue<AutoBidConfig>> autoBidConfigs,
            AuctionDAO auctionDAO, BidDAO bidDAO, WalletService walletService, UserDAO userDAO) {
        String lastBidderEmail = initialLastBidderEmail;

        while (true) {
            PriorityQueue<AutoBidConfig> pq = autoBidConfigs.get(auction.getId());
            if (pq == null || pq.isEmpty()) {
                break;
            }

            final String currentLastEmail = lastBidderEmail;
            List<AutoBidConfig> skipped = new ArrayList<>();
            AutoBidConfig best = null;

            while (!pq.isEmpty()) {
                AutoBidConfig candidate = pq.poll();
                if (best == null && !candidate.getBidderEmail().equalsIgnoreCase(currentLastEmail)) {
                    best = candidate;
                }
                skipped.add(candidate);
            }
            pq.addAll(skipped);

            if (best == null) {
                break;
            }

            BigDecimal nextBid = auction.getCurrentBid().add(best.getIncrement());
            if (nextBid.compareTo(best.getMaxBid()) > 0) {
                break;
            }

            Long prevLeaderId = auction.getHighestBidderId();
            BigDecimal prevBid = auction.getCurrentBid();
            Long botId = best.getBidderId();
            if (botId == null || botId == 0L) {
                User u = userDAO.findByEmail(best.getBidderEmail());
                botId = u != null ? u.getId() : null;
            }
            if (botId == null) {
                break;
            }

            WalletService.WalletApplyResult wr =
                    walletService.applyForBid(botId, prevLeaderId, prevBid, nextBid, auction.getId());
            if (!wr.success()) {
                break;
            }

            auction.setCurrentBid(nextBid);
            auction.setHighestBidderId(botId);
            auctionDAO.updateCurrentBid(auction.getId(), nextBid, botId);

            BidTransaction autoBid = new BidTransaction(
                    auction.getId(), botId, best.getBidderEmail(), "Auto-Bid", nextBid);
            Long autoBidId = bidDAO.insert(autoBid);
            autoBid.setId(autoBidId);
            auction.getBids().add(autoBid);

            lastBidderEmail = best.getBidderEmail();
        }
    }
}
