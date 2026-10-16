# Error Report — Lỗi ESLint tồn đọng ở frontend

> **Mục đích:** Ghi lại toàn bộ lỗi/warning ESLint đang tồn đọng ở frontend để xử lý sau.
> **Trạng thái: CHƯA SỬA** (tài liệu tham chiếu, không phải task đã hoàn thành).
> **Phạm vi:** `frontend/` — cấu hình `frontend/eslint.config.mjs` (`eslint-config-next/core-web-vitals` + `typescript`).
>
> **Lưu ý quan trọng:**
> - Đây là **lỗi lint**, KHÔNG phải lỗi build/type. `npx tsc --noEmit` sạch và `npm run build` vẫn xanh.
> - Tất cả lỗi đều **có sẵn từ trước Task 6**; Task 6 (dropdown chọn đơn hàng) **không thêm lỗi nào**.
> - Số dòng ghi tại thời điểm lập báo cáo — sau khi sửa có thể lệch, hãy lấy lại vị trí bằng `npx eslint .`.

---

## 0. Cách tái hiện

```bash
cd frontend
npx eslint .            # -> 18 errors, 6 warnings
npx tsc --noEmit        # -> sạch
npm run build           # -> xanh
```

**Nguyên nhân gốc:** bản `eslint-plugin-react-hooks` mới (React 19) bật thêm 2 rule nghiêm ngặt:
`react-hooks/refs` và `react-hooks/set-state-in-effect`. Phần lớn lỗi là do mẫu "fetch dữ liệu trong `useEffect`"
và "gán `ref.current = value` ngay trong render" — cả hai đều là pattern hợp lệ về mặt chạy nhưng bị rule mới coi là
chưa tối ưu (gây cascading render / ref không ổn định).

---

## 1. Bảng tổng hợp

| Nhóm | Rule | Số lỗi |
|---|---|---|
| A | `react-hooks/set-state-in-effect` | 12 |
| B | `react-hooks/refs` | 2 |
| C | `@next/next/no-html-link-for-pages` | 2 |
| D | `@typescript-eslint/no-explicit-any` | 2 |
| | **Tổng lỗi (error)** | **18** |
| W | cảnh báo (không chặn build) | 6 |

---

## 2. Nhóm A — `react-hooks/set-state-in-effect` (12 lỗi)

> Rule báo: *"Calling setState synchronously within an effect can trigger cascading renders"*.

| # | File:dòng | Đoạn code bị flag | Ghi chú / hướng sửa |
|---|---|---|---|
| A1 | `src/app/account/page.tsx:109` | `if (!authLoading && isAuthenticated) load();` | Gate theo auth → bọc async IIFE (setState sau `await`) hoặc gọi `load()` khi trạng thái auth đổi trong event |
| A2 | `src/app/account/page.tsx:114` | `setLoginEmail(user?.email ?? '');` | Đồng bộ state form từ `user`. Có thể dùng `key={user?.email}` để reset input, hoặc khởi tạo một lần |
| A3 | `src/app/admin/orders/page.tsx:59` | `load();` | Fetch khi mount — cùng nhóm với A5/A6/A9 |
| A4 | `src/app/admin/orders/page.tsx:318` | `setCarItems([]);` | Reset khi đổi `carId` → xử lý trong `onChange` của select `carId` |
| A5 | `src/app/admin/page.tsx:167` | `load();` | Fetch khi mount |
| A6 | `src/app/admin/users/page.tsx:41` | `load();` | Fetch khi mount |
| A7 | `src/app/cart/page.tsx:42` | `if (!authLoading && isAuthenticated) load();` | Giống A1 |
| A8 | `src/app/checkout/page.tsx:64` | `if (!authLoading && isAuthenticated) load();` | Giống A1 |
| A9 | `src/components/admin/ResourceManager.tsx:142` | `load();` | Fetch khi mount |
| A10 | `src/components/ui/Pagination.tsx:24` | `setCurrentPage(Number.isNaN(p) \|\| p < 1 ? 1 : p);` | Đồng bộ từ prop/URL → tính giá trị dẫn xuất hoặc reset bằng `key` |
| A11 | `src/components/ui/SearchBar.tsx:25` | `setQuery((prev) => (prev === urlQuery ? prev : urlQuery));` | Đồng bộ ô nhập theo `urlQuery` → dùng `key={urlQuery}` hoặc tính trong render |
| A12 | `src/context/AuthContext.tsx:59` | `if (stored) setUser(stored);` (kèm `refresh().finally(...)`) | Đọc localStorage + refresh → bọc async IIFE |

### Cách sửa đề xuất cho nhóm A

1. **Fetch khi mount (A1, A3, A5, A6, A7, A8, A9, A12):** bọc trong async IIFE sao cho `setState` đầu tiên xảy ra
   **sau** `await` (pattern này đã dùng ở `frontend/src/components/admin/OrderDetailModal.tsx` và **qua lint**):

   ```tsx
   useEffect(() => {
     let cancelled = false;
     void (async () => {
       await Promise.resolve();               // đẩy ra khỏi thân effect đồng bộ
       if (cancelled) return;
       await load();
     })();
     return () => { cancelled = true; };
   }, [load]);
   ```

2. **Đồng bộ state từ prop/URL (A2, A10, A11):** ưu tiên **giá trị dẫn xuất trong render** hoặc **reset bằng `key`**
   thay vì copy vào state trong effect. Ví dụ:
   ```tsx
   <SearchInput key={urlQuery} defaultValue={urlQuery} />
   ```
3. **Nếu cố ý giữ mẫu cũ:** thêm chỉ thị kèm lý do:
   ```tsx
   // eslint-disable-next-line react-hooks/set-state-in-effect -- fetch dữ liệu khi mount là chủ đích
   load();
   ```

> Thứ tự ưu tiên gợi ý: A11, A10, A2, A4 (đồng bộ state — sửa gọn, ít rủi ro) trước; rồi tới nhóm fetch (A1, A3, A5–A9, A12).

---

## 3. Nhóm B — `react-hooks/refs` — "Cannot access refs during render" (2 lỗi)

| # | File:dòng | Đoạn code bị flag |
|---|---|---|
| B1 | `src/components/admin/ResourceManager.tsx:127` | `listRef.current = list;` |
| B2 | `src/components/ui/SearchBar.tsx:21` | `searchParamsRef.current = searchParams;` |

**Nguyên nhân:** gán `ref.current` ngay trong thân render (React coi ref không dùng để render).

**Hướng sửa:** chuyển việc gán vào `useEffect` (rule cho phép), đặt **trước** effect gọi `load()` để đảm bảo ref đã mới:

```tsx
const listRef = useRef(list);
useEffect(() => {
  listRef.current = list;
}, [list]);
```

Hoặc tách một hook `useLatest` dùng chung cho cả hai chỗ. (`SearchBar` chỉ cần giữ `searchParams` mới nhất để debounce
không phải thêm dependency — chuyển gán vào effect là đủ.)

---

## 4. Nhóm C — `@next/next/no-html-link-for-pages` (2 lỗi)

| # | File:dòng | Đoạn code |
|---|---|---|
| C1 | `src/components/layout/Footer.tsx:16` | `<a href="/" ...>Hãng xe</a>` |
| C2 | `src/components/layout/Footer.tsx:17` | `<a href="/cars" ...>Danh sách xe</a>` |

**Nguyên nhân:** dùng thẻ `<a>` để điều hướng nội bộ thay vì `next/link` (gây full page reload, mất client-side routing).

**Hướng sửa:**

```tsx
import Link from 'next/link';
// ...
<li><Link href="/" className="hover:text-[#c58e00] transition-colors">Hãng xe</Link></li>
<li><Link href="/cars" className="hover:text-[#c58e00] transition-colors">Danh sách xe</Link></li>
```

*(Các `<a href="#">` ở dòng 18/24/25 không bị flag vì không trỏ tới page nội bộ.)*

---

## 5. Nhóm D — `@typescript-eslint/no-explicit-any` (2 lỗi)

| # | File:dòng | Đoạn code |
|---|---|---|
| D1 | `src/hooks/useDebounce.ts:3` | `export function useDebounce<T extends (...args: any[]) => void>(` |
| D2 | `src/hooks/useDebounce.ts:25` | `(...args: any[]) => {` |

**Hướng sửa:** dùng `Parameters<T>` / `unknown[]` thay cho `any`:

```tsx
export function useDebounce<T extends (...args: never[]) => void>(
  callback: T,
  delay: number,
): T {
  // ...
  return useCallback(
    (...args: Parameters<T>) => {
      // ...
      callbackRef.current(...args);
    },
    [delay],
  ) as T;
}
```

*(Chọn `never[]`/`unknown[]` cho ràng buộc generic để giữ type an toàn; kiểm tra lại với các nơi gọi `useDebounce`.)*

---

## 6. Warnings (6 — không chặn build)

| # | File:dòng | Rule | Ghi chú |
|---|---|---|---|
| W1 | `src/app/not-found.tsx:6` | `@next/next/no-img-element` | Dùng `<img>` → cân nhắc `next/image` |
| W2 | `src/app/page.tsx:29` | `@next/next/no-img-element` | 〃 |
| W3 | `src/components/car/CarGallery.tsx:37` | `@next/next/no-img-element` | 〃 |
| W4 | `src/components/car/CarGallery.tsx:60` | `@next/next/no-img-element` | 〃 |
| W5 | `src/app/page.tsx:138` | `@typescript-eslint/no-unused-vars` | `searchCars` khai báo nhưng không dùng → xoá |
| W6 | `src/lib/api/http.ts:136` | `import/no-anonymous-default-export` | Gán object vào biến trước khi `export default` |

**Hướng sửa**
- W1–W4: thay bằng `next/image` (cần `width`/`height` hoặc `fill` + `sizes`), hoặc nếu ảnh động/không rõ kích thước thì
  thêm `// eslint-disable-next-line @next/next/no-img-element` kèm lý do.
- W5: xoá biến `searchCars` thừa (kiểm tra không còn tham chiếu).
- W6: `const httpClient = { ... }; export default httpClient;`

---

## 7. Thứ tự thực hiện đề xuất

1. **C (Footer `<a>` → `<Link>`)** — nhanh, rủi ro thấp, sửa hành vi điều hướng.
2. **D (`useDebounce` bỏ `any`)** — cục bộ, dễ kiểm chứng bằng `tsc`.
3. **B (chuyển gán `ref.current` vào `useEffect`)** — 2 chỗ, ít ảnh hưởng.
4. **A (12 lỗi set-state-in-effect)** — làm từng file, ưu tiên các chỗ đồng bộ state (A2, A10, A11, A4)
   trước, rồi tới nhóm fetch khi mount. Mỗi file chạy lại `npx eslint <file>` để xác nhận.
5. **Warnings** — dọn sau cùng (thay `next/image` cần kiểm tra hiển thị thực tế).

> Phương án dự phòng: nếu muốn "xanh" nhanh mà vẫn giữ code, có thể hạ mức rule trong `eslint.config.mjs`
> (ví dụ `"react-hooks/set-state-in-effect": "warn"`). **Không khuyến khích** — chỉ dùng tạm, vì che mất
> cảnh báo thật về hiệu năng render.

---

## 8. Checklist nghiệm thu (sau khi sửa)

- [ ] `cd frontend && npx eslint .` → **0 error** (warning còn lại phải được biện minh hoặc xử lý)
- [ ] `npx tsc --noEmit` → sạch
- [ ] `npm run build` → xanh
- [ ] Khởi động lại server prod :3000 và kiểm tra các trang bị ảnh hưởng render đúng
      (`/`, `/not-found`, `/cars/[id]` với CarGallery, `/admin`, `/admin/orders`, `/admin/users`,
      `/cart`, `/checkout`, `/account`, `/login`)
- [ ] Kiểm tra hành vi đã sửa: điều hướng bằng `<Link>` ở footer không reload trang; ô tìm kiếm vẫn đồng bộ URL;
      phân trang vẫn đúng; dữ liệu admin vẫn tải khi mount

---

## 9. Liên quan tới Task 6

- Task 6 (xem `task6.md`) chỉ **chèn thêm** import/type/state/hàm nạp options trong `ResourceManager.tsx`;
  2 lỗi của file này (B1 `listRef.current = list`, A9 `load()`) nằm trên **code cũ**, không phải do Task 6.
- Hàm nạp options của Task 6 được gọi trong **click handler** (`openCreate`) — chủ đích để **không** phát sinh
  thêm lỗi `react-hooks/set-state-in-effect`.
- Danh sách file Task 6 thêm mới (không gây lỗi lint): `backend/.../dto/OrderOptionResponse.java`,
  `backend/.../controller/SalesOrderOptionMockMvcTests.java`, cùng các sửa đổi ở
  `frontend/src/{types/order.ts,types/index.ts,lib/api/orders.ts,app/admin/{invoices,contracts,payments}/page.tsx}`.
