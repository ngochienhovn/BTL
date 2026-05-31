# BTL - Sprint 4 (14/04 – 20/04): Observer Pattern & Realtime Broadcast + Wallet Changes

Duration: Week of 14/04 - 20/04  
Dependency: Sprint 3 completed (Bid + Wallet system working)  
Key Feature: Server-side broadcast of auction updates + wallet state changes (realtime UI without polling)

---

## Sprint 4 Overview

### What gets completed in Sprint 4

1. AuctionBroadcaster Pattern (server-side)
   - Server maintains list of connected clients
   - When bid succeeds -> broadcast auction + wallet updates to ALL clients
   - When auction state changes -> broadcast to all

2. State Machine for Auction Lifecycle (server-side)
   - OPEN -> RUNNING (auto-transition)
   - RUNNING -> FINISHED (auto-transition at endTime)
   - Broadcast state changes

3. Realtime message types (protocol)
   - AUCTION_UPDATE (server -> client push)
   - WALLET_UPDATE (server -> client push)
   - AUCTION_STATE_CHANGE (server -> client push)

4. Client-side message listener (background thread)
   - Listen for broadcasts from server
   - Fire events when broadcasts received

5. UI updates via Platform.runLater() (frontend)
   - Subscribe to broadcast events
   - Update UI safely (not blocking)
   - Countdown timer (Timeline, not Thread.sleep)

---

## Current BTL Status (Pre-Sprint 4)

```
COMPLETED ITEMS:
  - Models: Auction, BidTransaction, Wallet, WalletTransaction, AuctionState, UserRole
  - DAOs: AuctionDAO, BidDAO, WalletDAO, WalletTransactionDAO, UserDAO, ItemDAO
  - Services: AuctionService, WalletService, AuthService, ItemService
  - Protocol: MessageType, ClientToServerMessage, ServerToClientMessage
  - Network: ClientHandler (handles GET_AUCTIONS, PLACE_BID, DEPOSIT, WITHDRAW)
  - Client: Controllers (Home, AuctionDetail, Auth), Views (FXML)

MISSING (Sprint 4):
  - AuctionBroadcaster (no observer pattern)
  - AuctionStateManager (no lifecycle automation)
  - ServerMessageListener (no client-side listener)
  - Broadcast message types (AUCTION_UPDATE, WALLET_UPDATE, AUCTION_STATE_CHANGE)
  - Realtime UI updates (still using pull model)
  - Countdown timer (realtime)
```

---

## TEAM MEMBER 1: LEADER

Responsibilities:

- Lifecycle automation (state machine)
- Observer/Broadcaster pattern
- Auction service updates to integrate with broadcaster
- Design pattern & architecture

---

## Checklist: Leader (Sprint 4)

### TASK 1.1: Create AuctionStateManager.java

File: server/src/main/java/com/ltnc/auction/server/services/AuctionStateManager.java

What to create:

```java
public class AuctionStateManager {
  - private final ScheduledExecutorService scheduler
  - private final Map<Long, ScheduledFuture<?>> scheduledTasks
  - private final AuctionDAO auctionDAO
  - private final AuctionBroadcaster broadcaster

  // Methods:
  + scheduleStateTransition(Long auctionId, AuctionState fromState, AuctionState toState, Duration delay)
    // Schedule task to trigger transition in 'delay' time
    // When triggered: update DB, broadcast to all clients

  + cancelScheduledTransition(Long auctionId)
    // Cancel pending transition

  + transitionAuctionState(Long auctionId, AuctionState newState)
    // Immediate transition + broadcast
}
```

Acceptance Criteria:

- [x] Can schedule OPEN->RUNNING transition after delay
- [x] Can schedule RUNNING->FINISHED transition at specific time
- [x] Broadcast triggers when state changes
- [x] Handles cancel gracefully
- [x] No memory leaks (scheduled tasks cleaned up)

Reference: Similar to Project_OLD if exists, or use Java ScheduledExecutorService

---

### TASK 1.2: Create AuctionBroadcaster.java

File: server/src/main/java/com/ltnc/auction/server/network/AuctionBroadcaster.java

What to create:

```java
public class AuctionBroadcaster {
  - private final List<ClientHandler> listeners = new CopyOnWriteArrayList<>()
    // Thread-safe list of connected clients

  // LISTENER MANAGEMENT (called by ClientHandler on connect/disconnect):
  + registerListener(ClientHandler client)
    // Add client to broadcast list

  + unregisterListener(ClientHandler client)
    // Remove client from broadcast list

  // BROADCAST METHODS (called by services when state changes):
  + broadcastAuctionUpdate(Long auctionId, Auction updatedAuction)
    // Send AUCTION_UPDATE to all listeners
    // Include: auctionId, currentBid, highestBidderId, status

  + broadcastWalletUpdate(Long userId, Wallet wallet)
    // Send WALLET_UPDATE to user's wallet (only send to client of that user)
    // Include: userId, balance, reserved, available

  + broadcastAuctionStateChange(Long auctionId, String newState)
    // Send AUCTION_STATE_CHANGE to all listeners
    // Include: auctionId, newState (OPEN/RUNNING/FINISHED)

  - private void sendMessageToAllListeners(ServerToClientMessage msg)
    // Helper: serialize + send to all connected clients
    // Log errors if send fails (client might be disconnected)
}
```

Acceptance Criteria:

- [x] Singleton or properly managed (only 1 instance in ServerMain)
- [x] Thread-safe listener list (CopyOnWriteArrayList or synchronized)
- [x] Can add/remove listeners without crashing
- [x] Broadcast serializes to JSON correctly
- [x] Handles disconnected clients gracefully (removes from list if send fails)

---

### TASK 1.3: Create ServerToClientMessage updates

File: shared/src/main/java/com/ltnc/auction/shared/protocol/ServerToClientMessage.java

What to modify:

- Add new fields (if not already present):

```java
public class ServerToClientMessage {
  // Existing fields:
  public String messageType;
  public String result;
  //...

  // NEW FIELDS FOR BROADCAST:
  public String eventType; // e.g., AUCTION_UPDATE, WALLET_UPDATE, STATE_CHANGE

  // Auction update fields:
  public Long auctionId;
  public Double currentBid;
  public Long highestBidderId;
  public String auctionStatus; // OPEN, RUNNING, FINISHED

  // Wallet update fields:
  public Long userId;
  public Double balance;
  public Double reserved;
  public Double available;

  // Time sync fields:
  public Long serverCurrentTimeMs; // For client time sync
}
```

Acceptance Criteria:

- [x] Compiles without errors
- [x] Gson can serialize/deserialize this message
- [x] All new fields are Optional (can be null)

---

### TASK 1.4: Update MessageType.java

File: shared/src/main/java/com/ltnc/auction/shared/protocol/MessageType.java

What to modify:

```java
public enum MessageType {
    // ... existing ...

    // NEW BROADCAST TYPES (Server -> Client):
    AUCTION_UPDATE,           // Server broadcasts auction state change
    WALLET_UPDATE,            // Server broadcasts wallet change
    AUCTION_STATE_CHANGE,     // Server broadcasts OPEN/RUNNING/FINISHED
}
```

Acceptance Criteria:

- [x] Enum adds without breaking existing code
- [x] Can be imported/used in ClientHandler

---

### TASK 1.5: Update AuctionService.java

File: server/src/main/java/com/ltnc/auction/server/services/AuctionService.java

What to modify:

- Add broadcaster injection:

```java
public class AuctionService {
  - private final AuctionBroadcaster broadcaster;

  public AuctionService(AuctionDAO auctionDAO, BidDAO bidDAO,
                        WalletDAO walletDAO, WalletTransactionDAO walletTxDAO,
                        AuctionBroadcaster broadcaster) {
    // ... existing ...
    this.broadcaster = broadcaster;
  }

  // EXISTING METHOD - ADD BROADCAST AT END:
  public BidResult placeBid(Long auctionId, String bidderEmail, BigDecimal amount) {
    // ... existing logic (lock, validate, update, reserve/release, insert logs) ...

    // AT END: broadcast to all clients
    Auction updatedAuction = auctionDAO.findById(auctionId);
    broadcaster.broadcastAuctionUpdate(auctionId, updatedAuction);

    // Also broadcast wallet updates
    Wallet bidderWallet = walletDAO.findByUserId(bidderId);
    broadcaster.broadcastWalletUpdate(bidderId, bidderWallet);

    if (oldLeaderId != null && !oldLeaderId.equals(bidderId)) {
      Wallet oldLeaderWallet = walletDAO.findByUserId(oldLeaderId);
      broadcaster.broadcastWalletUpdate(oldLeaderId, oldLeaderWallet);
    }

    return new BidResult(BidResultCode.OK);
  }
}
```

Acceptance Criteria:

- [x] AuctionService compiles with broadcaster
- [x] Broadcast calls don't block bid processing (use async if needed)
- [x] No null pointer exceptions

---

### TASK 1.6: Update ServerMain.java

File: server/src/main/java/com/ltnc/auction/server/ServerMain.java

What to modify:

```java
public class ServerMain {
  public static void main(String[] args) {
    // ... existing ...

    // CREATE BROADCASTER SINGLETON:
    AuctionBroadcaster broadcaster = new AuctionBroadcaster();

    // CREATE STATE MANAGER:
    AuctionStateManager stateManager = new AuctionStateManager(
      auctionDAO, broadcaster
    );

    // PASS TO SERVICES:
    AuctionService auctionService = new AuctionService(
      auctionDAO, bidDAO, walletDAO, walletTxDAO, broadcaster
    );

    // PASS TO CLIENT HANDLER FACTORY:
    // When creating ClientHandler for each new connection:
    new ClientHandler(socket, authService, itemService, auctionService,
                      walletService, broadcaster);
  }
}
```

Acceptance Criteria:

- [x] ServerMain compiles
- [x] Broadcaster initialized once
- [x] StateManager initialized once
- [x] Passed to all necessary components

---

### TASK 1.7: Design document - Architecture (optional but recommended)

File: docs/sprint-4-observer-pattern.md

What to document:

- Flow diagram: User bids -> AuctionService -> Broadcaster -> All ClientHandlers -> Serialized JSON -> Clients
- List of broadcast message types and when triggered
- Thread-safety guarantees
- Performance considerations

Acceptance Criteria:

- [x] Document exists and explains observer pattern

---

## Leader Deliverables Summary

| File                       | Status | Details                                                  |
| -------------------------- | ------ | -------------------------------------------------------- |
| AuctionStateManager.java   | CREATE | State machine + scheduling                               |
| AuctionBroadcaster.java    | CREATE | Observer pattern, listener registration, broadcast       |
| ServerToClientMessage.java | MODIFY | Add broadcast fields                                     |
| MessageType.java           | MODIFY | Add AUCTION_UPDATE, WALLET_UPDATE, STATE_CHANGE          |
| AuctionService.java        | MODIFY | Inject broadcaster, call broadcast after bid             |
| ServerMain.java            | MODIFY | Instantiate broadcaster, stateManager, wire dependencies |
| (optional) design doc      | CREATE | Architecture docs                                        |

---

## TEAM MEMBER 2: FRONTEND

Responsibilities:

- Receive & handle broadcasts from server
- Update UI in realtime using Platform.runLater()
- Implement countdown timer
- Polish UI responsiveness

---

## Checklist: Frontend (Sprint 4)

### TASK 2.1: Create ServerMessageListener.java (network layer)

File: client/app/network/ServerMessageListener.java

What to create:

```java
public class ServerMessageListener implements Runnable {
  - private final Socket socket
  - private final Gson gson
  - private final List<ServerMessageCallback> callbacks
  - private volatile boolean running = true

  public ServerMessageListener(Socket socket) {
    this.socket = socket;
    this.gson = new Gson();
    this.callbacks = new CopyOnWriteArrayList<>();
  }

  // LISTENER REGISTRATION:
  + registerCallback(ServerMessageCallback callback)
    // Add listener for incoming messages

  + unregisterCallback(ServerMessageCallback callback)
    // Remove listener

  // BACKGROUND THREAD:
  @Override
  + void run()
    // Runs in separate thread
    // Loop: read from socket -> parse JSON -> fire callbacks
    // Handle disconnection gracefully

  + void stop()
    // Signal thread to stop
}

public interface ServerMessageCallback {
  void onMessageReceived(ServerToClientMessage message);
  void onConnectionLost();
}
```

Acceptance Criteria:

- [x] Runs in background thread (doesn't block UI)
- [x] Can parse ServerToClientMessage JSON correctly
- [x] Fires callbacks when messages arrive
- [x] Handles socket disconnection gracefully
- [x] Can be stopped cleanly

---

### TASK 2.2: Update AuctionDetailController.java

File: client/app/controller/AuctionDetailController.java

What to modify:

```java
public class AuctionDetailController implements Initializable, ServerMessageCallback {
  - private ServerMessageListener messageListener
  - private Timeline countdownTimeline
  - private LocalDateTime serverEndTime

  @FXML private Label auctionTitleLabel;
  @FXML private Label currentBidLabel;
  @FXML private Label endTimeLabel; // NEW: countdown timer
  @FXML private Label walletStatusLabel; // NEW: balance/reserved/available
  @FXML private TableView<BidTransaction> bidHistoryTable; // NEW or UPDATE
  @FXML private TextField bidAmountTextField;
  @FXML private Button placeBidButton;

  // INITIALIZATION:
  @Override
  public void initialize(URL location, ResourceBundle resources) {
    // Subscribe to server messages
    messageListener.registerCallback(this);
  }

  // RECEIVE & HANDLE BROADCASTS:
  @Override
  public void onMessageReceived(ServerToClientMessage message) {
    if ("AUCTION_UPDATE".equals(message.eventType)) {
      handleAuctionUpdate(message);
    } else if ("WALLET_UPDATE".equals(message.eventType)) {
      handleWalletUpdate(message);
    } else if ("STATE_CHANGE".equals(message.eventType)) {
      handleStateChange(message);
    }
  }

  - private void handleAuctionUpdate(ServerToClientMessage msg)
    // Update: currentBid label
    // Update: highestBidderId indicator
    // Refresh bid history table
    // Use Platform.runLater() for UI updates

  - private void handleWalletUpdate(ServerToClientMessage msg)
    // Parse: balance, reserved, available
    // Update: wallet status label
    // Use Platform.runLater() for UI updates

  - private void handleStateChange(ServerToClientMessage msg)
    // Handle OPEN/RUNNING/FINISHED state
    // Enable/disable bid button accordingly

  // COUNTDOWN TIMER (NEW):
  - private void startCountdownTimer(LocalDateTime endTime, long serverCurrentMs)
    // Create Timeline (1000ms tick)
    // Each tick: calculate remaining time, update label
    // When time <= 0: stop timer, show FINISHED
    // Use DateTimeFormatter to format time

  // CLEANUP:
  public void cleanup() {
    if (countdownTimeline != null) countdownTimeline.stop();
    messageListener.unregisterCallback(this);
  }
}
```

Acceptance Criteria:

- [x] Compiles and integrates with existing code
- [x] Receives AUCTION_UPDATE broadcasts
- [x] Updates currentBid label in realtime
- [x] Updates bidHistoryTable without reloading entire page
- [x] Countdown timer updates every second (no missed seconds)
- [x] Wallet status updates when balance/reserved changes
- [x] UI never freezes on broadcast (uses Platform.runLater)

---

### TASK 2.3: Update auction-detail.fxml

File: client/view/auction-detail.fxml

What to modify:

```xml
<!-- Update: add countdown label near end time -->
<HBox>
  <Label text="Ends in:" />
  <Label fx:id="endTimeLabel" text="LOADING..." /> <!-- NEW -->
</HBox>

<!-- Update: add wallet status panel -->
<VBox fx:id="walletStatusBox">
  <Label text="Balance: " />
  <Label fx:id="walletBalanceLabel" />
  <Label text="Reserved: " />
  <Label fx:id="walletReservedLabel" />
  <Label text="Available: " />
  <Label fx:id="walletAvailableLabel" />
</VBox>

<!-- Update: bid history table (if not already present) -->
<TableView fx:id="bidHistoryTable">
  <columns>
    <TableColumn text="Bidder" fx:id="bidderColumn" />
    <TableColumn text="Amount" fx:id="amountColumn" />
    <TableColumn text="Time" fx:id="timeColumn" />
  </columns>
</TableView>
```

Acceptance Criteria:

- [x] FXML compiles
- [x] Labels are properly bound in controller
- [x] Table displays correctly

---

## Frontend Deliverables Summary

| File                         | Status | Details                                              |
| ---------------------------- | ------ | ---------------------------------------------------- |
| ServerMessageListener.java   | CREATE | Background thread to listen for broadcasts           |
| AuctionDetailController.java | MODIFY | Subscribe to broadcasts, handle updates, start timer |
| auction-detail.fxml          | MODIFY | Add countdown label, wallet status, bid history      |

---

## TEAM MEMBER 3: NETWORK

Responsibilities:

- Update ClientHandler to register/unregister with broadcaster
- Ensure protocols are correct
- Handle client disconnection
- Manage socket reliability

---

## Checklist: Network (Sprint 4)

### TASK 3.1: Update ClientHandler.java - register with broadcaster

File: server/src/main/java/com/ltnc/auction/server/network/ClientHandler.java

What to modify:

```java
public class ClientHandler implements Runnable {
  - private final Socket socket
  - private final AuctionBroadcaster broadcaster // NEW
  - private String authenticatedUserEmail // Track who this client is

  // CONSTRUCTOR:
  public ClientHandler(
      Socket socket,
      AuthService authService,
      ItemService itemService,
      AuctionService auctionService,
      WalletService walletService,
      AuctionBroadcaster broadcaster  // NEW PARAM
  ) {
    this.socket = socket;
    // ... existing ...
    this.broadcaster = broadcaster;
  }

  @Override
  public void run() {
    try {
      // REGISTER ON CONNECT:
      broadcaster.registerListener(this);

      // ... existing message read loop ...

    } catch (IOException e) {
      // ...
    } finally {
      // UNREGISTER ON DISCONNECT:
      broadcaster.unregisterListener(this);
      try { socket.close(); } catch (IOException e) { }
    }
  }

  // NEW METHOD: Send broadcast message to this client:
  public void sendBroadcast(ServerToClientMessage message) {
    try {
      String json = gson.toJson(message);
      out.println(json);
      out.flush();
    } catch (Exception e) {
      System.err.println("Failed to send broadcast: " + e.getMessage());
    }
  }
}
```

Acceptance Criteria:

- [x] ClientHandler registers on creation
- [x] ClientHandler unregisters on disconnection
- [x] Can send broadcast messages to individual client
- [x] Handles send failures gracefully

---

### TASK 3.2: Update SocketServer.java to pass broadcaster

File: server/src/main/java/com/ltnc/auction/server/network/SocketServer.java

What to modify (if separate from ServerMain):

```java
public class SocketServer {
  - private final AuctionBroadcaster broadcaster

  public SocketServer(AuctionBroadcaster broadcaster, ...) {
    this.broadcaster = broadcaster;
  }

  // In accept loop:
  public void run() {
    try {
      ServerSocket serverSocket = new ServerSocket(PORT);
      while (true) {
        Socket clientSocket = serverSocket.accept();
        ClientHandler handler = new ClientHandler(
          clientSocket,
          authService,
          itemService,
          auctionService,
          walletService,
          broadcaster  // PASS BROADCASTER
        );
        new Thread(handler).start();
      }
    } catch (IOException e) { }
  }
}
```

Acceptance Criteria:

- [x] Broadcaster passed to all ClientHandlers
- [x] No null pointer exceptions

---

## Network Deliverables Summary

| File               | Status | Details                                                    |
| ------------------ | ------ | ---------------------------------------------------------- |
| ClientHandler.java | MODIFY | Register/unregister with broadcaster, sendBroadcast method |
| SocketServer.java  | MODIFY | Pass broadcaster to ClientHandler                          |

---

## TEAM MEMBER 4: DATABASE/QA

Responsibilities:

- Ensure broadcaster doesn't cause database contention
- Concurrency testing with multiple clients
- Performance testing
- Data consistency verification

---

## Checklist: Database/QA (Sprint 4)

### TASK 4.1: Create AuctionBroadcasterTest.java

File: server/src/test/java/com/ltnc/auction/server/network/AuctionBroadcasterTest.java

What to create:

- Test: testBroadcasterWithManyListeners() - 100 clients receive messages
- Test: testBroadcasterListenerCleanup() - register/unregister works
- Test: testBroadcasterPerformance() - broadcast < 100ms for 100 clients

Acceptance Criteria:

- [x] Broadcaster handles 100+ clients without significant slowdown
- [x] Listener registration/unregistration works correctly
- [x] Test passes

---

### TASK 4.2: Create AuctionServiceBroadcastTest.java

File: server/src/test/java/com/ltnc/auction/server/services/AuctionServiceBroadcastTest.java

What to create:

- Test: testBidBroadcastsToAllClients() - place bid -> all clients receive AUCTION_UPDATE
- Test: testBroadcastIncludesCorrectData() - ensure payload has correct currentBid, etc
- Test: testWalletBroadcastOnBid() - wallet updates broadcast correctly

Acceptance Criteria:

- [x] Bid triggers broadcast
- [x] All connected clients receive update
- [x] Broadcast payload is correct (currentBid, etc)
- [x] Test passes

---

### TASK 4.3: Create ConcurrencyLoadTest.java

File: server/src/test/java/com/ltnc/auction/server/ConcurrencyLoadTest.java

What to create:

- Test: testConcurrentBidsWithBroadcasts() - 50 threads bid, 100 clients receive
- Test: no deadlocks, no lost bids (currentBid == max), all broadcasts deliver

Acceptance Criteria:

- [x] No deadlocks
- [x] No lost bids (currentBid is max)
- [x] All clients receive updates
- [x] No corrupted data
- [x] Test completes in <15s

---

### TASK 4.4: Run regression tests

What to verify:

- [x] All existing tests pass: mvn test
- [x] No regression from broadcaster addition

Acceptance Criteria:

- [x] All existing tests pass

---

## QA Deliverables Summary

| File                             | Status | Details                             |
| -------------------------------- | ------ | ----------------------------------- |
| AuctionBroadcasterTest.java      | CREATE | Unit tests for broadcaster          |
| AuctionServiceBroadcastTest.java | CREATE | Integration tests for broadcasts    |
| ConcurrencyLoadTest.java         | CREATE | Concurrency + broadcast stress test |
| (regression tests)               | RUN    | Verify Sprint 3 still works         |

---

## SPRINT 4 ACCEPTANCE CRITERIA (Overall)

### Feature-level Acceptance Criteria

- [x] Broadcaster Pattern Working
- [x] Auction Updates Broadcast
- [x] Wallet Updates Broadcast
- [x] Countdown Timer Working
- [x] State Machine / Lifecycle
- [x] No Polling

### Quality Acceptance Criteria

- [x] Thread Safety
- [x] Performance
- [x] Error Handling
- [x] Data Integrity
- [x] No Regressions

---

## SPRINT 4 TIMELINE ESTIMATE

| Role     | Task                            | Hours       | Notes                       |
| -------- | ------------------------------- | ----------- | --------------------------- |
| Leader   | AuctionBroadcaster              | 3-4         | Maybe reference Project_OLD |
| Leader   | AuctionStateManager             | 2-3         | ScheduledExecutorService    |
| Leader   | Wire ServerMain                 | 1-2         | Dependencies                |
| Frontend | ServerMessageListener           | 2-3         | Background thread           |
| Frontend | AuctionDetailController updates | 3-4         | Platform.runLater           |
| Frontend | Countdown timer                 | 2           | Timeline-based              |
| Frontend | FXML updates                    | 1-2         | Minor polish                |
| Network  | ClientHandler updates           | 1-2         | Register/unregister         |
| Network  | SocketServer updates            | 1           | Pass broadcaster            |
| QA       | Broadcaster tests               | 2-3         | Unit + integration          |
| QA       | Concurrency tests               | 2-3         | Load testing                |
| QA       | Regression tests                | 1-2         | Ensure Sprint 3 works       |
| TOTAL    |                                 | 22-30 hours | ~1 week for 4 people        |

---

## SUCCESS CRITERIA (Go-Live)

Sprint 4 is complete when:

1. [x] Developer can start server -> start 2+ clients -> place bid in one -> other client sees update in real-time (no refresh)
2. [x] Countdown timer shows accurate remaining time
3. [x] All 50+ tests pass (unit + integration + concurrency)
4. [x] Code follows Google Java Style Guide
5. [x] Zero performance warnings (broadcasts < 100ms)
6. [x] Team walkthrough & approval

---

Document Version: Sprint 4 Implementation Guide v1.0  
Created: 18/04/2026
