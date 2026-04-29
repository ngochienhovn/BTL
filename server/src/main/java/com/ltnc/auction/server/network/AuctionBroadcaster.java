package com.ltnc.auction.server.network;

import java.util.*;
import java.util.concurrent.*;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;

public class AuctionBroadcaster {
  private final List<ClientHandler> listeners = new CopyOnWriteArrayList<>();

  public void registerListener(ClientHandler client) {
   
  }

  public void unregisterListener(ClientHandler client) {
    
  }

  public void broadcastAuctionUpdate(Long auctionId, Auction auction) {
    
  }

  public void broadcastWalletUpdate(Long userId, Wallet wallet) {
    
  }

  public void broadcastAuctionStateChange(Long auctionId, String newState) {
    
  }

  private void sendMessageToAllListeners(ServerToClientMessage msg) {
    
  }
}
