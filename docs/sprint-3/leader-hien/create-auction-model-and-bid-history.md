# BE (Sprint 3) - Tạo model đấu giá và lịch sử trả giá


## File cần tạo

- `~/Documents/GitHub/BTL/server/src/main/java/com/ltnc/auction/server/model/Auction.java`
- `~/Documents/GitHub/BTL/server/src/main/java/com/ltnc/auction/server/model/BidTransaction.java`

## Model Auction

Model `Auction` dùng để biểu diễn một phiên đấu giá.

Các thông tin:

- `id`
- `itemId`
- `title`
- `description`
- `startingBid`
- `currentBid`
- `status` (có thể dùng `String` hoặc `AuctionStatus`)
- `highestBidderId`
- `endTime`
- `List<BidTransaction>`
- `ReentrantLock bidLock`

Bổ sung hàm lấy lock để hỗ trợ đồng bộ khi xử lý bid:

- `public java.util.concurrent.locks.ReentrantLock getBidLock()`

## Model BidTransaction

Model `BidTransaction` dùng để lưu thông tin mỗi lần người dùng trả giá.

Các trường:

- `id`
- `auctionId`
- `bidderId`
- `bidderEmail`
- `bidderName`
- `amount` (`java.math.BigDecimal`)
- `createdAt` (`java.time.LocalDateTime`)

## Ghi chú

- `Auction` là model chính cho phiên đấu giá.
- `BidTransaction` dùng để lưu lịch sử bid của từng phiên.
