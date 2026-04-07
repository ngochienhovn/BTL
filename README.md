# 🔨 BidMaster - Hệ thống Đấu giá Trực tuyến (Online Auction System)

**Dự án Bài tập lớn Môn Lập trình Nâng cao (LTNC)**
**Thành viên thực hiện:** NgocHien & Team
**Kiến trúc:** Client-Server (Socket/TCP) | MVC | Multi-module Maven

---

## 🌟 Tổng quan Dự án
BidMaster là một nền tảng đấu giá trực tuyến mạnh mẽ, được xây dựng trên nền tảng Java với cấu trúc module tách biệt, hỗ trợ đa người dùng và cập nhật dữ liệu thời gian thực. Hệ thống tập trung vào tính toàn vẹn của dữ liệu trong môi trường cạnh tranh (Concurrency) và trải nghiệm người dùng hiện đại với JavaFX.

## 🏗️ Kiến trúc Hệ thống
Dự án được tổ chức theo mô hình **Multi-module Maven** giúp quản lý phụ thuộc và tái sử dụng mã nguồn hiệu quả:
- **`shared`**: Chứa các đối tượng dùng chung (DTO, Protocol, Enum) giữa Client và Server.
- **`server`**: Xử lý logic nghiệp vụ, quản lý phiên đấu giá, kết nối Cơ sở dữ liệu (H2) và điều phối Socket.
- **`myclient`**: Giao diện người dùng JavaFX hiện đại, sử dụng cơ chế truyền tin Socket để giao tiếp với Server.

## 🚀 Tính năng Nổi bật

### 1. Chức năng Cốt lõi (Mandatory)
- **Quản lý người dùng:** Đăng ký, Đăng nhập, Phân quyền (Admin, Seller, Bidder).
- **Quản lý phiên đấu giá:** Tạo mới, chỉnh sửa, xóa sản phẩm và thiết lập thời gian đấu giá.
- **Đấu giá trực tuyến:** Đặt giá (Bid), kiểm tra giá sàn, xác định người dẫn đầu.
- **Tự động hóa:** Tự động mở/đóng phiên đấu giá chính xác theo thời gian thực.

### 2. Kỹ thuật Nâng cao (Advanced & Refactored)
- **Realtime Update:** Sử dụng cơ chế Observer và Socket Broadcast để cập nhật giá mới tức thì tới toàn bộ người xem.
- **Anti-Sniping:** Tự động gia hạn thêm 60 giây nếu có lượt bid phát sinh trong 30 giây cuối của phiên.
- **Auto-Bidding:** Thuật toán sử dụng PriorityQueue để tự động trả giá thay người dùng theo ngân sách tối đa.
- **Concurrency Control:** Sử dụng `ReentrantLock` theo từng phiên đấu giá tại `AuctionService` để tránh lỗi *Lost Update* khi có nhiều người bid cùng lúc.
- **Clean Architecture:** Logic đấu giá được tách biệt hoàn toàn sang `BidProcessor` và `AuctionMapper` để mã nguồn luôn "sạch", dễ bảo trì.

## 🛠️ Công nghệ Sử dụng
- **Ngôn ngữ:** Java 17+
- **Giao diện:** JavaFX 21 (AtlantaFX Theme)
- **Cơ sở dữ liệu:** H2 Database (File-based)
- **Build Tool:** Maven 3.8+
- **Giao thức:** TCP Socket (JSON Serialization)
- **Testing:** JUnit 5 (Unit Test cho Logic nghiệp vụ)

## 📖 Hướng dẫn Khởi chạy

### 1. Biên dịch Dự án
Tại thư mục gốc, chạy lệnh:
```bash
# Biên dịch Server và Shared
mvn clean install

# Biên dịch Client (Module riêng)
mvn -f myclient/pom.xml clean compile
```

### 2. Chạy Server
```bash
cd server
mvn exec:java -Dexec.mainClass="com.ltnc.auction.server.ServerMain"
```
### 3. Chạy Client
```bash
cd myclient
mvn javafx:run
```

## 🧪 Kiểm thử
Chạy bộ Unit Test cho logic đấu giá (Concurrency, Anti-sniping, Auto-bid):
```bash
mvn -pl server test
```

## 📂 Cấu trúc Thư mục Chính
```text
Project/
├── server/          # Mã nguồn phía Server
├── myclient/        # Mã nguồn phía Client (JavaFX)
├── shared/          # Các lớp dùng chung (DTO, Protocol)
├── data/            # Lưu trữ file Database (H2)
├── docs/            # Tài liệu thiết kế, sơ đồ UML, ARCHITECTURE
└── pom.xml          # Cấu hình Maven Parent
```