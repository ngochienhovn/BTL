# DB/QA Task - Create WalletDAO

## Mục tiêu
Tạo `WalletDAO` để quản lý ví người dùng, gồm số dư hiện có và số tiền đang reserved.

## File cần tạo
`server/src/main/java/com/ltnc/auction/server/dao/WalletDAO.java`

## Preconditions
- Đã có bảng `wallets`
- Có bảng `users`
- Có `DBConnection`
- Có model `Wallet`

## Hàm cần có
- `findOrCreateByUserId(Long userId)`
- `updateBalances(Long userId, BigDecimal balance, BigDecimal reserved)`

## Công việc cần làm

### 1. Tạo class `WalletDAO`
Class này dùng để thao tác dữ liệu ví.

### 2. Implement `findOrCreateByUserId(Long userId)`
- Nếu đã có ví thì lấy ra
- Nếu chưa có ví thì tạo ví mới với:
  - `balance = 0`
  - `reserved = 0`

### 3. Implement `updateBalances(...)`
- Update `balance`
- Update `reserved`

## Test steps
1. Tạo `WalletDAO`.
2. Chọn 1 user chưa có ví.
3. Gọi `findOrCreateByUserId(userId)`.
4. Query DB kiểm tra ví đã được tạo.
5. Gọi lại `findOrCreateByUserId(userId)` lần nữa.
6. Kiểm tra không tạo trùng ví.
7. Gọi `updateBalances(...)`.
8. Query lại DB để kiểm tra giá trị mới.

## Expected result
- User chưa có ví sẽ được tạo ví mới
- User đã có ví thì không bị tạo trùng
- `balance` và `reserved` update đúng
- Không có lỗi SQL/JDBC