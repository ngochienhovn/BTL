# Learning - OOP & Singleton (Sprint 1)

## 4 nguyên tắc OOP (cốt lõi)

1. **Encapsulation (Đóng gói)**: che giấu dữ liệu/logic bên trong bằng `private` và cung cấp public methods.
2. **Inheritance (Kế thừa)**: tái sử dụng code/behavior qua lớp cha (khi thật sự hợp lý).
3. **Polymorphism (Đa hình)**: cùng 1 interface/abstract nhưng hành vi khác nhau theo implement.
4. **Abstraction (Trừu tượng hóa)**: tập trung vào “điều gì” thay vì “làm chi tiết ra sao” (interface/abstract class).

## Singleton Pattern (khi nào dùng)

- Dùng khi có **1 instance duy nhất** cho toàn app và cần truy cập toàn cục (ví dụ: connection manager, config manager).

## Singleton an toàn luồng (gợi ý)

- Ưu tiên cách `enum Singleton` thread-safe mặc định hoặc double-checked locking với `volatile`.
- Tránh singleton bừa vì khi đó code sẽ rất khó kiểm thử.

- Cân nhắc singleton cho một `ServerConfig` hoặc `AuctionRegistry,` nhưng vẫn nên thiết kế để test được.

