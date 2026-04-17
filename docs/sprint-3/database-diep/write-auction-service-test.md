# DB/QA Task - Write AuctionServiceTest

## Mục tiêu
Viết test cho `AuctionService` để kiểm tra logic bid, logic ví và race condition khi nhiều thread bid cùng lúc.

## File cần tạo
`server/src/test/java/com/ltnc/auction/server/services/AuctionServiceTest.java`

## Preconditions
- Đã có schema cho `auctions`, `bid_transactions`, `wallets`, `wallet_transactions`
- Đã có `AuctionService`
- DAO hoạt động được với DB test
- Có user/auction/wallet test data

## Test cases bắt buộc

### Case 1: bid cao hơn -> `OK`
#### Test steps
1. Tạo auction có `currentBid` ban đầu.
2. Tạo bidder có đủ tiền khả dụng.
3. Gọi hàm bid với giá cao hơn `currentBid`.
4. Kiểm tra response.
5. Query lại DB:
   - `auctions.current_bid`
   - `auctions.current_winner_id`
   - `wallets.reserved`

#### Expected result
- Kết quả là `OK`
- `currentBid` được cập nhật
- `currentWinnerId` đúng
- `reserved` tăng đúng

---

### Case 2: bid thấp hơn -> `BID_TOO_LOW`
#### Test steps
1. Tạo auction có `currentBid` hiện tại.
2. Gọi bid với giá thấp hơn hoặc bằng `currentBid`.
3. Kiểm tra response.
4. Query lại DB.

#### Expected result
- Kết quả là `BID_TOO_LOW`
- `currentBid` không đổi
- Không update sai dữ liệu ví

---

### Case 3: `bidAmount > available` -> `INSUFFICIENT_FUNDS`
#### Test steps
1. Tạo wallet với:
   - `balance` nhỏ
   - `reserved` đã có giá trị
2. Tính `available = balance - reserved`.
3. Gọi bid với `bidAmount > available`.
4. Kiểm tra response.

#### Expected result
- Kết quả là `INSUFFICIENT_FUNDS`
- `requiredTopUp` được tính đúng
- `currentBid` không bị update
- `reserved` không bị đổi sai

---

### Case 4: deposit rồi bid lại -> `OK`
#### Test steps
1. Sau case thiếu tiền, nạp thêm tiền vào ví.
2. Gọi bid lại với cùng bidAmount.
3. Kiểm tra response và dữ liệu DB.

#### Expected result
- Bid thành công
- `currentBid` được update đúng
- Ví được reserve đúng số tiền

---

### Case 5: concurrency - nhiều thread bid cùng lúc
#### Test steps
1. Tạo 1 auction dùng chung.
2. Tạo nhiều request bid đồng thời.
3. Dùng `ExecutorService` hoặc `CountDownLatch` để chạy nhiều thread cùng lúc.
4. Chờ tất cả thread hoàn thành.
5. Query lại DB:
   - `auctions.current_bid`
   - `wallets.reserved`
   - `bid_transactions`

#### Expected result
- Không lost update
- `currentBid` cuối cùng là giá lớn nhất hợp lệ
- `reserved` không âm
- Tổng reserved không sai logic
- Lịch sử bid được lưu đúng cho các bid hợp lệ