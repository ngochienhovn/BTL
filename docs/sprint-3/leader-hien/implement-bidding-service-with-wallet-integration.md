# BE (Sprint 3) - Implement bidding service with wallet integration

## File cần tạo

- `~/Documents/GitHub/BTL/server/src/main/java/com/ltnc/auction/server/services/AuctionService.java`

## Chức năng đặt giá

Trong `AuctionService`, bổ sung hàm xử lý đặt giá cho một phiên đấu giá:

- `public BidResult placeBid(Long auctionId, String bidderEmail, double amount)`

Luồng xử lý chính bao gồm:

- kiểm tra auction có tồn tại hay không
- khóa theo từng auction để tránh xung đột khi nhiều người bid cùng lúc
- kiểm tra trạng thái auction phải đang ở `RUNNING`
- kiểm tra giá bid mới phải lớn hơn `currentBid` và lớn hơn `0`
- tìm thông tin bidder từ `bidderEmail` để lấy `bidderId`
- lấy thông tin ví của bidder để kiểm tra khả năng đặt giá

## Kiểm tra ví khi bid

Trước khi cập nhật giá đấu, kiểm tra số dư khả dụng trong ví của người bid.

Nếu số tiền bid lớn hơn số dư khả dụng trong ví thì trả về kết quả không đủ tiền, đồng thời có thể kèm giá trị cần nạp thêm để tiếp tục tham gia đấu giá.

Các xử lý hỗ trợ:

- đọc thông tin `Wallet` của bidder
- so sánh `bidAmount` với `available`
- trả về trạng thái `INSUFFICIENT_FUNDS` nếu không đủ điều kiện

## Cập nhật auction và lịch sử bid

Nếu hợp lệ, service cần cập nhật lại thông tin auction và lưu lịch sử trả giá.

Các bước chính gồm:

- cập nhật `currentBid`
- cập nhật `highestBidderId`
- gọi DAO để lưu giá hiện tại mới
- thêm một bản ghi `BidTransaction`

## Xử lý reserve và release tiền

Khi có bidder mới dẫn đầu, hệ thống cập nhật lại phần tiền giữ chỗ trong ví.

Cách xử lý gồm:

- giải phóng phần tiền đã giữ của người đang dẫn đầu trước đó (nếu có)
- giữ lại số tiền tương ứng với mức bid mới của người đang dẫn đầu hiện tại
- thêm `WalletTransaction` cho các hành động `RELEASE` và `RESERVE`

## Kết quả trả về

Hàm `placeBid(...)` nên trả về `BidResult` với các mã kết quả phù hợp, ví dụ:

- `OK`
- `BID_TOO_LOW`
- `AUCTION_CLOSED`
- `INVALID_AMOUNT`
- `AUCTION_NOT_FOUND`
- `INSUFFICIENT_FUNDS`

## Chức năng bổ sung

Ngoài hàm đặt giá, service cũng hỗ trợ hàm lấy danh sách auction:

- `public java.util.List<Auction> getAllAuctions()`



## Ghi chú

- Cơ chế broadcast cập nhật auction chưa bắt buộc trong sprint này.
- Có thể để sẵn stub cho `broadcastAuctionUpdate(...)` và hoàn thiện ở Sprint 4.
