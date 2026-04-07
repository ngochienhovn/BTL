 ## User Roles:

- ADMIN
- SELLER
- BIDDER
## Mô tả:

- Sử dụng Enum để định nghĩa role thống nhất trong hệ thống.
- Role được đặt tại module server để dùng chung cho toàn bộ backend.
- Tích hợp vào luồng xử lý đăng nhập và xác thực người dùng.
## Mục tiêu đạt được:

- Chuẩn hóa vai trò người dùng.
- Tránh hardcode role dạng string.
- Tạo nền tảng cho authorization ở các chức năng sau.