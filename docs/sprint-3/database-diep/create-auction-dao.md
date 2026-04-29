# DB/QA Task - Create AuctionDAO

## Mục tiêu
Tạo `AuctionDAO` để thao tác với bảng `auctions` bằng JDBC.

## File cần tạo
`server/src/main/java/com/ltnc/auction/server/dao/AuctionDAO.java`

## Preconditions
- Đã có bảng `auctions`
- Có `DBConnection`
- Có model `Auction` hoặc object tương ứng

## Hàm cần có
- `findAll()`
- `findById(Long id)`
- `updateCurrentBid(Long auctionId, BigDecimal amount, Long winnerId)`

## Công việc cần làm

### 1. Tạo class `AuctionDAO`
Class này chịu trách nhiệm truy cập dữ liệu bảng `auctions`.

### 2. Implement `findAll()`
- Query tất cả auction
- Trả về danh sách object `Auction`

### 3. Implement `findById(Long id)`
- Query theo `id`
- Trả về 1 object `Auction`

### 4. Implement `updateCurrentBid(...)`
- Update `current_bid`
- Update `current_winner_id`
- Dùng cho flow bid thành công

## Test steps
1. Tạo `AuctionDAO`.
2. Insert dữ liệu auction mẫu vào DB.
3. Gọi `findAll()` và kiểm tra danh sách trả về.
4. Gọi `findById(id)` và kiểm tra dữ liệu đúng.
5. Gọi `updateCurrentBid(...)`.
6. Query lại DB để kiểm tra:
   - `current_bid`
   - `current_winner_id`

## Expected result
- `findAll()` trả đúng danh sách auction
- `findById()` trả đúng auction theo id
- `updateCurrentBid()` update đúng dữ liệu
- Không lỗi SQL/JDBC