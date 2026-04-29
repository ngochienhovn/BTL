<<<<<<< HEAD


# BTL - DAO Pattern & H2 Database

## Giới thiệu
Project này triển khai tầng truy cập dữ liệu (**DAO Pattern**) cho hệ thống đấu giá, sử dụng **JDBC** để kết nối với **H2 Database**.

Nội dung chính của phần này gồm:
- Thiết kế kết nối CSDL bằng `DBConnection`
- Khởi tạo schema bằng `SchemaInitializer`
- Viết lớp `UserDAO` để thao tác với bảng `users`
- Viết lớp `ItemDAO` để thao tác với bảng `items`
- Thực hiện các thao tác CRUD cơ bản bằng câu lệnh SQL
=======
# Auction Server - Java Socket

Một server đấu giá đơn giản được xây dựng bằng **Java Socket** và **Gson**, sử dụng mô hình giao tiếp **Client - Server** thông qua JSON message.

## Giới thiệu

Project này là phần backend server cho hệ thống đấu giá.  
Server nhận kết nối từ nhiều client, đọc dữ liệu JSON gửi lên, xử lý theo loại message và trả về phản hồi tương ứng.

Hiện tại project đã có các chức năng nền tảng như:

- Khởi động socket server
- Nhận nhiều kết nối client đồng thời bằng `Thread`
- Giao tiếp giữa client và server bằng JSON
- Xử lý một số loại message cơ bản:
  - `LOGIN`
  - `REGISTER`
  - `AUCTION_BID`
  - `GET_ITEMS`
  - `CREATE_AUCTION`
>>>>>>> a7b09fa9049b4b79b22aea4f1f7fefb77ed2db5c

---

## Công nghệ sử dụng
<<<<<<< HEAD
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
=======

- **Java**
- **Java Socket**
- **Maven**
- **Gson**

---

## Cấu trúc project

```bash
server/
├── pom.xml
├── src/main/java/com/ltnc/auction/server/
│   ├── MainServer.java
│   ├── protocol/
│   │   ├── ClientToServerMessage.java
│   │   ├── MessageType.java
│   │   └── ServerToClientMessage.java
│   └── server/
│       ├── ClientHandler.java
│       └── SockerServer.java
>>>>>>> a7b09fa9049b4b79b22aea4f1f7fefb77ed2db5c
