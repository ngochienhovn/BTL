# DB/QA Task - Upgrade auction, bid and wallet schema

## Mục tiêu
Nâng cấp schema DB để hỗ trợ đầy đủ flow đấu giá + ví, đủ dùng cho end-to-end test.

## File cần sửa
`server/src/main/resources/db/001_init_auction_tables.sql`

## Preconditions
- Đã có project backend chạy được với DB H2 hoặc DB test
- File `001_init_auction_tables.sql` đang được dùng để init schema
- Có bảng `users` để liên kết khóa ngoại

## Công việc cần làm

### 1. Thêm bảng `auctions`
Tạo bảng `auctions` để lưu thông tin phiên đấu giá, gồm các trường cơ bản:
- `id`
- `item_id`
- `seller_id`
- `start_price`
- `current_bid`
- `current_winner_id`
- `start_time`
- `end_time`
- `status`

### 2. Thêm bảng `bid_transactions`
Tạo bảng `bid_transactions` để lưu lịch sử bid:
- `id`
- `auction_id`
- `bidder_id`
- `amount`
- `created_at`

### 3. Thêm bảng `wallets`
Tạo bảng `wallets` để quản lý số dư ví:
- `user_id` PK/FK
- `balance DECIMAL`
- `reserved DECIMAL`
- `updated_at TIMESTAMP`

### 4. Thêm bảng `wallet_transactions`
Tạo bảng `wallet_transactions` để lưu lịch sử giao dịch ví:
- `id`
- `user_id`
- `type`
- `amount`
- `ref_auction_id`
- `created_at`

### 5. Kiểm tra ràng buộc
- PK đúng
- FK đúng
- Kiểu `DECIMAL` dùng cho tiền
- Có thể insert/query bình thường

## Test steps
1. Sửa file `001_init_auction_tables.sql`.
2. Chạy init schema trên DB test.
3. Query kiểm tra đã tạo đủ 4 bảng mới.
4. Kiểm tra từng cột quan trọng trong bảng.
5. Thử insert dữ liệu mẫu vào từng bảng.

## Expected result
- DB tạo thành công các bảng:
  - `auctions`
  - `bid_transactions`
  - `wallets`
  - `wallet_transactions`
- Không lỗi SQL syntax
- PK/FK hoạt động đúng
- Dùng được cho flow bid + wallet