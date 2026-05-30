# Auction System (JavaFX + Socket + DB)

## Tổng quan

Hệ thống đấu giá thời gian thực (real-time auction) cho nhiều người dùng cùng lúc. Client hiển thị UI JavaFX, trao đổi dữ liệu với Server qua Socket (JSON). Server quản lý phiên đấu giá, kiểm tra tính hợp lệ của lệnh đặt giá (bid), cập nhật giá hiện tại theo thời gian thực và ghi lịch sử/ dữ liệu vào CSDL.

## Mục tiêu dự án

- Xây dựng luồng Client-Server ổn định bằng TCP/IP Socket.
- Quản lý vòng đời phiên đấu giá: `OPEN -> RUNNING -> FINISHED`.
- Khớp lệnh/bid với ràng buộc “giá mới phải cao hơn giá hiện tại”.
- Tránh trượt giá khi nhiều người bid trong cùng thời điểm (đảm bảo concurrency).
- Cập nhật realtime cho tất cả client mà không polling (dùng broadcast/observer).
- Lưu trữ dữ liệu chuẩn hóa và xử lý lịch sử bid phục vụ QA và audit.
- Triển khai các cơ chế nâng cao: Anti-Sniping, Auto-Bidding.

## Công nghệ dự kiến

- Backend: Java (ServerSocket/Socket, multithreading, JSON parse bằng Gson/Jackson, JDBC).
- Frontend: JavaFX (FXML, Scene Builder, MVC, TableView, realtime UI cập nhật).
- DB: SQL (chuẩn hóa/bảng quan hệ, DAO pattern).
- Build/Project: Maven hoặc Gradle (khởi tạo ở Sprint 1).

## Chạy client JavaFX (hiện tại)

Module Maven `client/` (JavaFX + Socket JSON, phụ thuộc `shared`).

- **Yêu cầu**: JDK 17+ và Maven.
- **Chạy app**: tại root `mvn -pl client javafx:run` hoặc trong `client/` chạy `mvn javafx:run`
- **Entry point**: `app.MainApp`
- **Socket client**: `app.net.SocketClient`

## Kiến trúc hướng tới

- **MVC cho JavaFX**: tách Model (dữ liệu/DTO), View (FXML), Controller (logic UI và gọi Network layer).
- **Observer/Broadcast**: khi bid hợp lệ, Server đẩy thông báo realtime tới tất cả client đang kết nối.
- **State Machine**: quản lý trạng thái phiên đấu giá và điều kiện chuyển trạng thái.
- **Concurrency Control**: khóa tài nguyên Auction khi xử lý lệnh bid để ngăn lost update.
- **Thread Pool + ExecutorService**: mở rộng khả năng nhận nhiều kết nối và lệnh bid đồng thời.

## Phân công nhóm

- **Thành viên 1 (Leader)**: UML, khung build (Maven/Gradle), phân quyền vai trò, logic state machine.
- **Thành viên 2 (Frontend)**: Wireframe + FXML tĩnh, UI JavaFX, event handling, realtime hiển thị realtime chart.
- **Thành viên 3 (Network)**: ServerSocket/Socket, test `Ping - Pong`, endpoint nhận JSON, luồng xử lý đa client, đảm bảo integrity data stream.
- **Thành viên 4 (DB/QA)**: Thiết kế schema CSDL, viết DAO (CRUD), unit test + CI/CD, xử lý exception/transaction.

## Lộ trình Sprint (8 tuần)

### Sprint 1 (24/03 – 30/03): Khởi tạo & Sơ đồ cấu trúc

#### Leader

- Phác thảo UML Class Diagram (Entity, Item, User...).
- Setup Maven/Gradle.
- Khởi tạo GitHub.
- Cần học: 4 nguyên tắc OOP, Singleton Pattern.

#### Frontend

- Dựng Wireframe và kéo thả giao diện `fxml` tĩnh cơ bản.
- Cần học: MVC của JavaFX và Scene Builder.

#### Network

- Mở port `ServerSocket/Socket`.
- Test kết nối gửi chuỗi `Ping - Pong`.
- Cần học: TCP/IP Socket, Gson/Jackson để parse JSON.

#### DB/QA

- Cài đặt CSDL, thiết kế schema các bảng thông tin.
- Cần học: Normalization (chuẩn hóa SQL).

### Sprint 2 (31/03 – 06/04): Quản lý dữ liệu tĩnh & CRUD

#### Leader

- Code logic phân quyền vai trò người dùng (`Admin`, `Seller`, `Bidder`).
- Cần học: State Machine để chuẩn bị đổi trạng thái phiên đấu giá.

#### Frontend

- Hoàn thiện màn hình Đăng nhập/Đăng ký và form quản lý sản phẩm (Seller).
- Cần học: Lấy dữ liệu từ form và hiển thị danh sách lên `TableView`.

#### Network

- Viết API/Socket endpoint để nhận dữ liệu từ Client (JSON) và chuyển Server xử lý.
- Cần học: Luồng dữ liệu Client-Server.

#### DB/QA

- Viết lớp DAO cho Thêm/Sửa/Xóa sản phẩm và người dùng.
- Cần học: DAO pattern, JDBC Connection, câu lệnh SQL cơ bản.

### Sprint 3 (07/04 – 13/04): Khớp lệnh cơ bản

#### Mục tiêu chung

- Khi người dùng đặt giá, hệ thống xác định lệnh có hợp lệ hay không.

#### Leader

- Logic kiểm tra bid: **giá mới bắt buộc cao hơn giá hiện tại**.
- Cập nhật trạng thái người dẫn đầu (highest bidder).
- Cần học: BigDecimal cho tiền tệ an toàn.

#### Frontend

- Màn hình chi tiết sản phẩm với nút “Đặt giá”.
- Cần học: `Event Handling` với `setOnAction`.

#### Backend

- Phân luồng Thread pool trên Server cho nhiều lệnh `BidTransaction` tới.
- Cần học: `ExecutorService`.

#### DB/QA

- Ghi nhận lịch sử `BidTransaction` vào DB.
- Bắt lỗi: “Đặt giá thấp hơn” và “Đấu giá khi phiên đã đóng”.
- Cần học: SQL Transaction để tránh ghi đè sai.

### Sprint 4 (14/04 – 20/04): Observer Pattern & Cập nhật Realtime

#### Mục tiêu chung

- Thay vì F5 liên tục, Server chủ động đẩy giá mới lên màn hình tất cả người xem.

#### Leader

- Tích hợp bộ đếm thời gian tự động chuyển vòng đời phiên: `OPEN -> RUNNING -> FINISHED`.
- Cần học: Task Scheduling.

#### Frontend

- UI đấu giá trực tiếp hiển thị giá nhảy realtime.
- Cần học: `Platform.runLater()` để UI không bị đơ khi nhận tín hiệu mạng.

#### Network

- Áp dụng Observer/Broadcast: bid hợp lệ -> broadcast tới toàn bộ client đang kết nối.
- Cần học: Thread-safe notify, **không dùng polling**.

#### DB/QA

- Cập nhật giá hiện tại cao nhất vào bảng thông tin sản phẩm.
- Cần học: SQL Trigger hoặc logic DAO tối ưu read/write.

### Sprint 5 (21/04 – 27/04): Concurrency & Chống trượt giá

#### Mục tiêu chung

- Tránh tuyệt đối hiện tượng trượt giá khi nhiều người bấm cùng một mili-giây.

#### Leader

- Dùng `synchronized` hoặc `ReentrantLock` để khóa đối tượng `Auction` khi xử lý bid.
- Cần học: Java Multithreading chuyên sâu.

#### Frontend

- Debounce UI: chặn spam click nút “Đặt giá”.
- Hiển thị pop-up nếu bị từ chối do mạng chậm.
- Cần học: vô hiệu hóa nút theo thời gian chờ.

#### Network

- Đảm bảo thứ tự gói tin JSON truyền đi không bị lộn xộn gây sai lệch giá trị.
- Cần học: TCP Data Stream Integrity.

#### DB/QA

- Xử lý triệt để exception: lỗi dữ liệu, rớt mạng giữa chừng.
- Viết kịch bản test giả lập 100 user cùng bid.
- Cần học: Exception Handling nâng cao.

### Sprint 6 (28/04 – 04/05): Anti-Sniping

#### Mục tiêu chung

- Chống người chơi “bắn lệnh” vào giây cuối để nẫng tay trên.

#### Leader

- Logic: có bid mới trong `X` giây cuối -> tự động cộng thêm `Y` giây.
- Cần học: đồng bộ biến thời gian trong đa luồng.

#### Frontend

- Đồng hồ đếm ngược trực quan.
- UI phải mượt và nhảy thêm giây khi bị Anti-sniping kích hoạt.
- Cần học: `Timeline` và `Animation` trong JavaFX.

#### Network

- Đồng bộ `endTime` qua Socket để tất cả client hiển thị đúng lượng thời gian được gia hạn.
- Cần học: tối ưu kích thước gói tin mạng.

#### DB/QA

- Update kéo dài `endTime` dưới DB.
- Kiểm tra lỗi logic: `endTime` mới < thời điểm hiện tại.
- Cần học: xử lý Datetime trong SQL.

### Sprint 7 (05/05 – 11/05): Auto-Bidding

#### Mục tiêu chung

- Xây dựng bot tự động trả giá như Expert Advisor (EA).

#### Leader

- Hàng đợi bot tự động (auto-bid queue).
- Thuật toán so sánh nhiều lệnh auto-bid, ưu tiên thời gian đăng ký.
- Tính toán cộng thêm bước giá (`increment`) không vượt trần (`maxBid`).
- Xử lý xung đột bid.
- Cần học: `PriorityQueue`.

#### Frontend

- Bảng cài đặt thông số Bot: `maxBid` và `increment`.
- Cần học: Form Validation.

#### Network

- Đảm bảo luồng bot chạy ngầm không làm nghẽn kết nối người dùng đấu giá thủ công.
- Cần học: Background Jobs thread optimization.

#### DB/QA

- Tạo schema/bảng lưu setting auto-bid theo từng người dùng.
- Cần học: tối ưu Query để bot lấy dữ liệu nhanh.

### Sprint 8 (12/05 – 18/05): Trực quan hóa & Tích hợp

#### Mục tiêu chung

- Trở thành giao diện chuyên nghiệp với biểu đồ theo dõi lực mua và update realtime mượt.

#### Leader

- Rà soát áp dụng Design Pattern: Singleton, Factory, Observer.
- Review mã nguồn toàn nhóm + refactoring.
- Cần học: Refactoring (mã sạch).

#### Frontend

- `LineChart` vẽ Bid History Visualization.
- Trục X là thời gian, trục Y là giá.
- Đồ thị tự chạy và update mượt mỗi khi có giá mới hợp lệ.
- Cần học: `API XYChart.Series` JavaFX.

#### Network

- Dữ liệu realtime cho biểu đồ hoạt động liên tục, độ trễ thấp nhất.
- Cần học: auto-reconnect khi ngắt kết nối.

#### DB/QA

- Viết Unit Test JUnit 5 cho logic quan trọng.
- Cấu hình GitHub Actions CI/CD.
- Đảm bảo tuân thủ Google Java Style Guide.
- Cần học: YAML CI/CD.

## Checklist đầu ra (gợi ý)

- Có UML Class Diagram và tài liệu thiết kế sơ bộ.
- Có server nhận kết nối và parse JSON đúng.
- CRUD chạy ổn định trên DB với DAO/JDBC.
- Bid hợp lệ/không hợp lệ đúng theo trạng thái phiên.
- Realtime broadcast hoạt động, UI cập nhật mượt với `Platform.runLater()`.
- Không lost update khi nhiều user bid đồng thời (có test/benchmark).
- Có Anti-Sniping + đồng bộ endTime cho client.
- Có Auto-bidding theo `maxBid` và `increment`.
- Có Bid History chart realtime + CI chạy tự động.

## Regression (DoD Sprint 3–8 — đối chiếu `sprint.md`)

Chạy **`mvn -q test`** từ thư mục gốc (module `shared`, `server`, `client`).

| Tiêu chí | Cách kiểm tra nhanh |
|----------|---------------------|
| **Ví end-to-end** | Đăng nhập → màn chi tiết phiên: hiển thị balance/reserved/available; Deposit/Withdraw; bid khi thiếu tiền nhận `INSUFFICIENT_FUNDS` → dialog nạp → đặt lại bid. |
| **Broadcast** | Hai client cùng xem một phiên: bid bên A → B thấy giá và ví cập nhật qua `AUCTION_UPDATE` / `WALLET_UPDATE` (không F5). |
| **Trạng thái phiên** | `AuctionStateManager` tick → OPEN/RUNNING/FINISHED; client nhận `AUCTION_STATE_CHANGE` (và badge extended khi anti-sniping). |
| **Concurrency + ví** | Unit: `AuctionServiceTest` (`concurrency_fifty_threads_*`) + `WalletServiceTest` (reserved ≥ 0). |
| **Auto-bid + ví** | Bot không đủ tiền → không tăng giá (`autoBid_skipped_when_bot_wallet_insufficient`). |
| **Telemetry broadcast** | `BroadcastManager` có counter/latency/failed (log/metrics). |
| **CI** | Workflow `.github/workflows/ci.yml` chạy `mvn -q test`. |

## Ghi chú

Repo này sẽ được tổ chức dần theo các phần:

- `client/` (JavaFX)
- `server/` (Socket server, business logic, state machine)
- `db/` (schema, migrations nếu có)
- `docs/` (UML, tài liệu hướng dẫn, test plan)

