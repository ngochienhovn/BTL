# QA Test Case - `ItemDAO.insert()`

## Mục tiêu
Xác minh hàm `itemDAO.insert()` ghi dữ liệu thật vào DB H2.

## Preconditions
- Đã chạy `SchemaInitializer.initialize()`
- Có user seller trong bảng `users`

## Test steps
1. Tạo `ItemDAO`.
2. Tạo object `Item` với:
   - `sellerId` = ID seller hợp lệ
   - `sellerEmail` = email seller
   - `type` = `Electronics`
   - `name` = `Laptop Test`
   - `description` = `QA insert check`
   - `startingBid` = `1000.00`
3. Gọi `Long id = itemDAO.insert(item)`.
4. Query kiểm tra:
   - `SELECT id, name, seller_email FROM items WHERE id = ?`
5. So sánh kết quả:
   - Có đúng 1 bản ghi
   - `name = Laptop Test`
   - `seller_email` đúng email seller

## Expected result
- `id` trả về khác `null`
- Query step 4 trả về bản ghi tương ứng
- Dữ liệu trong DB khớp với input
