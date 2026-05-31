# DB/QA - UserDAO

## Mục tiêu

`UserDAO` là class thuộc tầng truy cập dữ liệu, dùng để thao tác với bảng `users` trong cơ sở dữ liệu.

Class này được triển khai theo **DAO Pattern (Data Access Object)** để:
- Tách phần truy vấn SQL ra khỏi tầng nghiệp vụ
- Hỗ trợ thao tác dữ liệu an toàn với JDBC
- Giúp mã nguồn dễ bảo trì và mở rộng

## Chức năng chính

`UserDAO` phụ trách các thao tác với bảng `users`, bao gồm:

- `findByEmail(...)`
- `insert(...)`

## Vai trò

`UserDAO` giúp tầng service làm việc với dữ liệu người dùng mà không cần viết SQL trực tiếp.

Ví dụ:
- Khi đăng ký tài khoản mới, service gọi `insert(...)`
- Khi kiểm tra email đã tồn tại chưa, service gọi `findByEmail(...)`

## Cách triển khai

`UserDAO` sử dụng JDBC để:
- Kết nối tới DB thông qua `DBConnection`
- Dùng `PreparedStatement` để truyền tham số
- Dùng `ResultSet` để đọc dữ liệu
- Chuyển dữ liệu từ DB thành object Java

## Các hàm chính

### `findByEmail(...)`

Dùng để:
- Tìm user theo email
- Kiểm tra email đã tồn tại trong hệ thống hay chưa

Ý nghĩa:
- Nếu tìm thấy dữ liệu, trả về object `User`
- Nếu không tìm thấy, trả về `null`

### `insert(...)`

Dùng để:
- Thêm một user mới vào bảng `users`

Ý nghĩa:
- Nhận object `User`
- Thực hiện câu lệnh `INSERT`
- Ghi dữ liệu vào DB

## Xử lý exception

`UserDAO` cần xử lý lỗi truy vấn để tránh làm Server bị crash.

Các lỗi cần kiểm soát:
- `SQLException`
- Lỗi kết nối CSDL
- Trùng `email`
- Sai cú pháp SQL
- Lỗi đọc dữ liệu

Cách xử lý:
- Bắt exception tại DAO
- Ghi log lỗi hoặc in thông báo rõ ràng
- Trả về kết quả phù hợp cho tầng service
- Không để exception chưa xử lý làm dừng chương trình

## Lưu ý

- Nên dùng `PreparedStatement` để tránh SQL Injection
- Nên kiểm tra email tồn tại trước khi insert
- DAO chỉ nên xử lý dữ liệu, không chứa business logic phức tạp

## Kết luận

`UserDAO` là lớp truy cập dữ liệu cho bảng `users`, hỗ trợ thao tác tìm kiếm và thêm mới người dùng một cách an toàn, rõ ràng và tách biệt với tầng nghiệp vụ.