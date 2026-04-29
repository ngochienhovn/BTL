# DB/QA Task - Sync SchemaInitializer for auction, bid and wallet

## Mục tiêu
Đồng bộ `SchemaInitializer` với schema mới để khi server khởi động có thể tạo đủ bảng phục vụ module đấu giá và ví.

## File cần sửa
`server/src/main/java/com/ltnc/auction/server/config/SchemaInitializer.java`

## Preconditions
- Đã có schema mới trong `001_init_auction_tables.sql`
- Server đang dùng `SchemaInitializer` để dựng DB
- Có kết nối DB test hợp lệ

## Công việc cần làm

### Cách 1
Cho `SchemaInitializer` chạy file SQL init.

### Cách 2
Hoặc bổ sung trực tiếp lệnh `CREATE TABLE` trong `SchemaInitializer` cho các bảng:
- `auctions`
- `bid_transactions`
- `wallets`
- `wallet_transactions`

## Test steps
1. Mở `SchemaInitializer`.
2. Đồng bộ logic init schema theo schema mới.
3. Chạy server hoặc gọi `SchemaInitializer.initialize()`.
4. Query DB kiểm tra đủ các bảng mới.
5. Chạy lại thêm 1 lần để kiểm tra không lỗi tạo bảng trùng.

## Expected result
- `SchemaInitializer` tạo đủ schema mới
- Không thiếu bảng/cột cho module bid + wallet
- Không lỗi khi chạy lại
- Đồng bộ với `001_init_auction_tables.sql`