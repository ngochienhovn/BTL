
## Mục tiêu

Tài liệu này mô tả các endpoint Socket dùng để trao đổi dữ liệu giữa Client và Server.

Mỗi request được gửi dưới dạng JSON và phải có `requestType` để Server xác định cách xử lý.

## Danh sách request type

- `LOGIN`
- `REGISTER`
- `CREATE_ITEM`
- `GET_ALL_ITEMS`
- `UPDATE_ITEM`
- `DELETE_ITEM`

## LOGIN

### Mục đích

Dùng để đăng nhập tài khoản.

### Field cần có

- `requestType`
- `username`
- `password`

### Request

```json
{
  "requestType": "LOGIN",
  "username": "admin",
  "password": "123456"
}