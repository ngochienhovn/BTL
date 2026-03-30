# Sprint 1 (24/03 – 30/03): Khởi tạo & Sơ đồ cấu trúc

## Trạng thái (đã làm)

- Leader
  - UML Class Diagram: `docs/uml/class-diagram.puml`
  - Setup build Maven (multi-module): `Project/pom.xml`, `shared/`, `server/`, `client/`
- Frontend
  - FXML wireframe tĩnh + controller tối thiểu: `client/src/main/resources/.../main.fxml`
- Network
  - ServerSocket mở port, nhận line-delimited JSON và phản hồi `PING -> PONG`:
    - `server/src/main/java/com/ltnc/auction/server/network/SocketServer.java`
  - Test plan: `docs/test-plan/sprint-01-ping-pong.md`
- DB/QA
  - Schema SQL cho H2: `db/schema/auction_schema.sql`
  - Migration 001: `db/migrations/001_init_auction_tables.sql`

## Chạy thử nhanh

1. Chạy server: chạy `ServerMain` (mặc định port `5555`)
2. Chạy client JavaFX: `ClientMain`, bấm nút `Ping server`
3. Hoặc test CLI:
  - xem `docs/test-plan/sprint-01-ping-pong.md`

## GitHub 

Sprint 1 có yêu cầu khởi tạo GitHub. Lúc triển khai thực tế, nhóm đã làm sẽ:

- tạo repo
- push local code
- đặt branch theo ý đồ nhóm

