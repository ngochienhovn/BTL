# Network (Sprint 3) - Socket protocol + Bid/Wallet endpoints

Sprint 3 yêu cầu: mở rộng giao tiếp Client - Server để hỗ trợ xem phiên đấu giá, đặt giá và quản lý ví tiền.

## Mục tiêu chính

* Client xem danh sách phiên đấu giá đang mở.
* Client gửi lệnh đặt giá (bid) theo thời gian thực.
* Server kiểm tra bid hợp lệ và phản hồi kết quả.
* Client xem số dư ví.
* Hỗ trợ nạp / rút tiền để phục vụ đấu giá.

## Công việc đã thực hiện

### 1. Cập nhật Shared Protocol

Repo dùng module `shared` để định nghĩa cấu trúc dữ liệu giữa Client và Server.

#### File cập nhật

* `shared/src/main/java/com/ltnc/auction/shared/protocol/MessageType.java`
* `shared/src/main/java/com/ltnc/auction/shared/protocol/ClientToServerMessage.java`
* `shared/src/main/java/com/ltnc/auction/shared/protocol/ServerToClientMessage.java`

#### MessageType bổ sung

**Client -> Server**

* `GET_AUCTIONS`: lấy danh sách đấu giá
* `PLACE_BID`: đặt giá
* `GET_WALLET`: xem ví
* `DEPOSIT`: nạp tiền
* `WITHDRAW`: rút tiền

**Server -> Client**

* `AUCTION_LIST`: trả danh sách đấu giá
* `BID_RESULT`: kết quả đặt giá
* `WALLET_RESULT`: thông tin ví
* `DEPOSIT_RESULT`: kết quả nạp tiền
* `WITHDRAW_RESULT`: kết quả rút tiền

## 2. Mở rộng dữ liệu request từ Client

### File

* `shared/.../ClientToServerMessage.java`

### Field bổ sung

* `auctionId`: ID phiên đấu giá
* `bidAmount`: số tiền đặt giá
* `amount`: số tiền nạp / rút

### Ý nghĩa

* Client gửi đúng phiên đấu giá cần thao tác.
* Server biết số tiền người dùng nhập.

## 3. Mở rộng dữ liệu response từ Server

### File

* `shared/.../ServerToClientMessage.java`

### Field bổ sung

* `auctions`: danh sách phiên đấu giá
* `auction`: thông tin chi tiết 1 phiên
* `balance`: tổng số dư ví
* `reserved`: tiền đang giữ khi bid
* `available`: tiền còn khả dụng
* `requiredTopUp`: số tiền cần nạp thêm nếu bid thiếu tiền
