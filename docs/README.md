# BE/docs — Database seed & SQL

Cấu trúc gom theo vai trò (một manifest thống nhất thứ tự chạy).

```
BE/docs/
├── README.md                 ← bạn đang đọc
├── seed-manifest.txt         ← thứ tự seed (single source of truth)
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
| **Render / external** | `cd BE` → `.\docs\scripts\run-all-seed-render.ps1` (cần `.env.render-db`) |
| **Compose tự seed** | `docker compose up -d` → service `postgres-seed` đọc `seed-manifest.txt` |

Flyway schema mới: `BE/src/main/resources/db/migration/` (chạy khi BE boot).

## Thêm SQL mới

1. Đặt file vào `sql/seed/` (hoặc `sql/manual/` nếu không nằm pipeline).
2. Thêm **một dòng** tên file vào `seed-manifest.txt` đúng thứ tự phụ thuộc.
3. Chạy lại seed trên DB dev; cập nhật Flyway nếu đổi schema production.

## Tài liệu liên quan

- Java package map: `BE/docs/PACKAGE_STRUCTURE.md` (nếu team bật doc refactor BE)
- [`../../docs/05_development_&_deployment.md`](../../docs/05_development_&_deployment.md) — deploy monorepo
