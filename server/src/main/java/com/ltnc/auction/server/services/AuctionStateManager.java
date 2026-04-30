package com.ltnc.auction.server.services;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import com.ltnc.auction.server.dao.AuctionDAO;
import com.ltnc.auction.server.network.AuctionBroadcaster;

public class AuctionStateManager 
{
  /**
   * This class is responsible for managing the state of an auction.
   * It schedules transitions and broadcasts state changes to all clients.
   */
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
  private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
  private final AuctionDAO auctionDAO;
  private final AuctionBroadcaster broadcaster;

  /**
   * Constructor for AuctionStateManager.
   * @param auctionDAO The auction DAO.
   * @param broadcaster The auction broadcaster.
   */
  public AuctionStateManager(AuctionDAO auctionDAO, AuctionBroadcaster broadcaster) 
  {
    this.auctionDAO = auctionDAO;
    this.broadcaster = broadcaster;
  }

  /**
   * Schedule a transition from one state to another after a delay.
   * @param auctionId The ID of the auction.
   * @param fromState The current state of the auction.
   * @param toState The state to transition to.
   * @param delay The delay before the transition.
   */
  public void scheduleStateTransition(Long auctionId, String fromState, String toState, Duration delay) 
  {
    cancelScheduledTransition(auctionId);
    ScheduledFuture<?> future = scheduler.schedule(() -> {
      try {
        Auction auction = auctionDAO.findById(auctionId);

        if (auction == null) 
        {
          return;
        }

        if (fromSate != null && !fromSate.equals(auction.getStatus())) return;

        transitionAuctionState(auctionId, toState);
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        scheduledTasks.remove(auctionId);
      }
    }, delay.toMillis(), TimeUnit.MILLISECONDS);
    scheduledTasks.put(auctionId, future);
  }

  /**
   * Cancel a scheduled transition.
   * @param auctionId The ID of the auction.
   */
  public void cancelScheduledTransition(Long auctionId) 
  {
    ScheduledFuture<?> future = scheduledTasks.remove(auctionId);

    if (future != null) future.cancel(false);
  }

  /**
   * Transition the auction state immediately.
   * @param auctionId The ID of the auction.
   * @param newState The new state of the auction.
   */
  public void transitionAuctionState(Long auctionId, String newState) 
  {
    auctionDAO.updateStatus(auctionId, newState);
    broadcaster.broadcastAuctionStateChange(auctionId, newState);
  }
}
