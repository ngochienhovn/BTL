
## Mục tiêu

`ClientHandler` là class xử lý giao tiếp giữa Server và từng Client thông qua Socket.

Nhiệm vụ chính:
- Nhận request từ Client
- Chuyển JSON thành object Java
- Xác định loại request
- Gọi đúng service để xử lý
- Trả response về cho Client

## Vai trò

`ClientHandler` đóng vai trò như một bộ điều hướng ở phía Server.

Khi nhận được request:
- Nếu là `LOGIN` thì gọi `AuthService`
- Nếu là `REGISTER` thì gọi `AuthService`
- Nếu là `CREATE_ITEM` thì gọi `ItemService`
- Nếu là `UPDATE_ITEM` thì gọi `ItemService`
- Nếu là `DELETE_ITEM` thì gọi `ItemService`

Class này giúp tách riêng:
- tầng giao tiếp mạng
- tầng xử lý nghiệp vụ

## Các request chính cần xử lý

- `LOGIN`
- `REGISTER`
- `CREATE_ITEM`
- `GET_ALL_ITEMS`
- `UPDATE_ITEM`
- `DELETE_ITEM`

## Ví dụ logic điều hướng

```java
switch (request.getRequestType()) {
    case "LOGIN":
        response = authService.login(request);
        break;
    case "REGISTER":
        response = authService.register(request);
        break;
    case "CREATE_ITEM":
        response = itemService.createItem(request);
        break;
    case "GET_ALL_ITEMS":
        response = itemService.getAllItems();
        break;
    case "UPDATE_ITEM":
        response = itemService.updateItem(request);
        break;
    case "DELETE_ITEM":
        response = itemService.deleteItem(request.getItemId());
        break;
    default:
        response = new AppResponse(false, "Loại request không hợp lệ", null);
}