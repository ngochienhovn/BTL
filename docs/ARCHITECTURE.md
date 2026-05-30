# Kiến trúc hệ thống – Hệ thống Đấu giá Trực tuyến

## 1. Tổng quan

Hệ thống gồm hai tiến trình chạy độc lập:

```
┌─────────────────────────────┐         TCP Socket          ┌────────────────────────────────┐
│          CLIENT             │◄──────── port 9090 ─────────►│           SERVER               │
│       (JavaFX / client)     │       JSON (mỗi dòng)        │  (Java ServerSocket / server)  │
└─────────────────────────────┘                              └────────────────────────────────┘
```

Client và Server giao tiếp bằng **JSON qua TCP Socket**. Mỗi message là một dòng JSON.
Định nghĩa message nằm trong module **`shared/`** (dùng chung cho cả hai phía).

---

## 2. Cấu trúc thư mục

```
Project/
├── server/          ← Backend: socket server, business logic, DB
├── client/          ← Frontend: JavaFX GUI
├── shared/          ← Giao thức chung: message types, DTO
├── db/              ← Schema SQL, migration scripts
└── docs/            ← Tài liệu, UML, sprint plans
```

---

## 3. Kiến trúc Server (MVC phía Server)

```
Yêu cầu từ Client (JSON)
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  TẦNG NETWORK (Thành viên Network – TV3)                    │
│                                                             │
│  SocketServer.java                                          │
│  └── Mở port 9090, nhận kết nối, tạo ClientHandler mới     │
│      mỗi khi có client → giao cho Thread Pool (16 thread)  │
│                                                             │
│  ClientHandler.java  ← CONTROLLER của Server               │
│  └── Đọc JSON → parse → gọi Service → ghi JSON response    │
│      handleRequest() = bộ điều phối (switch-case type)     │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  TẦNG SERVICE / BUSINESS LOGIC (Thành viên Leader – TV1)    │
│                                                             │
│  AuthService.java    → đăng ký, đăng nhập, quản lý user    │
│  AuctionService.java → đặt giá, anti-sniping, auto-bid     │
│                        state machine, broadcast             │
│  ItemService.java    → CRUD sản phẩm                       │
│  AuctionStateManager.java → tick() mỗi 1 giây (OPEN→RUNNING   │
│                           → FINISHED)                      │
│  BroadcastManager.java → push realtime update tới clients  │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  TẦNG MODEL (Thành viên Leader – TV1)                       │
│                                                             │
│  Entity (abstract) ← User (abstract) ← Bidder/Seller/Admin │
│                    ← Item (abstract)  ← Electronics/Art/   │
│                                          Vehicle            │
│  Auction.java       (có ReentrantLock cho concurrency)      │
│  BidTransaction.java                                        │
│  AutoBidConfig.java                                         │
│  ItemFactory.java   (Factory Pattern)                       │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  TẦNG DAO / DATABASE (Thành viên DB/QA – TV4)               │
│                                                             │
│  DBConnection.java    → Singleton, kết nối H2 database      │
│  SchemaInitializer.java → tạo bảng khi khởi động           │
│  DataSeeder.java      → dữ liệu mẫu để test                │
│                                                             │
│  UserDAO.java         → CRUD bảng users                     │
│  ItemDAO.java         → CRUD bảng items                     │
│  AuctionDAO.java      → CRUD bảng auctions                  │
│  BidDAO.java          → CRUD bảng bid_transactions          │
│  AutoBidDAO.java      → CRUD bảng auto_bid_configs          │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. Kiến trúc Client (MVC phía Client)

```
Người dùng tương tác (click, nhập liệu)
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  TẦNG VIEW – Thành viên Frontend (TV2)                      │
│                                                             │
│  src/view/*.fxml                                            │
│  ├── sign-in.fxml, sign-up.fxml                             │
│  ├── home-page.fxml        ← danh sách phiên đấu giá        │
│  ├── auction-detail.fxml   ← chi tiết, đặt giá, biểu đồ   │
│  ├── seller-items.fxml     ← quản lý sản phẩm              │
│  ├── admin-dashboard.fxml  ← quản trị hệ thống             │
│  └── ...                                                    │
│                                                             │
│  CSS: src/resources/css/                                    │
└─────────────────────────────────────────────────────────────┘
        │  JavaFX bind
        ▼
┌─────────────────────────────────────────────────────────────┐
│  TẦNG CONTROLLER – Thành viên Frontend (TV2)                │
│                                                             │
│  src/app/controller/                                        │
│  ├── AuthController.java        ← xử lý đăng nhập/ký       │
│  ├── HomePageController.java    ← tìm kiếm, lọc, watchlist │
│  ├── AuctionDetailController.java ← đặt giá, biểu đồ       │
│  ├── SellerItemsController.java ← CRUD sản phẩm            │
│  ├── AdminDashboardController.java ← quản trị              │
│  └── ...                                                    │
│                                                             │
│  Controller KHÔNG gọi SocketClient trực tiếp.              │
│  Controller chỉ gọi Service interface (IAuctionService...) │
└─────────────────────────────────────────────────────────────┘
        │  gọi qua interface
        ▼
┌─────────────────────────────────────────────────────────────┐
│  TẦNG SERVICE / MODEL – Thành viên Network (TV3)            │
│                                                             │
│  src/app/service/                                           │
│  ├── IAuctionService.java  ← interface (không biết network) │
│  ├── IUserService.java                                      │
│  ├── IItemService.java                                      │
│  │                                                          │
│  ├── NetworkAuctionService.java  ← gửi request qua socket  │
│  ├── NetworkUserService.java      cache dữ liệu tại client │
│  ├── NetworkItemService.java      xử lý broadcast update   │
│  │                                                          │
│  ├── AuctionServiceMock.java  ← dữ liệu giả (khi offline)  │
│  ├── UserServiceMock.java                                   │
│  └── ItemServiceMock.java                                   │
└─────────────────────────────────────────────────────────────┘
        │
        ▼
┌─────────────────────────────────────────────────────────────┐
│  TẦNG NETWORK – Thành viên Network (TV3)                    │
│                                                             │
│  src/app/net/                                               │
│  ├── SocketClient.java   ← Singleton TCP connection         │
│  │   ├── sendAndReceive() → gửi request, chờ response       │
│  │   ├── Reader Thread   → đọc JSON liên tục từ server     │
│  │   └── Broadcast       → AUCTION_UPDATE → listeners       │
│  ├── AppRequest.java     ← format message Client→Server     │
│  ├── AppResponse.java    ← format message Server→Client     │
│  └── AppMessageType.java ← enum các loại message           │
└─────────────────────────────────────────────────────────────┘
```

---

## 5. Luồng dữ liệu – Ví dụ: Đặt giá (Place Bid)

```
[AuctionDetailController]
    │  1. User bấm nút "Đặt giá"
    │  2. Gọi auctionService.placeBid(user, auctionId, amount)
    ▼
[NetworkAuctionService]
    │  3. Tạo AppRequest { type: PLACE_BID, auctionId, email, bidAmount }
    │  4. Gọi SocketClient.sendAndReceive(request)
    ▼
[SocketClient]
    │  5. Serialize request → JSON string
    │  6. Gửi JSON qua TCP socket
    ▼
[SERVER – ClientHandler]
    │  7. Nhận JSON, parse thành ClientToServerMessage
    │  8. handleRequest() → handlePlaceBid()
    │  9. Gọi AuctionService.placeBid()
    ▼
[SERVER – AuctionService]
    │  10. Lock auction (ReentrantLock)
    │  11. Validate giá
    │  12. Cập nhật DB + cache
    │  13. Anti-Sniping check
    │  14. Auto-Bid chain
    │  15. broadcastAuctionUpdate() → BroadcastManager
    │  16. Unlock + trả về BidResult
    ▼
[SERVER – BroadcastManager]
    │  17. Gửi AUCTION_UPDATE JSON tới TẤT CẢ client đang kết nối
    ▼
[SocketClient – Reader Thread] (chạy song song ở mỗi client)
    │  18. Nhận AUCTION_UPDATE → gọi broadcastListeners
    ▼
[NetworkAuctionService.handleBroadcast()]
    │  19. Cập nhật localAuctions cache
    │  20. notifyObservers()
    ▼
[AuctionDetailController.onAuctionsUpdated()]
    │  21. Platform.runLater(() -> cập nhật UI)
    ▼
[Giao diện người dùng cập nhật giá mới]
```

---

## 6. Phân công và file phụ trách

| Thành viên | Vai trò | File chính phụ trách |
|---|---|---|
| **TV1 – Leader** | State machine, concurrency, design pattern | `model/`, `service/AuctionService.java`, `service/AuctionStateManager.java` |
| **TV2 – Frontend** | JavaFX, FXML, UI event | `src/view/*.fxml`, `src/app/controller/` |
| **TV3 – Network** | Socket, Client-Server communication | `network/SocketServer.java`, `network/ClientHandler.java`, `src/app/net/`, `src/app/service/Network*.java` |
| **TV4 – DB/QA** | Database, DAO, testing | `db/`, `dao/`, `db/SchemaInitializer.java`, `db/DataSeeder.java`, tests |

---

## 7. Design Pattern áp dụng

| Pattern | Nơi dùng | Mục đích |
|---|---|---|
| **Singleton** | `AuthService`, `AuctionService`, `BroadcastManager`, `SocketClient`, `NetworkAuctionService` | Một instance duy nhất trong JVM |
| **Factory Method** | `ItemFactory.java` | Tạo `Electronics` / `Art` / `Vehicle` theo type |
| **Observer** | `BroadcastManager` → `ClientHandler`; `NetworkAuctionService` → `Controller` | Push realtime update |
| **MVC** | JavaFX FXML (View) + Controller + Service (Model) | Tách UI khỏi logic |
| **DAO** | `UserDAO`, `ItemDAO`, `AuctionDAO`... | Tách logic DB khỏi business logic |
| **Strategy** (implicit) | `IAuctionService` interface → Network hoặc Mock | Hoán đổi implementation dễ dàng |

---

## 8. Concurrency (Xử lý đồng thời)

| Vấn đề | Giải pháp | File |
|---|---|---|
| Nhiều client bid cùng lúc → lost update | `ReentrantLock` per-Auction | `Auction.java`, `AuctionService.placeBid()` |
| Server phục vụ nhiều client cùng lúc | `ExecutorService` Thread Pool (16 threads) | `SocketServer.java` |
| Cache phiên đấu giá đọc/ghi từ nhiều thread | `ConcurrentHashMap` | `AuctionService.java` |
| Broadcast listeners đọc/ghi từ nhiều thread | `CopyOnWriteArrayList` | `BroadcastManager.java`, `SocketClient.java` |
| UI không bị đơ khi nhận data từ mạng | `Platform.runLater()` | Mỗi `Controller` |

---

## 9. Chạy hệ thống

```bash
# Terminal 1 – Khởi động Server
cd server
mvn package -q
java -jar target/server-fat.jar

# Terminal 2 – Khởi động Client (JavaFX)
cd client
mvn javafx:run
```

Server mặc định lắng nghe tại `localhost:9090`.
Client tự động kết nối; nếu thất bại → chạy ở MOCK mode với dữ liệu giả.
