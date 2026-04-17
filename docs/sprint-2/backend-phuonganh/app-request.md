
## Mục tiêu

`AppRequest` là class dùng để biểu diễn gói tin yêu cầu được gửi từ Client lên Server thông qua Socket.

Class này giúp chuẩn hóa dữ liệu đầu vào để Server có thể:
- Xác định loại yêu cầu mà Client gửi lên
- Đọc các dữ liệu đi kèm của request
- Chuyển request đó đến đúng service để xử lý

## Vai trò

`AppRequest` là model trung gian trong quá trình giao tiếp giữa Client và Server.

Khi người dùng thao tác ở phía Client:
- Client tạo object `AppRequest`
- Chuyển object thành JSON
- Gửi JSON lên Server qua Socket

## Cấu trúc đề xuất

Các field thường có:

- `requestType`: loại yêu cầu, ví dụ `LOGIN`, `REGISTER`, `CREATE_ITEM`
- `username`: tên đăng nhập
- `password`: mật khẩu
- `itemId`: mã item
- `itemName`: tên item
- `price`: giá item

## Ví dụ class

```java
public class AppRequest {
    private String requestType;
    private String username;
    private String password;
    private Integer itemId;
    private String itemName;
    private Double price;
}