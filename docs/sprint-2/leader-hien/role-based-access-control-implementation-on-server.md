## Quy tắc phân quyền:
- SELLER:
  + tạo sản phẩm
  + chỉnh sửa sản phẩm
- ADMIN:
  + xem toàn bộ danh sách user
- BIDDER:
  + chỉ tham gia đấu giá
## Mô tả:
- Kiểm tra quyền được thực hiện tại server trước khi xử lý request.
- Không tin tưởng client → mọi kiểm soát nằm ở backend.
- Áp dụng trực tiếp trong các service xử lý nghiệp vụ.
## Mục tiêu đạt được:
- Đảm bảo an toàn hệ thống.
- Ngăn chặn truy cập trái phép.
- Tách biệt rõ authentication và authorization.