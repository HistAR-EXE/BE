# BE/docs — Database seed & SQL

Cấu trúc gom theo vai trò (một manifest thống nhất thứ tự chạy).

```
BE/docs/
├── README.md                 ← bạn đang đọc
├── seed-manifest.txt         ← thứ tự seed (single source of truth)
├── seed-manifest-post-flyway.txt ← seed sau Flyway (payment tables, …)
├── sql/
│   ├── schema/               ← Postgres init (Docker volume mới)
│   ├── seed/                 ← migration + data idempotent
│   └── manual/               ← chạy tay (Flyway baseline, QA check)
├── reference/                ← ghi chú kỹ thuật (.md)
└── scripts/
    ├── Invoke-HistarSeed.ps1
    ├── run-all-seed.ps1      ← Docker local
    └── run-all-seed-render.ps1
```

## Chạy seed

| Môi trường | Lệnh |
|------------|------|
| **Docker local** | `cd BE` → `.\docs\scripts\run-all-seed.ps1` |
| **DB trống + schema** | `.\docs\scripts\run-all-seed.ps1 -IncludeSchema` |
| **Render / external** | `cd BE` → `.\docs\scripts\run-all-seed-render.ps1 -ConfirmProduction` (cần `.env.render-db`; tự migrate Flyway rồi chạy seed) |
| **Compose tự seed** | `docker compose up -d` → service `postgres-seed` đọc `seed-manifest.txt` |

Flyway schema mới: `BE/src/main/resources/db/migration/`. Script Render sẽ chạy các migration còn thiếu trước khi seed; không dùng bootstrap local hoặc `-Reset` cho Render.

## Bootstrap đầy đủ cho database local rỗng

Khi một thành viên cần **toàn bộ schema, dữ liệu seed và migration đến phiên bản hiện tại**,
không chạy riêng `flyway:migrate` trên database rỗng. Các migration lịch sử V1–V13 giả định
schema cũ đã tồn tại.

Từ thư mục `BE`, chạy một lệnh sau với Docker Compose đang hoạt động:

```powershell
.\docs\scripts\bootstrap-local-db.ps1 -Database timelens_full -Reset
```

Lệnh này chỉ xóa database đã nêu sau khi có `-Reset`, sau đó nạp
`docs/sql/bootstrap/TimeLens_full_bootstrap.sql`, seed data, và ghi Flyway baseline ở migration
mới nhất. Không dùng lệnh này cho staging, Render hoặc production.

Khi thêm migration/seed mới, tái tạo file bootstrap trước khi commit:

```powershell
.\docs\scripts\New-HistarFullBootstrapSql.ps1
```

## Thêm SQL mới

1. Đặt file vào `sql/seed/` (hoặc `sql/manual/` nếu không nằm pipeline).
2. Thêm **một dòng** tên file vào `seed-manifest.txt` đúng thứ tự phụ thuộc.
3. Chạy lại seed trên DB dev; cập nhật Flyway nếu đổi schema production.

## Tài liệu liên quan

- Java package map: `BE/docs/PACKAGE_STRUCTURE.md` (nếu team bật doc refactor BE)
- [`../../docs/05_development_&_deployment.md`](../../docs/05_development_&_deployment.md) — deploy monorepo
