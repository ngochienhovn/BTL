# DB/QA (Sprint 1) - Cài đặt CSDL

Sprint 1 yêu cầu: Cài đặt CSDL, thiết kế schema các bảng thông tin.

## Gợi ý dùng H2 (dev/test)

Repo đã có dependency H2 ở module `server` để phục vụ lúc phát triển.

## Schema

- `db/schema/auction_schema.sql`
- hoặc migration `db/migrations/001_init_auction_tables.sql`

## Apply schema nhanh 

- Mở tool SQL client và chạy toàn bộ nội dung file `db/schema/auction_schema.sql`.
- Hoặc dùng H2 Console:
  - Kết nối tới DB H2
  - Run script bằng nội dung file schema

## Lưu ý

- Sprint 2/3 sẽ viết DAO/JDBC để tự động apply/migrate theo code.

