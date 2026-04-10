
## Mục tiêu

`AppResponse` là class dùng để biểu diễn gói tin phản hồi từ Server trả về Client sau khi request đã được xử lý.

Class này giúp Client biết:
- Request có thành công hay không
- Server trả về thông báo gì
- Có dữ liệu nào đi kèm hay không

## Vai trò

`AppResponse` là model đầu ra trong quá trình giao tiếp bằng Socket giữa Client và Server.

Sau khi Server xử lý xong:
- Tạo object `AppResponse`
- Chuyển object thành JSON
- Gửi JSON về Client

## Cấu trúc đề xuất

Các field thường có:

- `success`: trạng thái xử lý thành công hay thất bại
- `message`: thông báo trả về
- `data`: dữ liệu bổ sung nếu có

## Ví dụ class

```java
public class AppResponse {
    private boolean success;
    private String message;
    private Object data;
}