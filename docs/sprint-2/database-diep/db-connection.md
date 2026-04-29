# DB/QA - DBConnection

## Mục tiêu

`DBConnection` là class dùng để khởi tạo và quản lý kết nối tới cơ sở dữ liệu thông qua JDBC.

Class này giúp:
- Tập trung cấu hình kết nối DB vào một nơi
- Giúp các DAO tái sử dụng kết nối dễ dàng
- Hỗ trợ chuyển đổi giữa H2 và MySQL nếu cần

## Vai trò

`DBConnection` là lớp nền tảng cho tầng truy cập dữ liệu.

Các class như:
- `UserDAO`
- `ItemDAO`

sẽ sử dụng `DBConnection` để lấy `Connection` trước khi thực hiện truy vấn SQL.

## Cách triển khai

`DBConnection` cần thực hiện các bước:

- Nạp JDBC Driver
- Khai báo URL kết nối DB
- Khai báo username và password
- Trả về object `Connection`

## Hỗ trợ CSDL

Có thể cấu hình theo một trong hai loại DB:

### H2 Database

Phù hợp cho:
- môi trường dev/test
- chạy local nhanh
- dễ cấu hình

### MySQL

Phù hợp cho:
- môi trường production hoặc demo thực tế
- dữ liệu lớn hơn
- dễ tích hợp với hệ quản trị phổ biến

## Ví dụ nhiệm vụ chính

- Cung cấp hàm `getConnection()`
- Trả về kết nối JDBC cho các class DAO
- Đảm bảo cấu hình DB thống nhất trong toàn hệ thống

## Xử lý exception

`DBConnection` cần kiểm soát lỗi kết nối để tránh làm chương trình lỗi toàn bộ.

Các lỗi cần chú ý:
- Không nạp được JDBC Driver
- Sai URL kết nối
- Sai username/password
- DB chưa khởi động
- `SQLException`

Cách xử lý:
- Bắt exception khi tạo kết nối
- In/log nguyên nhân lỗi rõ ràng
- Trả thông báo phù hợp cho tầng trên
- Không để lỗi kết nối làm chương trình crash không kiểm soát

## Lưu ý

- Nên để cấu hình DB tập trung ở một file hoặc một class
- Nên dùng H2 cho dev/test nếu dự án còn ở giai đoạn đầu
- Nếu dùng MySQL, cần đảm bảo DB đã chạy trước khi kết nối
- Các DAO không nên tự viết lại logic kết nối DB

## Kết luận

`DBConnection` là class nền tảng giúp hệ thống kết nối CSDL qua JDBC một cách thống nhất, dễ quản lý và thuận tiện cho việc mở rộng sau này.