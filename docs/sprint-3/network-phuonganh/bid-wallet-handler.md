## 4. Xử lý Socket endpoint tại ClientHandler

### File

* `server/src/main/java/com/ltnc/auction/server/network/ClientHandler.java`

### Nội dung thực hiện

#### Thêm service vào handler

* Inject `AuctionService`
* Inject `WalletService`

#### Thêm xử lý switch case

* `GET_AUCTIONS`
* `PLACE_BID`
* `GET_WALLET`
* `DEPOSIT`
* `WITHDRAW`

#### Các hàm xử lý chính

* `handleGetAuctions()`

  * lấy danh sách phiên đấu giá từ DB
  * trả về client

* `handlePlaceBid()`

  * nhận bid từ client
  * kiểm tra hợp lệ
  * cập nhật giá cao nhất
  * trả kết quả thành công / thất bại

* `handleGetWallet()`

  * lấy thông tin ví hiện tại

* `handleDeposit()`

  * cộng tiền vào ví

* `handleWithdraw()`

  * trừ tiền nếu đủ số dư

## Luồng hoạt động

```text
Client gửi JSON request
        ↓
ClientHandler nhận request
        ↓
Phân loại MessageType
        ↓
Gọi AuctionService / WalletService
        ↓
Xử lý logic + DB
        ↓
Server trả JSON response
```