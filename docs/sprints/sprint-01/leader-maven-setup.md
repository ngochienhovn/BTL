# Leader (Sprint 1) - Maven setup

Repo sử dụng **Maven multi-module** phục vụ cho việc phát triển độc lập theo team nhiều người.

## Modules

- `shared/`: DTO/protocol client-server
- `server/`: Socket server + business logic (Sprint 2+)
- `client/`: JavaFX client

## Lệnh build

- Build toàn bộ:
  - `mvn package` (tại `Project/`)
- Build riêng module:
  - `mvn -pl shared package`
  - `mvn -pl server -am package`

