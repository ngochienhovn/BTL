## Ví dụ request test

### Lấy danh sách đấu giá

```json
{
  "type": "GET_AUCTIONS"
}
```

### Đặt giá

```json
{
  "type": "PLACE_BID",
  "auctionId": 1,
  "bidAmount": 500
}
```

### Xem ví

```json
{
  "type": "GET_WALLET"
}
```

## Kết quả đạt được

* Hoàn thiện giao thức Socket cho đấu giá.
* Client gửi / nhận JSON đúng format.
* Hỗ trợ bid cơ bản.
* Hỗ trợ quản lý ví tiền.

