# 🚀 CHI TIẾT CÁC KỸ THUẬT VÀ THÀNH TỰU CỦA DỰ ÁN BIDMASTER

Tài liệu này liệt kê chi tiết toàn bộ các kỹ thuật lập trình nâng cao (Advanced Programming Techniques) và toàn bộ các tính năng đã được phát triển trong dự án BidMaster.

## PHẦN 1: TÍNH NĂNG HỆ THỐNG (SYSTEM FEATURES)

Hệ thống được thiết kế với đầy đủ các luồng nghiệp vụ của một sàn đấu giá thực tế, chia làm 3 phân hệ người dùng chính:

### 1. Phân hệ Quản trị viên (Admin Dashboard)
- **Bảng điều khiển Thống kê (Statistics):** Hiển thị số lượng Người dùng, số lượng Phiên đấu giá phân chia theo trạng thái (OPEN, RUNNING, FINISHED, CANCELED, PAID).
- **Biểu đồ Phân tích (Charts):** Tích hợp JavaFX `PieChart` phân tích tỷ trọng các danh mục sản phẩm đang được đấu giá.
- **Xuất Báo cáo (Data Export):** Tính năng trích xuất toàn bộ dữ liệu đấu giá ra file `.csv` chuẩn Excel với đầy đủ thông tin (Giá khởi điểm, Người bán, Người thắng cuộc, v.v.).
- **Quản lý Tài khoản (User Management):** Tìm kiếm, phân quyền (Phân cấp Role: Admin, Seller, Bidder), và Xóa tài khoản (kèm logic cảnh báo nếu user đang có giao dịch chưa hoàn tất).
- **Quản lý Đấu giá & Thanh toán (Auction & Payment):** Admin có quyền Hủy (Cancel), Xóa (Delete), Chỉnh sửa (Edit) mọi phiên đấu giá. Đặc biệt là tính năng **Confirm Payment** (Chuyển tiền từ ví Bidder sang ví Seller khi phiên đấu giá kết thúc thành công).
- **Audit Logs:** Theo dõi toàn bộ lịch sử đặt giá (Bid Log) theo thời gian thực (Real-time).

### 2. Phân hệ Người bán (Seller)
- **Quản lý Sản phẩm (Item Management):** Đăng bán sản phẩm (kèm URL hình ảnh, mô tả, giá sàn). Hỗ trợ chuẩn CRUD (Create, Read, Update, Delete).
- **Tạo Phiên đấu giá (Create Auction):** Lấy sản phẩm từ kho đồ cá nhân để lên sàn. Thiết lập thời gian diễn ra phiên đấu giá (Start Time / End Time).

### 3. Phân hệ Người đấu giá (Bidder)
- **Real-time Bidding:** Nút "Place Bid" kết nối trực tiếp với luồng Socket, cập nhật giá thầu của toàn bộ người dùng khác ngay lập tức mà không cần tải lại trang.
- **Biểu đồ Đấu giá (LineChart):** Vẽ biểu đồ biến động giá thầu theo thời gian thực tại trang Chi tiết đấu giá.
- **Đấu giá Tự động (Auto-Bid):** Người dùng cài đặt mức giá tối đa (Max Bid) và bước nhảy (Increment). Hệ thống Server sẽ tự động thay mặt người dùng giành lại vị thế mỗi khi bị người khác trả giá cao hơn.
- **Chống Bắn tỉa (Anti-Sniping):** Tự động gia hạn thời gian (ví dụ thêm 60 giây) nếu có người đặt giá trong những giây cuối cùng của phiên đấu giá, đảm bảo tính công bằng.
- **Hệ thống Thông báo (Notification System):** Nhận thông báo thời gian thực về việc bị trả giá cao hơn (Outbid), thắng đấu giá (Win), và phiên đấu giá được gia hạn (Extended). Chuông thông báo hiển thị số lượng chưa đọc.
- **Quản lý Ví (Wallet Management):** Tính năng nạp tiền (Deposit) và Rút tiền (Withdraw). Hiển thị song song "Balance" (Số dư thực) và "Reserved" (Số dư đang bị khóa do đang dẫn đầu một phiên đấu giá). Bảng lịch sử giao dịch (Transaction Logs).
- **Danh sách theo dõi (Watchlist):** Lưu trữ các phiên đấu giá yêu thích để tiện theo dõi.

---

## PHẦN 2: KIẾN TRÚC & KỸ THUẬT LẬP TRÌNH (ENGINEERING EXCELLENCE)

Dự án vượt xa khuôn khổ của một bài tập lớn thông thường nhờ áp dụng các tiêu chuẩn thiết kế cấp doanh nghiệp (Enterprise Standards):

### 1. Kiến trúc Đa phân hệ (Multi-module Architecture & DRY Principle)
- Sử dụng Maven Multi-module chia tách: `client-javafx`, `server`, và `shared`.
- Giao thức mạng và các Đối tượng truyền tải (DTOs) được tập trung tại module `shared`, loại bỏ hoàn toàn sự lặp lại mã nguồn (Don't Repeat Yourself - DRY) giữa Client và Server.

### 2. Giao tiếp Mạng Phi đồng bộ & Đa Luồng (Asynchronous Socket & Concurrency)
- **Custom Protocol over TCP:** Tự xây dựng giao thức truyền tin hai chiều (Bi-directional) qua Socket bằng định dạng JSON (Gson library).
- **ThreadPool Broadcasting:** Server sử dụng `ExecutorService` với 16 luồng ảo để đẩy (Broadcast) dữ liệu đến các Client. Tránh hiện tượng "Nút thắt cổ chai" (Bottleneck) khi mạng của một Client chậm làm treo toàn bộ Server.
- **Heartbeat & Ping Sweep:** Khắc phục triệt để lỗi Rò rỉ bộ nhớ (Memory Leak) do kết nối ma (Zombie connections). Client gửi Ping ngầm mỗi 15 giây; Server quét mỗi 30 giây để cắt đứt các kết nối đã chết.

### 3. An toàn Dữ liệu & Xử lý Giao dịch Đồng thời (Data Integrity)
- **Concurrency Locks (Chống Data Race):** Lớp `WalletService` sử dụng `ReentrantLock` theo luồng khóa từng ID người dùng (`lockFor(userId)`). Đảm bảo tính nhất quán của số dư ví (Balance) khi hàng chục người nạp tiền/trừ tiền cùng 1 phần nghìn giây.
- **Graceful Shutdown:** Server cài đặt `Runtime.getRuntime().addShutdownHook` để dọn dẹp các ThreadPool và đóng kết nối Database an toàn trước khi tắt máy.

### 4. Hiệu năng & Tối ưu hóa Database (Performance Tuning)
- **Caffeine Cache:** Tích hợp bộ nhớ đệm In-Memory tốc độ cao 2 lớp cho các dữ liệu tĩnh (như Auction List), giảm tải tối đa các luồng I/O từ Database.
- **Database B-Tree Indexing:** Hệ thống được tối ưu bằng các câu lệnh `CREATE INDEX` trên các bảng `users`, `items`, `auctions`, `bid_transactions`. Điều này giảm độ phức tạp truy vấn từ $O(N)$ (Full Table Scan) xuống $O(log N)$, cho phép hệ thống đáp ứng lượng dữ liệu khổng lồ.
- **HikariCP Connection Pool:** Tái sử dụng các luồng kết nối cơ sở dữ liệu thay vì khởi tạo lại liên tục.

### 5. Bảo mật Hệ thống (Security)
- **BCrypt Hashing:** Mật khẩu được băm và thêm "muối" (Salt) với độ khó 12. Không thể bị tấn công bằng Brute-force hay Rainbow Table.
- **Rate Limiting:** Tích hợp bộ giới hạn lưu lượng (150 requests/s) ở phía Server để chống spam và tấn công từ chối dịch vụ (DDoS quy mô nhỏ).
- **Data Validation:** Sử dụng Regex chuẩn để validate cấu trúc Email và độ khó Mật khẩu ở cả Client và Server.

### 6. Giao diện Người Dùng (UI/UX)
- **Neo-Brutalism Design & AtlantaFX:** Ứng dụng CSS nâng cao để tạo bóng đổ (Drop Shadow), Gradient, và các bo góc mượt mà.
- **UI Non-blocking:** Không làm đơ (Freeze) giao diện. Mọi tác vụ I/O được đẩy xuống luồng nền (`new Thread()`), chỉ cập nhật dữ liệu hiển thị bằng `Platform.runLater()`.
- **Skeleton Loading:** Tích hợp hiệu ứng "khung xương" tải trang hiện đại trong lúc chờ dữ liệu từ mạng.

### 7. Tiêu chuẩn Thương mại (Production Standards)
- **Audit Logging (Logback):** Tích hợp Rolling File Appender. Mọi hoạt động của Server được ghi ra thư mục `logs/server.log`, tự động cắt file theo ngày và lưu trữ tối đa 30 ngày.
- **Giao diện đa nền tảng:** Code giao diện tương thích tốt bất kể hệ điều hành nhờ cấu trúc Anchor/VBox/HBox chuẩn của JavaFX.
