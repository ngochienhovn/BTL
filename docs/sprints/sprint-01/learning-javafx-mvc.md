# Learning - JavaFX MVC (Sprint 1)

Trong JavaFX, MVC thường được tổ chức kiểu:

- **Model**: dữ liệu/DTO (ví dụ: `AuctionState`, message DTO trong `shared/`)
- **View**: UI mô tả bằng FXML (`main.fxml`, các view khác sau này)
- **Controller**: xử lý event UI, gọi network/service, và update model/view

## Lưu ý quan trọng về thread UI

- Network/Socket chạy ở thread khác.
- Khi update UI trong JavaFX, dùng `Platform.runLater(() -> { ... })`.
- Tránh chạm trực tiếp lên node UI từ thread socket.

