# DB Schema Sprint 1

Sprint 1 yêu cầu: Cài đặt CSDL, thiết kế schema các bảng thông tin.

## Files schema

- `db/schema/auction_schema.sql` (bản schema đầy đủ)
- `db/migrations/001_init_auction_tables.sql` (migration 001)

## Bảng chính

- `users` (Admin/Seller/Bidder)
- `items` (sản phẩm/đấu giá được bán bởi `seller_id`)
- `auctions` (phiên đấu giá cho 1 `item_id`, có `state`, `current_price`, `highest_bidder_id`)
- `bids` (lịch sử bid theo `auction_id`, `bid_amount`)

## Dùng H2 

Repo đang có dependency H2 trong module `server`. Sprint 2/3 sẽ viết DAO/JDBC để:

- tạo connection tới H2
- apply `db/schema/auction_schema.sql`

Nếu cần apply thủ công bằng tool SQL của H2, hãy chạy nội dung file `db/schema/auction_schema.sql`.