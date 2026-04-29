

# BTL - DAO Pattern & H2 Database

## Giới thiệu
Project này triển khai tầng truy cập dữ liệu (**DAO Pattern**) cho hệ thống đấu giá, sử dụng **JDBC** để kết nối với **H2 Database**.

Nội dung chính của phần này gồm:
- Thiết kế kết nối CSDL bằng `DBConnection`
- Khởi tạo schema bằng `SchemaInitializer`
- Viết lớp `UserDAO` để thao tác với bảng `users`
- Viết lớp `ItemDAO` để thao tác với bảng `items`
- Thực hiện các thao tác CRUD cơ bản bằng câu lệnh SQL

---

## Công nghệ sử dụng
- Java
- JDBC
- H2 Database
- Maven

---

## Cấu trúc thư mục
```text
BTL
├── db
├── docs
├── server
│   ├── db
│   └── src
│       ├── dao
│       │   ├── ItemDAO.java
│       │   └── UserDAO.java
│       └── db
│           ├── DBConnection.java
│           └── SchemaInitializer.java
├── target
├── pom.xml
├── shared
└── README.md