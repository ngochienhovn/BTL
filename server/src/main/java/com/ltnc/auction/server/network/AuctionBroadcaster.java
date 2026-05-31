package com.ltnc.auction.server.network;

import com.ltnc.auction.server.model.Auction;
import com.ltnc.auction.server.model.Wallet;
import com.ltnc.auction.shared.protocol.ServerToClientMessage;
import com.ltnc.auction.shared.protocol.MessageType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionBroadcaster 
{
  private final List<ClientHandler> listeners = new CopyOnWriteArrayList<>();

  public void registerListener(ClientHandler client) 
  {
    if (client == null) return;

    if (!listeners.contains(client)) listeners.add(client);
  }

  public void unregisterListener(ClientHandler client) 
  {
    if (client == null) return;

    listeners.remove(client);
  }

  public void broadcastAuctionUpdate(Long auctionId, Auction updateAuction) 
  {
    if (updateAuction == null) return;

    ServerToClientMessage msg = new ServerToClientMessage();
    msg.type = MessageType.AUCTION_UPDATE_RESULT;
    msg.auctionId = auctionId;
    msg.currentBid = updateAuction.getCurrentBid() == null ? 0.0 : updateAuction.getCurrentBid().doubleValue();
    msg.highestBidderId = updateAuction.getHighestBidderId();
    msg.auctionStatus = updateAuction.getStatus();
    msg.serverCurrentTimeMs = System.currentTimeMillis();

    sendToAll(msg);
  }

  public void broadcastWalletUpdate(Long userId, Wallet wallet) 
  {
    if (wallet == null) return;

    ServerToClientMessage msg = new ServerToClientMessage();
    msg.type = MessageType.WALLET_UPDATE_RESULT;
    msg.userId = userId;
    msg.balance = wallet.getBalance() == null ? 0.0 : wallet.getBalance().doubleValue();
    msg.reserved = wallet.getReserved() == null ? 0.0 : wallet.getReserved().doubleValue();
    msg.available = wallet.getAvailable() == null ? 0.0 : wallet.getAvailable().doubleValue();
    msg.serverCurrentTimeMs = System.currentTimeMillis();

    for (ClientHandler client : listeners) 
    {
      try 
      {
        if (userId.equals(client.getAuthenticatedUserId()))
        {
          client.sendBroadcast(msg);
        } 
      } catch (Exception e) 
      {
        unregisterListener(client);
      }
    }
  }

  public void broadcastAuctionStateChange(Long auctionId, String newState) 
  {
    if (auctionId == null || newState == null) return;
    
    ServerToClientMessage msg = new ServerToClientMessage();
    msg.type = MessageType.AUCTION_STATE_CHANGE_RESULT;
    msg.auctionId = auctionId;
    msg.auctionStatus = newState;
    msg.serverCurrentTimeMs = System.currentTimeMillis();

    sendToAll(msg);
  }

  private void sendToAll(ServerToClientMessage msg) 
  {
    for (ClientHandler client : listeners) 
    {
      try 
      {
        client.sendBroadcast(msg);
      } catch (Exception e) 
      {
        unregisterListener(client);
      }
    }
  }
}
