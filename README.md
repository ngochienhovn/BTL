# BTL - Auction Socket Project

## Modules
- `shared`: JSON protocol (request/response message)
- `server`: socket server, auth, role-based authorization, DAO/JDBC
- `client`: JavaFX UI screens

## Run server
```bash
mvn -pl server -am exec:java
```

## Build all
```bash
mvn clean package
```

## Key deliverables
- Role model: `ADMIN`, `SELLER`, `BIDDER` in `UserRole`
- Auction state enum: `OPEN`, `RUNNING`, `FINISHED`
- Auth authorization checks in `AuthService`
- DAO + JDBC CRUD in `UserDAO`, `ItemDAO`, `DBConnection`
- SQL init script: `server/src/main/resources/db/001_init_auction_tables.sql`
- Socket endpoint docs: `docs/socket-endpoints.md`
- UML docs: `docs/uml-class-diagram.md`
