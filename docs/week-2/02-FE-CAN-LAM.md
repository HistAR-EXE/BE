# CP3 Tuần 2 — FE cần làm (Polish + Focus Group)

> **Đọc trước:** [01-BE-DA-LAM.md](./01-BE-DA-LAM.md) · **Chạy tay:** [03-THAO-TAC-BAN-TU-LAM.md](./03-THAO-TAC-BAN-TU-LAM.md)  
> **Tiền đề:** Tuần 1 xong (mode Online/Offline, virtual tour, `data.items`).

---

## 0. Điều kiện trước khi bắt đầu Ngày 8

- [ ] Tuần 1 FE checklist [02-FE-CAN-LAM.md](../week-1/02-FE-CAN-LAM.md) đã pass
- [ ] `powershell -ExecutionPolicy Bypass -File docs/scripts/smoke-week2-screens.ps1` pass trên BE local
- [ ] `npm run build` FE pass

---

## 1. Checklist theo ngày

### Ngày 8–9 — Polish UI (Prompt #4)

**Màn ưu tiên:** Explore, Quests, Profile, Leaderboard, Scan.

**Ràng buộc:** KHÔNG rewrite component — chỉ chỉnh style so với Stitch `screen.png`.

| Màn | API BE | Việc FE |
|-----|--------|---------|
| **Explore** | `GET /api/locations` → `items` | Spacing card, cover image, rating, tab active |
| **Quests** | `quests`, `me/quests` paginated | Card height, progress `currentStep/stepsTotal` |
| **Profile** | `profile/me`, `me/badges` | Level bar, avatar, badge grid |
| **Leaderboard** | `leaderboard?scope=` → `entries` | Rank list, highlight `currentUser` |
| **Scan** | `POST /api/checkins` | QR UI, GPS feedback |

- [ ] So từng màn với Stitch (ưu tiên folder `(3)` > `(2)` > `(1)`)
- [ ] Chỉnh: spacing, font-size H1, tab active, card height, scrollbar
- [ ] Dùng `getPageData` / `unwrapPage` cho list paginated
- [ ] Leaderboard dùng `data.entries` (không phải `items`)
- [ ] Báo cáo **từng màn xong** trước khi sang màn tiếp

### Ngày 10 — Loading / Error / Empty states

| Loại | Màn | Gợi ý |
|------|-----|-------|
| **Loading** | Tất cả 5 màn | Skeleton hoặc spinner khi gọi API |
| **Loading** | Tour 360, photo frame | Spinner ảnh nặng |
| **Error** | Scan | GPS tắt, camera denied → toast tiếng Việt |
| **Error** | Mọi API | Mạng chậm, 401, 422, 500 → `getFriendlyErrorMessage` + toast |
| **Empty** | Profile badges | `me/badges` = `[]` → illustration + hướng dẫn |
| **Empty** | Quests | `me/quests` rỗng → CTA bắt đầu quest |
| **Empty** | Leaderboard | `entries: []` → text hướng dẫn check-in |

- [ ] Không để **màn trắng** khi lỗi hoặc đang load
- [ ] Copy tiếng Việt, không lộ thuật ngữ dev
- [ ] `npm run lint` + `npm run build` pass

### Ngày 11 — Chuẩn bị Focus Group

- [ ] Đọc [04-FOCUS-GROUP-SCRIPT.md](./04-FOCUS-GROUP-SCRIPT.md)
- [ ] Recruit 10–16 users (Zalo FPT, bạn bè, personas CP2)
- [ ] Incentive (trà sữa) + lịch 2 buổi
- [ ] Setup máy demo: laptop BE+FE + điện thoại cùng WiFi
- [ ] Test full flow 2 mode trước buổi

### Ngày 12–13 — Focus Group

- [ ] Buổi 1: 5–8 users, ghi notes + screen record
- [ ] Buổi 2: 5–8 users
- [ ] Affinity mapping, top 5 insights
- [ ] List bug UX/API để fix Ngày 14

### Ngày 14 — Buffer + BMC

- [ ] Fix bug P0 từ Focus Group (phối hợp BE nếu API)
- [ ] CEO/CMO: BMC finalize + testing report draft
- [ ] GD: pitch deck screenshots từ UI đã polish

---

## 2. Map API chi tiết cho polish

### Explore

```typescript
const { items, totalItems } = unwrapPage(await api.get('/api/locations', {
  params: { page: 0, size: 20, sort: 'createdAt,desc' },
}));
```

Fields UI: `coverImage`, `name`, `city`, `rating`, `distanceKm` (khi có `nearLat`/`nearLng`).

### Quests

```typescript
const quests = unwrapPage(await api.get('/api/quests', { params: { locationId, page: 0, size: 20 } }));
const mine = unwrapPage(await api.get('/api/me/quests', { params: { page: 0, size: 20 } }));
```

Progress bar: `currentStep / stepsTotal`.

### Profile

```typescript
const me = unwrap(await api.get('/api/profile/me'));
const badges = unwrap(await api.get('/api/me/badges')); // array, có thể []
```

Empty badges: `badges.length === 0`.

### Leaderboard

```typescript
const lb = unwrap(await api.get('/api/leaderboard', { params: { scope: 'all' } }));
const entries = lb.entries; // NOT items
```

Tabs: `all` | `city` | `week` — query `city` khi scope=city.

### Scan

```typescript
await api.post('/api/checkins', {
  locationId,
  latitude,
  longitude,
  qrCode: `timelens:location:${locationId}`,
});
```

Catch `422` `BUSINESS_RULE` → toast GPS/QR.

---

## 3. Error handling (copy từ Tuần 1, bổ sung)

```typescript
function getFriendlyErrorMessage(err: unknown): string {
  const data = (err as { response?: { data?: { code?: string; message?: string } } })?.response?.data;
  if (!data) return 'Không kết nối được máy chủ. Kiểm tra WiFi hoặc thử lại.';
  switch (data.code) {
    case 'VALIDATION_ERROR': return 'Vui lòng kiểm tra lại thông tin.';
    case 'BUSINESS_RULE':
      if (data.message?.includes('GPS') || data.message?.includes('distance'))
        return 'Bạn đang ở quá xa di tích. Hãy đến gần hơn hoặc dùng quét QR.';
      return data.message ?? 'Không thực hiện được thao tác này.';
    case 'UNAUTHORIZED': return 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.';
    default: return data.message ?? 'Đã xảy ra lỗi. Thử lại sau.';
  }
}
```

---

## 4. Appendix — Prompt #4 (rút gọn)

```
Task: Polish UI + loading/error/empty cho Explore, Quests, Profile, Leaderboard, Scan.
- So Stitch screen.png, chỉnh spacing/font/tab/card — KHÔNG rewrite
- Skeleton/spinner API + ảnh nặng
- GPS off, camera denied, API fail → useToast tiếng Việt
- Empty: badge, quest, leaderboard → illustration + text
- getPageData cho paginated; leaderboard dùng entries
- npm run lint && npm run build
- Làm từng màn, báo cáo từng màn xong
```

---

## 5. Không làm Tuần 2

- Deploy production (Tuần 3)
- Ảnh 360 Củ Chi thật (Tuần 3)
- Rewrite architecture FE
- Thêm API BE mới (trừ fix bug Ngày 14)

---

*FE polish + Focus Group — BE giữ nguyên contract, xem [01-BE-DA-LAM.md](./01-BE-DA-LAM.md).*
