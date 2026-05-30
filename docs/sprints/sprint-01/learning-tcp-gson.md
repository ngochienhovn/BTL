# Learning - TCP/IP Socket & Gson/Jackson (Sprint 1)

## TCP Socket cho Client-Server

- Server mở `ServerSocket(port)` và `accept()` từng client.
- Mỗi client xử lý trong thread/pool riêng.
- Truyền dữ liệu theo protocol thống nhất trong repo hiện tại: **line-delimited JSON**.

## Vì sao dùng 1 JSON trên 1 dòng

- Socket là stream byte, không có khái niệm message boundary.
- Nếu quy ước mỗi JSON kết thúc bằng `\n`, ta có thể dùng `BufferedReader.readLine()`.

## Gson vs Jackson

- **Gson**: parse JSON nhanh, code gọn, phù hợp prototyping.
- **Jackson**: mạnh ở mapping/annotations, nhưng cấu hình có thể dài hơn.
- Trong Sprint 1 repo dùng `Gson` cho request/response.

