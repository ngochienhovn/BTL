package com.ltnc.auction.server.service;

import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.server.model.BidTransaction;
import com.ltnc.auction.server.model.User;
import com.ltnc.auction.server.model.Wallet;
import com.ltnc.auction.shared.dto.AuctionDto;
import com.ltnc.auction.shared.dto.BidDto;
import com.ltnc.auction.shared.dto.UserDto;
import com.ltnc.auction.shared.dto.WalletDto;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Helper class to convert between Domain Entities and Data Transfer Objects (DTOs).
 * Reduces boilerplate and keeps Services focused on business logic.
 */
public class AuctionMapper {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static AuctionDto toDto(Auction auction) {
        if (auction == null) return null;
        AuctionDto dto = new AuctionDto();
        dto.id = auction.getId();
        dto.itemId = auction.getItemId();
        dto.title = auction.getTitle();
        dto.description = auction.getDescription();
        dto.category = auction.getCategory();
        dto.startingBid = auction.getStartingBid() != null ? auction.getStartingBid().doubleValue() : 0;
        dto.currentBid = auction.getCurrentBid() != null ? auction.getCurrentBid().doubleValue() : 0;
        dto.startTime = auction.getStartTime() != null ? auction.getStartTime().format(ISO_FORMATTER) : null;
        dto.endTime = auction.getEndTime() != null ? auction.getEndTime().format(ISO_FORMATTER) : null;
        dto.status = auction.getStatus();
        dto.sellerEmail = auction.getSellerEmail();
        dto.winnerEmail = auction.getWinnerEmail();
        dto.imageUrl = auction.getImageUrl();
        
        if (auction.getBids() != null) {
            dto.bids = auction.getBids().stream().map(AuctionMapper::toBidDto).collect(Collectors.toList());
        } else {
            dto.bids = new ArrayList<>();
        }
        return dto;
    }

    public static BidDto toBidDto(BidTransaction bid) {
        if (bid == null) return null;
        BidDto dto = new BidDto();
        dto.id = bid.getId();
        dto.bidderName = bid.getBidderName();
        dto.bidderEmail = bid.getBidderEmail();
        dto.amount = bid.getAmount() != null ? bid.getAmount().doubleValue() : 0;
        dto.timestamp = bid.getCreatedAt() != null ? bid.getCreatedAt().format(ISO_FORMATTER) : null;
        return dto;
    }

    public static UserDto toUserDto(User user) {
        if (user == null) return null;
        return new UserDto(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name());
    }

    public static WalletDto toWalletDto(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        WalletDto dto = new WalletDto();
        dto.userId = wallet.getUserId();
        dto.balance = wallet.getBalance() != null ? wallet.getBalance().doubleValue() : 0;
        dto.reserved = wallet.getReserved() != null ? wallet.getReserved().doubleValue() : 0;
        dto.available = wallet.getAvailable() != null ? wallet.getAvailable().doubleValue() : 0;
        return dto;
    }
}
