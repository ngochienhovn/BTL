# DB/QA - 001_init_auction_tables.sql

## Mục tiêu

File `001_init_auction_tables.sql` dùng để khởi tạo schema cơ sở dữ liệu ban đầu cho hệ thống.

Script này có nhiệm vụ:
- Tạo các bảng chính của hệ thống
- Khai báo kiểu dữ liệu phù hợp
- Thiết lập khóa chính, khóa ngoại và các ràng buộc cần thiết

## Các bảng chính

Schema cần hoàn thiện với các bảng:

- `users`
- `items`
- `auctions`

## Vai trò

File SQL này là nền tảng để hệ thống có thể:
- Lưu thông tin người dùng
- Lưu thông tin item
- Lưu thông tin phiên đấu giá
- Đảm bảo liên kết dữ liệu giữa các bảng

## Yêu cầu thiết kế

### Bảng `users`

Dùng để lưu thông tin tài khoản người dùng.

Các cột thường có:
- `id`
- `username`
- `email`
- `password`
- `created_at`

Ràng buộc nên có:
- `PRIMARY KEY`
- `UNIQUE(username)`
- `UNIQUE(email)`

### Bảng `items`

Dùng để lưu thông tin item do người dùng đăng bán.

Các cột thường có:
- `id`
- `name`
- `description`
- `starting_price`
- `seller_id`
- `created_at`

Ràng buộc nên có:
- `PRIMARY KEY`
- `FOREIGN KEY (seller_id)` tham chiếu tới bảng `users(id)`

### Bảng `auctions`

Dùng để lưu thông tin phiên đấu giá.

Các cột thường có:
- `id`
- `item_id`
- `start_time`
- `end_time`
- `status`

Ràng buộc nên có:
- `PRIMARY KEY`
- `FOREIGN KEY (item_id)` tham chiếu tới bảng `items(id)`

## Lưu ý

- Kiểu dữ liệu phải phù hợp với từng cột
- Cần khai báo khóa ngoại rõ ràng để đảm bảo tính liên kết
- Nên dùng `IF NOT EXISTS` khi tạo bảng để tránh lỗi khi chạy lại script
- Schema phải đồng bộ với model Java và tầng DAO

## Kết luận

`001_init_auction_tables.sql` là file khởi tạo schema quan trọng, giúp hệ thống có cấu trúc dữ liệu chuẩn ngay từ đầu và sẵn sàng cho các thao tác CRUD ở tầng DAO.