# DB/QA Task - Create BidDAO

## Mục tiêu
Tạo `BidDAO` để lưu và truy vấn lịch sử bid trong bảng `bid_transactions`.

## File cần tạo
`server/src/main/java/com/ltnc/auction/server/dao/BidDAO.java`

## Preconditions
- Đã có bảng `bid_transactions`
- Có model `BidTransaction`
- Có `DBConnection`

## Hàm cần có
- `insert(BidTransaction bid)`
- `findByAuction(Long auctionId)`

## Công việc cần làm

### 1. Tạo class `BidDAO`
Class này dùng để thao tác với dữ liệu lịch sử bid.

### 2. Implement `insert(BidTransaction bid)`
- Ghi một lần bid mới vào DB

### 3. Implement `findByAuction(Long auctionId)`
- Lấy danh sách bid theo `auction_id`

## Test steps
1. Tạo `BidDAO`.
2. Tạo object `BidTransaction` hợp lệ.
3. Gọi `bidDAO.insert(...)`.
4. Query DB kiểm tra dữ liệu vừa insert.
5. Insert thêm nhiều bid cho cùng 1 auction.
6. Gọi `findByAuction(auctionId)`.
7. Kiểm tra danh sách bid trả về.

## Expected result
- Insert bid thành công
- Dữ liệu lưu đúng vào bảng `bid_transactions`
- `findByAuction()` trả đúng các bid thuộc auction đó
- Không trả nhầm bid của auction khác