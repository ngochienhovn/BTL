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

---

## Công nghệ sử dụng

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