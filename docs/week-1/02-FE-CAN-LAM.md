# CP3 Tuần 1 — FE cần làm để tích hợp BE

> Checklist bám **contract BE thực tế**. Không rewrite feature — chỉ chỉnh client API + thêm mode UI theo plan CP3.
>
> **Đọc trước:** [01-BE-DA-LAM.md](./01-BE-DA-LAM.md) · **Chạy tay:** [03-THAO-TAC-BAN-TU-LAM.md](./03-THAO-TAC-BAN-TU-LAM.md)

---

## 0. Setup bắt buộc (Ngày 1)

- [ ] BE chạy: `docker compose up -d` + `./mvnw spring-boot:run`
- [ ] Chạy đủ 5 file SQL (4 file cũ + `week-1/2026-week1_location_sources.sql`)
- [ ] `.env` FE: `VITE_API_URL=http://localhost:8080`
- [ ] BE `.env`: `GEMINI_API_KEY=...` (cho chat)
- [ ] Smoke BE: `powershell -ExecutionPolicy Bypass -File docs/scripts/smoke-golden-path.ps1` pass

---

## 1. Điều chỉnh code FE cũ (breaking changes)

### 1.1 List API → đọc `data.items`

| Endpoint | Trước | Sau |
|----------|-------|-----|
| `GET /api/locations` | `data` = array | `data.items` |
| `GET /api/quests` | `data` = array | `data.items` |
| `GET /api/me/quests` | `data` = array | `data.items` |
| `GET /api/chat/.../messages` | `data` = array | `data.items` |

**Vẫn là array:** `characters`, `photo-pairs`, `panoramas`, `hotspots`, `badges`, `photo-frames`, `leaderboard.entries`

### 1.2 Validation error = HTTP 422

Map `code === 'VALIDATION_ERROR'` → hiển thị `fieldErrors`, không assume 400.

### 1.3 Auth

Lưu `accessToken` (hoặc `token` — cùng giá trị). Tuần 1 có thể chỉ dùng `token`; refresh flow optional.

```typescript
const API_BASE = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken') ?? localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function unwrap<T>(res: { data: { success: boolean; data: T } }): T {
  return res.data.data;
}

export function unwrapPage<T>(res: {
  data: {
    success: boolean;
    data: { items: T[]; page: number; size: number; totalItems: number; totalPages: number };
  };
}) {
  return res.data.data;
}

// Ví dụ Explore
const page = unwrapPage<Location>(await api.get('/api/locations', { params: { page: 0, size: 20 } }));
const locations = page.items;
```

### 1.4 Bỏ mock

Sau khi SQL chạy xong, Explore/Quest/Chat/Profile dùng API thật. UUID Củ Chi: `11111111-1111-1111-1111-111111111111`.

---

## 2. Checklist theo ngày CP3

### Ngày 1 — Data thật + golden path

- [ ] Thay mọi `mockLocations` / hardcoded list bằng `GET /api/locations?page=0&size=20`
- [ ] Location detail: `GET /api/locations/{id}`
- [ ] Photo slider: `GET /api/photo-pairs/by-location/{id}`
- [ ] Gỡ/bật flag quest seed + `featuredCards` Stitch nếu đang chặn data API
- [ ] Chạy golden path local: register → locations → characters → panorama → chat

### Ngày 2–4 — Online / Offline mode (Prompt #1)

**Không cần API BE mới.** Làm trên FE:

- [ ] `ModeSelectPage` sau login: "Bạn đang ở đâu?"
  - `Khám phá từ xa` → `ONLINE`
  - `Đang tại di tích` → `OFFLINE`
- [ ] `AppModeContext` + `localStorage` (`timelens.appMode`)
- [ ] TopNav badge: `Khám phá từ xa` / `Tại di tích` + nút đổi mode
- [ ] **ONLINE** highlight: Explore, Tour360, Chat, TimePortal (photo slider)
- [ ] **OFFLINE** highlight: Scan, Quests, SecretStory, PhotoFrame, Share
- [ ] **KHÔNG** xóa logic feature cũ — chỉ group + điều hướng

### Ngày 3 — ONLINE flow

| Bước | API |
|------|-----|
| Explore | `GET /api/locations?page&size` → `items` |
| Location detail | `GET /api/locations/{id}` |
| Tour 360 | `GET /api/panoramas/by-location/{id}` |
| Hotspots | `GET /api/hotspots/by-panorama/{panoramaId}` |
| Chat | `POST /api/chat` (JWT), `characterId` từ `GET /api/characters/by-location/{id}` |
| Photo slider | `GET /api/photo-pairs/by-location/{id}` |

### Ngày 4 — OFFLINE flow

| Bước | API |
|------|-----|
| Check-in QR/GPS | `POST /api/checkins` |
| Quests | `GET /api/quests?locationId=`, `GET /api/me/quests`, `POST .../start` |
| Secret | `GET /api/locations/{id}/secret-story` (JWT) |
| Photo frame | `GET /api/photo-frames`, `POST /api/user-creations` |
| Share | `GET /api/share/prefill`, `POST .../record-share` |

### Ngày 5 — Hiển thị chat có nguồn

- [ ] `POST /api/chat` — contract **không đổi**, `data.reply` là string
- [ ] Render reply nguyên văn; dòng `Nguồn: ...` ở cuối do BE/Gemini thêm
- [ ] Optional: style riêng dòng bắt đầu `Nguồn:` (nhỏ, màu muted)
- [ ] Error `BUSINESS_RULE` (rate limit): toast tiếng Việt

### Ngày 6 — Virtual Tour đa scene (Prompt #3)

- [ ] Cài `@photo-sphere-viewer/virtual-tour-plugin` (lazy load)
- [ ] `GET /api/panoramas/by-location/{locationId}` — danh sách scene
- [ ] `GET /api/hotspots/by-panorama/{panoramaId}` — info + `type: scene`
- [ ] Nối ≥3 scene demo; **tạm** dùng ảnh Google Street View Củ Chi nếu `imageUrl` placeholder
- [ ] Giữ layout Stitch, không rewrite page

### Ngày 7 — Mobile LAN + review

- [ ] `VITE_API_URL=http://<IP-LAN-MÁY-BE>:8080` trên điện thoại
- [ ] BE `.env`: `CORS_ALLOWED_ORIGINS=http://localhost:5173,http://<IP-LAN>:5173`
- [ ] Test cả 2 mode trên mobile
- [ ] Ghi bug internal review

---

## 3. Map route → API (Tuần 1 scope)

| Trang / feature | API chính | Paginated? |
|-----------------|-----------|------------|
| Login / Register | `POST /api/auth/login`, `register` | — |
| Explore | `GET /api/locations` | **Có** (`items`) |
| Location detail | `GET /api/locations/{id}` | — |
| Photo slider | `GET /api/photo-pairs/by-location/{id}` | Array |
| Tour 360 | `panoramas/by-location`, `hotspots/by-panorama` | Array |
| Chat | `POST /api/chat`, `GET .../messages` | Messages: **Có** |
| Profile | `GET /api/profile/me` | — |
| Scan (offline) | `POST /api/checkins` | — |
| Quests (offline) | `GET /api/quests`, `/api/me/quests` | **Có** |

---

## 4. Error handling

```typescript
function getFriendlyErrorMessage(err: unknown): string {
  const data = (err as { response?: { data?: { code?: string; message?: string } } })?.response?.data;
  if (!data) return 'Không kết nối được máy chủ. Kiểm tra mạng.';
  switch (data.code) {
    case 'VALIDATION_ERROR': return data.message ?? 'Dữ liệu không hợp lệ';
    case 'UNAUTHORIZED': return 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.';
    case 'BUSINESS_RULE': return data.message ?? 'Không thực hiện được thao tác này.';
    case 'NOT_FOUND': return 'Không tìm thấy dữ liệu.';
    default: return data.message ?? 'Đã xảy ra lỗi.';
  }
}
```

Dùng `useToast` — copy tiếng Việt, không để màn trắng.

---

## 5. Appendix — Prompt Cursor (rút gọn từ plan)

### Prompt #1 — Online/Offline (Ngày 2–4)

```
Task: Thêm AppMode ONLINE/OFFLINE.
- ModeSelectPage sau login: "Khám phá từ xa" | "Đang tại di tích"
- AppModeContext + localStorage trong src/shared/
- TopNav badge + đổi mode
- ONLINE: Explore, Tour360, Chat, TimePortal
- OFFLINE: Scan, Quests, SecretStory, PhotoFrame, Share
- KHÔNG xóa feature, KHÔNG đổi layout Stitch
```

### Prompt #3 — Virtual Tour (Ngày 6)

```
Task: Tour360Page dùng @photo-sphere-viewer/virtual-tour-plugin.
- API: panoramas/by-location, hotspots/by-panorama
- ≥3 scene, lazy load plugin
- Tạm Street View Củ Chi nếu imageUrl placeholder
- Giữ binding API hiện có
```

---

## 6. Không làm trong Tuần 1

- Deploy BE production (Tuần 3)
- Ảnh 360 thật từ Củ Chi (Tuần 3)
- Polish pixel-perfect Stitch (Tuần 2)
- Focus Group 20 users (Tuần 2–3)
- Thêm API BE cho visit mode — không cần

---

*FE đọc kèm [01-BE-DA-LAM.md](./01-BE-DA-LAM.md) và [BE_PROJECT_STATUS_AND_FE_GUIDE.md](../BE_PROJECT_STATUS_AND_FE_GUIDE.md).*
