# 🏆 BidMaster - Hệ Thống Đấu Giá Trực Tuyến Phân Tán (JavaFX)

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white) ![JavaFX](https://img.shields.io/badge/JavaFX-007396?style=for-the-badge&logo=java&logoColor=white) ![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white) ![Socket](https://img.shields.io/badge/TCP_Socket-000000?style=for-the-badge&logo=socket.io&logoColor=white)

**BidMaster** là một hệ thống đấu giá trực tuyến theo mô hình Client-Server phân tán (Distributed Architecture). Dự án được thiết kế với tiêu chuẩn phần mềm doanh nghiệp (Enterprise Standards), chú trọng vào hiệu năng xử lý đa luồng (Concurrency), bảo mật dữ liệu, và trải nghiệm người dùng (UX) hiện đại.

---

## 🏗 Cấu Trúc Dự Án (Multi-module Architecture)
Dự án được xây dựng dưới dạng **Multi-module Maven**, chia làm 3 module độc lập giúp đảm bảo nguyên lý DRY (Don't Repeat Yourself) và dễ dàng quản lý:
- `shared/`: Chứa các DTOs (Data Transfer Objects), Enums, và Giao thức mạng (Protocol) dùng chung giữa Client và Server.
- `server/`: Xử lý Logic nghiệp vụ (Business Logic), Socket I/O Broadcast, quản lý đa luồng (Concurrency), và tương tác với Cơ sở dữ liệu.
- `client/`: Giao diện người dùng đồ họa (GUI) được viết bằng JavaFX, chịu trách nhiệm hiển thị và giao tiếp mạng phi đồng bộ với Server.

## 🛠 Công Nghệ Tích Hợp (Tech Stack)
- **Ngôn ngữ:** Java 17
- **Giao diện (UI):** JavaFX với `AtlantaFX` CSS (Phong cách Neo-Brutalism hiện đại).
- **Cơ sở dữ liệu:** H2 Database (Embedded) + HikariCP Connection Pooling.
- **Giao tiếp mạng:** Raw TCP Sockets, luồng tin nhắn đa chiều, định dạng JSON qua Google Gson.
- **Caching:** Caffeine In-Memory Cache (High Performance).
- **Bảo mật:** Băm mật khẩu (Hashing) bằng BCrypt; Rate Limiting chống DDOS.
- **Logging:** SLF4J + Logback (Rolling File Appender).

---

## ✨ Tính Năng Nổi Bật (Key Features)
1. **Real-time Engine:** Mọi thao tác như đặt giá (bid), thêm sản phẩm mới đều được Server "Broadcast" trực tiếp đến tất cả các Client đang online gần như ngay lập tức mà không cần F5.
2. **Auto-bid & Anti-Sniping:** Tích hợp bộ máy đấu giá thông minh. Tự động trả giá thay người dùng (Auto-bid) và tự động cộng giờ (Extension) nếu có người nẫng tay trên ở những giây cuối.
3. **Data Integrity (Toàn vẹn dữ liệu):** Sử dụng `ReentrantLock` theo luồng (Per-user Lock) để xử lý triệt để bài toán Data Race trong giao dịch nạp/rút tiền tệ, một lỗi kinh điển của hệ thống đa luồng.
4. **Optimistic UI & Notification:** Hộp thư thông báo hỗ trợ đa ngôn ngữ, số lượng Unread Badge cập nhật ngay lập tức nhờ kỹ thuật "Cập nhật Lạc quan" (Optimistic Updates).
5. **Role-based Access:** Quản trị quyền hạn chặt chẽ với các vai trò: Admin (Xem biểu đồ, xác nhận thanh toán), Seller (Tạo phiên đấu giá), Bidder (Tham gia đấu giá).

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy (How to Run)

### 1. Build Project
Cài đặt **Java JDK 17** và **Maven**. Mở Terminal tại thư mục gốc của dự án và chạy:
```bash
mvn clean install
```

### 2. Khởi chạy Server
Server phải được bật đầu tiên để lắng nghe cổng `9090`.
```bash
mvn compile exec:java -pl server
```

### 3. Khởi chạy Client (Có thể mở nhiều Client)
Mở một tab terminal mới và chạy lệnh khởi động ứng dụng UI:
```bash
mvn javafx:run -pl client
```
*Mẹo: Để trải nghiệm tính năng Real-time, hãy mở 3 tab terminal để chạy 3 cửa sổ Client khác nhau.*

---
*Dự án thuộc môn học Lập trình Nâng Cao (LTNC).*
