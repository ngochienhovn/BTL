# Test plan Sprint 1 - Ping/Pong

Mục tiêu: xác nhận server mở port, nhận JSON line-delimited và phản hồi đúng `PONG`.

## Chuẩn bị
- Chạy server: `ServerMain` (mặc định port `5555`)
- Dùng client CLI (khuyến nghị) hoặc netcat

## Cách 1: Dùng CLI trong repo (khuyến nghị)
Chạy class `com.ltnc.auction.server.cli.PingPongClient` với tham số:
`<host> <port>`

Ví dụ:
- `localhost 5555`

## Cách 2: Dùng netcat
Trên macOS:
```bash
printf '{"type":"PING"}\n' | nc 127.0.0.1 5555
```

Kỳ vọng:
- Nhận 1 dòng JSON có `{"type":"PONG" ...}`

## Negative test
- Gửi JSON thiếu `type`:
```bash
printf '{"foo":"bar"}\n' | nc 127.0.0.1 5555
```
- Kỳ vọng: response `ERROR`

