# BE (Sprint 3) - Create wallet model and transaction history.

## File cần tạo

- `~/Documents/GitHub/BTL/server/src/main/java/com/ltnc/auction/server/model/Wallet.java`
- `~/Documents/GitHub/BTL/server/src/main/java/com/ltnc/auction/server/model/WalletTransaction.java`

## Model Wallet

Model `Wallet` dùng để quản lý thông tin số dư hiện tại của người dùng.

Các trường có:

- `userId`
- `balance` (`BigDecimal`)
- `reserved` (`BigDecimal`)
- `updatedAt` (`LocalDateTime`)

Ngoài ra bổ sung hàm để lấy số dư khả dụng của ví. Giá trị này được tính từ số dư hiện có trừ đi phần đang bị giữ chỗ, và không được nhỏ hơn `0`.

- `public BigDecimal getAvailable()`

## Model WalletTransaction

Model `WalletTransaction` dùng để lưu lại lịch sử biến động số dư trong ví.

Các trường cần có:

- `id`
- `userId`
- `type` (có thể dùng `String` hoặc `enum`)
- `amount` (`BigDecimal`)
- `refAuctionId` (`Long`)
- `createdAt` (`LocalDateTime`)

## Ghi chú

- `Wallet` dùng để theo dõi trạng thái số dư hiện tại của mỗi người dùng.
- `WalletTransaction` dùng để lưu lịch sử cộng, trừ hoặc giữ tiền trong ví.
