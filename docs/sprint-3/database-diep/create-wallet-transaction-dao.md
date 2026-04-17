# DB/QA Task - Create WalletTransactionDAO

## Mục tiêu
Tạo `WalletTransactionDAO` để lưu lịch sử giao dịch ví.

## File cần tạo
`server/src/main/java/com/ltnc/auction/server/dao/WalletTransactionDAO.java`

## Preconditions
- Đã có bảng `wallet_transactions`
- Có model `WalletTransaction`
- Có `DBConnection`

## Hàm cần có
- `insert(WalletTransaction tx)`

## Công việc cần làm

### 1. Tạo class `WalletTransactionDAO`
Class này dùng để ghi lịch sử thay đổi số dư ví.

### 2. Implement `insert(WalletTransaction tx)`
- Ghi transaction vào bảng `wallet_transactions`

## Test steps
1. Tạo `WalletTransactionDAO`.
2. Tạo object `WalletTransaction` với dữ liệu hợp lệ:
   - `userId`
   - `type`
   - `amount`
   - `refAuctionId` nếu có
3. Gọi `walletTransactionDAO.insert(tx)`.
4. Query DB kiểm tra dữ liệu vừa insert.

## Expected result
- Insert transaction thành công
- Dữ liệu lưu đúng vào bảng `wallet_transactions`
- Có thể dùng để audit giao dịch ví
- Không lỗi khi `ref_auction_id` null hoặc có giá trị