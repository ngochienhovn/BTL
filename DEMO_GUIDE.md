# 🎥 HƯỚNG DẪN DEMO TÍNH NĂNG (DEMO GUIDE)

Tài liệu này cung cấp kịch bản Step-by-Step chi tiết nhất để trình bày (demo) toàn bộ các tính năng của hệ thống BidMaster. Để quá trình demo trực quan, hãy đảm bảo bạn có đủ không gian màn hình hoặc có thể chia thành 3 cửa sổ nhỏ.

---

## 🛠 Chuẩn Bị Môi Trường Demo

### Bước 1: Khởi Động Server (Cửa sổ 1)
- Mở **Terminal 1** tại thư mục dự án và chạy:
  ```bash
  mvn compile exec:java -pl server
  ```
- Chờ đến khi thấy thông báo: `INFO: Server is running on port 9090...`

### Bước 2: Khởi Động 3 Client (Cửa sổ 2, 3, 4)
- Mở **Terminal 2, 3, 4** (nên dùng chế độ chia split-screen) và chạy chung một lệnh trên cả 3 tab:
  ```bash
  mvn javafx:run -pl client
  ```
- Sắp xếp 3 cửa sổ ứng dụng BidMaster lên màn hình để dễ dàng quan sát tương tác Real-time:
  - **Cửa sổ Trái (Client 1):** Sẽ dùng cho `demo@gmail.com` (Người mua 1).
  - **Cửa sổ Giữa (Client 2):** Sẽ dùng cho `binh@gmail.com` (Người mua 2).
  - **Cửa sổ Phải (Client 3):** Sẽ dùng cho `seller@gmail.com` (Người bán) hoặc `admin@gmail.com` (Quản trị viên).

### Bước 3: Đăng Nhập
- **Client 1:** Đăng nhập với `demo@gmail.com` / `Demo@123`
- **Client 2:** Đăng nhập với `binh@gmail.com` / `Binh@123`
- **Client 3:** Đăng nhập với `seller@gmail.com` / `Seller@123`

---

## 🎬 KỊCH BẢN DEMO CHI TIẾT

### Kịch Bản 1: Đấu Giá Cơ Bản & Đồng Bộ Real-time (Thời Gian Thực)
**Mục tiêu:** Chứng minh kiến trúc Socket I/O Broadcast giúp đồng bộ dữ liệu ngay lập tức mà không cần F5.

1. **Chuẩn bị:** Trên **Client 1** và **Client 2**, cùng nhấp vào xem chi tiết sản phẩm **"Laptop Dell XPS 15 OLED 2024"**.
2. **Hành động (Client 1):** 
   - Nhập `25,000,000` vào ô đặt giá (Bid Amount).
   - Nhấn **Place Bid**.
3. **Quan sát (Client 2 & Client 1):**
   - Không chạm vào Client 2. Ngay lập tức, bạn sẽ thấy mục **Current Bid** nhảy lên `25,000,000`.
   - **Highest Bidder** chuyển thành `demo@gmail.com`.
   - **LineChart** tự động vẽ thêm một điểm mới nối tiếp đồ thị.
   - Bảng **Bid History** cập nhật thêm một dòng.

### Kịch Bản 2: Đấu Giá Tự Động (Auto-Bid)
**Mục tiêu:** Chứng minh thuật toán tự động của Server có thể thay mặt người dùng giành lại vị thế đấu giá khi họ vắng mặt.

1. **Thiết lập Auto-Bid (Client 1):**
   - Vẫn ở trang sản phẩm Dell XPS 15 (giá đang là 25M).
   - Tại panel bên phải, nhập **Max Bid** là `30,000,000` và **Increment** là `500,000`.
   - Nhấn nút **Enable**. Giao diện báo `Auto-Bid Active` màu xanh.
2. **Kích hoạt tranh chấp (Client 2):**
   - Nhập `26,000,000` vào ô đặt giá.
   - Nhấn **Place Bid**.
3. **Quan sát kết quả (Cả 2 Client):**
   - Khi Client 2 vừa bấm đặt giá, giá thầu lập tức nhảy 2 bước liên tiếp.
   - Đầu tiên ghi nhận giá 26M của Client 2.
   - Sau đó chỉ mất 1/100 giây, Server tự động cộng thêm 500k cho Client 1, đẩy giá lên `26,500,000`.
   - Highest Bidder vẫn giữ nguyên là `demo@gmail.com`.
   - Bảng Bid History hiện ra 2 bản ghi sát nhau.

### Kịch Bản 3: Chống Bắn Tỉa (Anti-Sniping) & Hệ Thống Thông Báo
**Mục tiêu:** Tính công bằng trong những giây cuối cùng và độ mượt của Notification UI.

1. **Anti-Sniping:**
   - Dùng **Client 3 (Seller)** tạo nhanh 1 phiên đấu giá với thời gian chỉ **1 Phút** (Xem hướng dẫn phần Kịch Bản 5).
   - Quay lại Client 1 và Client 2 tham gia phiên đấu giá đó.
   - Đợi đồng hồ đếm ngược xuống còn dưới **30 giây**.
   - Tại **Client 2**, bấm **Place Bid** đè giá Client 1.
   - **Quan sát đồng hồ:** Đồng hồ lập tức được cộng thêm 60 giây (tùy config), đảm bảo Client 1 có cơ hội trả giá lại.
2. **Notification & UI/UX (Client 1):**
   - Ngay khi Client 2 đè giá, Client 1 sẽ nhận được một thẻ Toast màu vàng nhảy lên ở góc màn hình báo "Outbid Alert".
   - Biểu tượng **Quả Chuông** ở thanh menu xuất hiện Badge đỏ (số 1).
   - Click vào Quả chuông mở danh sách thông báo.
   - Rê chuột vào và bấm **"Đánh dấu đã đọc" (Mark as read)**. Chấm xanh chưa đọc biến mất, Badge trừ đi 1.
   - Click thử Menu chuột phải (Context Menu) vào thông báo, chọn **"Mark as unread"** để thấy tính tương tác mượt mà (Optimistic Update).
   - Đổi ngôn ngữ (En/Vi) trên thanh menu để xem UI thay đổi động.

### Kịch Bản 4: Bảng Điều Khiển Của Người Bán (Seller)
**Mục tiêu:** Luồng tạo sản phẩm và thông báo cho cộng đồng.

1. **Hành động (Client 3 - Seller):**
   - Vào mục **Seller Items**.
   - Thêm sản phẩm mới: Tên `iPad Pro M4`, Giá `15,000,000`, kèm Link ảnh. Nhấn **Add**.
   - Chọn sản phẩm vừa tạo trong danh sách, thiết lập thời gian `10` phút.
   - Nhấn **Create Session**.
2. **Quan sát (Client 1 & 2 - Bidder đang ở trang chủ):**
   - Ngay lập tức, 1 thẻ sản phẩm mới (Card) hiện ra ở trang chủ với nhãn **NEW** màu xanh lá mà không cần tải lại trang.

### Kịch Bản 5: Quản Trị Viên (Admin) & Xác Nhận Thanh Toán
**Mục tiêu:** Quản trị dữ liệu, Biểu đồ và Luồng tiền tệ.

1. **Đăng xuất Client 3 và Đăng nhập bằng Admin (`admin@gmail.com` / `Admin@123`):**
2. **Bảng Thống kê:**
   - Trình diễn biểu đồ **PieChart** đang phân bổ tỷ trọng sản phẩm.
   - Lướt xem toàn bộ **Bid Logs** (Nhật ký đấu giá) của toàn hệ thống ở Tab tương ứng.
3. **Thanh toán (Payment Lock):**
   - Chỉ ra logic ví: Người thắng cuộc hiện tại sẽ có tiền bị giam ở cột **Reserved** (Ví dụ Client 1 đang bị giam 26.5M).
   - Khi một phiên đấu giá kết thúc thành công (Trạng thái FINISHED), Admin vào tab quản lý, chọn phiên đó và nhấn **Confirm Payment**.
   - **Kết quả:** Tiền Reserved của Client 1 biến mất, và Balance của Seller (Client 3) tăng lên tương ứng. Trạng thái phiên chuyển thành PAID.

---