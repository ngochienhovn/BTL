package com.ltnc.auction.server.network;

import java.util.*;
import java.util.concurrent.*;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;

import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.server.model.Wallet;

public class AuctionBroadcaster {
  private final List<ClientHandler> listeners = new CopyOnWriteArrayList<>();

  public void registerListener(ClientHandler client) {
    // Add to listeners
  }

  public void unregisterListener(ClientHandler client) {
    // Remove from listeners
  }

  public void broadcastAuctionUpdate(Long auctionId, Auction auction) {
    // Create message, serialize, send to all listeners
  }

  public void broadcastWalletUpdate(Long userId, Wallet wallet) {
    // Create message, serialize, send to all listeners
  }

  public void broadcastAuctionStateChange(Long auctionId, String newState) {
    // Create message, serialize, send to all listeners
  }

  private void sendMessageToAllListeners(ServerToClientMessage msg) {
    // Helper: iterate listeners, call sendBroadcast on each
  }
}
