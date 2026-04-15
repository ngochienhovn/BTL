# DB/QA - ItemDAO

## Mục tiêu

`ItemDAO` là class thuộc tầng truy cập dữ liệu, dùng để thao tác với bảng `items` trong cơ sở dữ liệu.

Class này được triển khai theo **DAO Pattern** để:
- Tách truy vấn SQL khỏi tầng nghiệp vụ
- Hỗ trợ CRUD cho item bằng JDBC
- Đảm bảo thao tác dữ liệu an toàn và dễ kiểm soát

## Chức năng chính

`ItemDAO` phụ trách các thao tác với bảng `items`, bao gồm:

- `insert(...)`
- `update(...)`
- `delete(...)`
- `findBySeller(...)`

## Vai trò

`ItemDAO` là lớp trung gian giữa service và DB đối với dữ liệu item.

Ví dụ:
- Khi người bán đăng item mới, service gọi `insert(...)`
- Khi sửa item, service gọi `update(...)`
- Khi xóa item, service gọi `delete(...)`
- Khi lấy danh sách item theo người bán, service gọi `findBySeller(...)`

## Cách triển khai

`ItemDAO` sử dụng JDBC để:
- Lấy kết nối từ `DBConnection`
- Dùng `PreparedStatement` để truyền tham số
- Dùng `ResultSet` để đọc dữ liệu
- Chuyển dữ liệu DB thành object Java

## Các hàm chính

### `insert(...)`

Dùng để:
- Thêm item mới vào bảng `items`

Ý nghĩa:
- Nhận object `Item`
- Thực hiện câu lệnh `INSERT`
- Ghi dữ liệu vào DB

### `update(...)`

Dùng để:
- Cập nhật thông tin item đã tồn tại

Ý nghĩa:
- Xác định item theo `id`
- Cập nhật dữ liệu mới trong bảng `items`

### `delete(...)`

Dùng để:
- Xóa item khỏi cơ sở dữ liệu

Ý nghĩa:
- Nhận `itemId`
- Thực hiện lệnh `DELETE`

### `findBySeller(...)`

Dùng để:
- Lấy danh sách item theo người bán

Ý nghĩa:
- Truy vấn các item có `seller_id` tương ứng
- Trả về danh sách item cho tầng service

## Xử lý exception

`ItemDAO` cần bắt lỗi để đảm bảo hệ thống vẫn hoạt động ổn định khi có lỗi DB.

Các lỗi cần kiểm soát:
- `SQLException`
- Lỗi kết nối DB
- Không tìm thấy item
- Lỗi update/delete dữ liệu
- Lỗi ràng buộc khóa ngoại

Cách xử lý:
- Bắt exception tại DAO
- Trả kết quả phù hợp cho service
- Có thể ghi log để hỗ trợ debug
- Không để lỗi lan ra ngoài làm crash Server

## Lưu ý

- Nên dùng `PreparedStatement`
- Nên kiểm tra item tồn tại trước khi update hoặc delete
- Nên tách từng hàm CRUD rõ ràng để dễ test
- DAO chỉ xử lý dữ liệu, không xử lý business logic phức tạp

## Kết luận

`ItemDAO` là lớp truy cập dữ liệu cho bảng `items`, hỗ trợ các thao tác thêm, sửa, xóa và truy vấn item theo người bán một cách an toàn và rõ ràng.