# Task — Buổi 3: Khởi tạo Frontend (Next.js + Tailwind) & tính năng công khai

> Mục tiêu: dựng **website xem xe (public)** bằng **Next.js (App Router) + TypeScript + Tailwind CSS**
> trong thư mục `frontend/`, gọi REST backend hiện có. Gồm: layout + routing, lớp `lib/api/`,
> **trang chủ**, **danh sách xe (phân trang)**, **chi tiết xe** (ảnh từ `car-media`, thông số từ `car-specs`,
> đánh giá từ `car-reviews`) và **tìm kiếm/lọc nâng cao** (theo tên / giá / tồn kho / hãng / dòng).
>
> Bám đúng contract backend đã hoàn thành ở Buổi 1–2 (xem `README.md`). **Không sửa logic backend**
> (chỉ thêm cấu hình CORS — tùy chọn). Không phá mã mẫu, không đổi test backend.
>
> **Trạng thái: CHƯA BẮT ĐẦU** — tài liệu này là kế hoạch chốt trước khi viết code.
>
> Sau khi xong: `cd frontend && npm run build` phải thành công; backend `.\mvnw.cmd test` vẫn xanh (47 test).

---

## 0. Quyết định thiết kế (chốt sẵn — có thể đổi)

1. **Stack frontend:** **Next.js 15+ (App Router)** + **TypeScript** + **Tailwind CSS**.
   Scaffold bằng `npx create-next-app@latest frontend --typescript --tailwind --app --src-dir --eslint`.
   Package manager: `npm`.
2. **Kiến trúc dữ liệu (RSC-first):** các trang đọc dữ liệu là **Server Components** gọi thẳng backend
   (server-to-server) → có SEO, không cần CORS, không cần client `useEffect`.
   Chỉ những phần tương tác mới là **Client Components**: `SearchBar`, `FilterPanel`, `Pagination`, menu mobile.
3. **Base URL backend:** biến môi trường server `API_BASE_URL` (mặc định `http://localhost:8080/api`)
   trong `.env.local`. Vì gọi server-to-server nên **không cần CORS** cho luồng chính.
4. **Proxy/rewrite (cho client & buổi 4):** `next.config.ts` cấu hình `rewrites()`:
   `/api/:path*` → `${API_BASE_URL}/:path*`. Dùng để client component (và nghiệp vụ giỏ hàng/đăng nhập ở buổi 4)
   gọi `/api/...` **cùng origin** → miễn CORS.
5. **CORS backend (tùy chọn):** vẫn thêm `config/WebConfig.java` cho `http://localhost:3000` để hỗ trợ khi
   gọi trực tiếp cổng 8080 (Postman/tool/không qua rewrite). Đánh dấu **tùy chọn**, không bắt buộc cho luồng RSC.
6. **Tìm kiếm/lọc qua URL (server-rendered):** trang `/cars` đọc `searchParams` và fetch theo bộ lọc →
   share/bookmark được, không cần state phức tạp. `FilterPanel`/`SearchBar` cập nhật query bằng `router.push`.
7. **Phân trang:** backend `GET /api/cars` trả **toàn bộ** danh sách → phân trang **phía client**
   (trên tập kết quả đã lọc), `page` + `size` (mặc định 9) truyền qua query. Khi dữ liệu lớn sẽ bổ sung
   endpoint phân trang server-side ở buổi 5.
8. **Dữ liệu phụ thuộc (Mongo) có thể rỗng:** `DataSeeder` chỉ seed Brand/CarModel/Car/Customer/Employee,
   **không seed** `car-media` / `car-specs` / `car-reviews`. Trang chi tiết phải **xử lý 404/empty**
   độc lập cho từng khối (ảnh → placeholder; thông số/đánh giá → "Chưa có dữ liệu"),
   **không để một khối lỗi làm sập cả trang** (dùng `Promise.all` + `fetchOptional` trả `null`).
9. **Ảnh:** `CarMediaResponse` có `thumbnail` + `images[]` + `videos[]` (URL). Chưa có media → placeholder.
   Dùng `next/image` cho ảnh từ URL ngoài (cấu hình `images.remotePatterns`) hoặc thẻ `<img>` nếu URL tùy biến nhiều.
10. **Giá:** backend trả `Double price` (seed 950.0 … 1200.0). Định dạng qua `formatPrice` →
    hiển thị **triệu VND** (`950 → "950 triệu"`). Chốt trong `lib/utils/format.ts`.
11. **Ngôn ngữ UI:** tiếng Việt; file `.tsx/.ts` viết UTF-8 (tránh lỗi font).
12. **Trạng thái xe:** `CarStatus = AVAILABLE | RESERVED | SOLD | MAINTENANCE` → badge màu;
    chỉ `AVAILABLE` mới bật nút "Đặt mua"/"Lái thử" (nút ở buổi 3 là placeholder/disabled, xử lý thật ở buổi 4).
13. **Không phá mã mẫu:** mọi thay đổi backend giới hạn ở file mới `config/WebConfig.java` (tùy chọn).

---

## 1. Hiện trạng

| Hạng mục | Trạng thái |
| --- | --- |
| Backend REST (Buổi 1–2) | ✅ READY, 47 test xanh, Swagger đủ endpoint |
| Cổng backend | `http://localhost:8080` (mặc định, không set `server.port`) |
| CORS | ❌ chưa cấu hình (tùy chọn cho setup Next rewrite) |
| Thư mục `frontend/` | ❌ chưa tồn tại |
| Seed SQL | 3 hãng, 4 dòng, 5 xe, 2 khách, 2 nhân viên |
| Seed Mongo | ❌ không seed media/specs/reviews |
| Node/npm | ✅ Node v25.9.0, npm 11.12.1 |

---

## 2. Cấu trúc thư mục `frontend/` (Next.js App Router)

```
frontend/
├── package.json
├── next.config.ts                 # rewrites /api/* -> backend
├── tsconfig.json
├── postcss.config.mjs             # Tailwind (v4 dùng plugin @tailwindcss/postcss)
├── eslint.config.mjs
├── .env.local                     # API_BASE_URL=http://localhost:8080/api (không commit)
├── .env.example
├── .gitignore
├── public/                        # logo, placeholder.svg
└── src/
    ├── app/
    │   ├── layout.tsx             # Root layout: <Header/> + children + <Footer/>, metadata
    │   ├── globals.css            # @import "tailwindcss"; + vài base token
    │   ├── page.tsx               # Home (trang chủ)
    │   ├── loading.tsx            # skeleton toàn cục
    │   ├── not-found.tsx          # 404
    │   └── cars/
    │       ├── page.tsx           # Danh sách + lọc + phân trang (đọc searchParams)
    │       └── [id]/
    │           └── page.tsx       # Chi tiết xe (generateMetadata + RSC fetch song song)
    ├── components/
    │   ├── layout/
    │   │   ├── Header.tsx         # nav + SearchBar nhanh
    │   │   ├── Footer.tsx
    │   │   └── MobileNav.tsx      # (client)
    │   └── ui/
    │       ├── CarCard.tsx
    │       ├── CarGrid.tsx
    │       ├── SearchBar.tsx      # (client) debounce -> router.push
    │       ├── FilterPanel.tsx    # (client) hãng/dòng/giá/tồn kho
    │       ├── Pagination.tsx     # (client) <Link> đổi ?page=
    │       ├── RatingStars.tsx
    │       ├── StatusBadge.tsx
    │       ├── PriceTag.tsx
    │       ├── Loading.tsx
    │       ├── ErrorMessage.tsx
    │       └── EmptyState.tsx
    ├── lib/
    │   ├── api/
    │   │   ├── http.ts            # fetchJson / fetchOptional + ApiError
    │   │   ├── cars.ts
    │   │   ├── brands.ts
    │   │   ├── carModels.ts
    │   │   ├── carSpecs.ts
    │   │   ├── carMedia.ts
    │   │   ├── carReviews.ts
    │   │   └── index.ts
    │   └── utils/
    │       └── format.ts          # formatPrice, formatDate
    ├── types/
    │   ├── car.ts                 # Car, CarStatus
    │   ├── brand.ts
    │   ├── carModel.ts
    │   ├── carSpec.ts
    │   ├── carMedia.ts
    │   ├── carReview.ts           # CarReview, CarReviewSummary, Reply
    │   └── index.ts
    └── hooks/
        └── useDebounce.ts         # (client)
```

> Lưu ý phiên bản: `create-next-app` hiện tại tạo **Tailwind v4** (CSS-first: `globals.css` dùng
> `@import "tailwindcss";`, không cần `tailwind.config.js`). Nếu project dùng Tailwind v3 thì thêm
> `tailwind.config.ts` + `@tailwind base/components/utilities`. Chốt theo output của `create-next-app`.

---

## 3. Ma trận route & màn hình

| Route | File | Loại | Nội dung chính | Nguồn dữ liệu |
| --- | --- | --- | --- | --- |
| `/` | `app/page.tsx` | RSC | Hero, xe nổi bật (6 xe đầu), danh sách hãng | `/api/cars`, `/api/brands` |
| `/cars` | `app/cars/page.tsx` | RSC + client filters | SearchBar + FilterPanel + lưới xe + phân trang | `/api/cars/*`, `/api/brands`, `/api/car-models` |
| `/cars/[id]` | `app/cars/[id]/page.tsx` | RSC | Gallery, giá/tồn kho, thông số, đánh giá + điểm TB | `/api/cars/{id}`, `/api/car-media/{id}`, `/api/car-specs/{id}`, `/api/car-reviews/car/{id}`(+`/summary`) |
| `*` | `app/not-found.tsx` | – | 404 | – |

**Query params của `/cars`** (đồng bộ URL để share/bookmark):
`q` (tên), `brand` (id hãng), `model` (id dòng), `minPrice`, `maxPrice`, `minQuantity`, `page`, `size`.

**Điều hướng:** dùng `next/link` (`<Link href="/cars">`), `useRouter`/`useSearchParams` từ `next/navigation`.

**Nút hành động (placeholder buổi 3):** "Đặt mua" / "Đăng ký lái thử" hiển thị nhưng `disabled`
(hoặc thông báo "Sẽ mở ở buổi 4") — nghiệp vụ thật làm ở Buổi 4.

---

## 4. API contract dùng cho Buổi 3

### 4.1. Endpoint

| Method | Path | Tham số | Trả về |
| --- | --- | --- | --- |
| GET | `/api/cars` | – | `CarResponse[]` |
| GET | `/api/cars/{id}` | – | `CarResponse` (cache Redis) |
| GET | `/api/cars/search/name` | `name` | `CarResponse[]` |
| GET | `/api/cars/search/price` | `minPrice`, `maxPrice` | `CarResponse[]` |
| GET | `/api/cars/search/stock` | `minQuantity` | `CarResponse[]` |
| GET | `/api/cars/search/advanced` | `modelName`, `minPrice`, `maxPrice`, `minQuantity` | `CarResponse[]` |
| GET | `/api/cars/search/advanced/brand` | `brandName`, `minPrice`, `maxPrice`, `minQuantity` | `CarResponse[]` |
| GET | `/api/cars/brand/{brandId}` | – | `CarResponse[]` |
| GET | `/api/cars/model/{modelId}` | – | `CarResponse[]` |
| GET | `/api/brands` | – | `BrandResponse[]` |
| GET | `/api/brands/{id}` | – | `BrandResponse` |
| GET | `/api/car-models` | – | `CarModelResponse[]` |
| GET | `/api/car-models/{id}` | – | `CarModelResponse` |
| GET | `/api/car-models/search/name` | `name` | `CarModelResponse[]` |
| GET | `/api/car-models/brand/{brandId}` | – | `CarModelResponse[]` |
| GET | `/api/car-specs/{carId}` | – | `CarSpecificationResponse` (**404 nếu chưa có**) |
| GET | `/api/car-media/{carId}` | – | `CarMediaResponse` (**404 nếu chưa có**) |
| GET | `/api/car-reviews/car/{carId}` | – | `CarReviewResponse[]` |
| GET | `/api/car-reviews/car/{carId}/summary` | – | `CarReviewSummary` (`totalReviews`, `averageRating`) |

### 4.2. Ánh xạ bộ lọc → endpoint (thứ tự ưu tiên)

| Điều kiện bộ lọc | Endpoint dùng |
| --- | --- |
| Chỉ `q` (tên) | `/api/cars/search/name?name={q}` |
| Có `brand`, không `model` | `/api/cars/search/advanced/brand?brandName={brandName}&minPrice&maxPrice&minQuantity` |
| Có `model` | `/api/cars/search/advanced?modelName={modelName}&minPrice&maxPrice&minQuantity` |
| Chỉ giá (không tên/hãng/dòng) | `/api/cars/search/price?minPrice&maxPrice` |
| Chỉ tồn kho | `/api/cars/search/stock?minQuantity` |
| Không lọc gì | `/api/cars` |

> Lưu ý: `advanced*` **bắt buộc đủ 4 tham số** → luôn gửi default `minPrice=0`, `maxPrice=10000000000`,
> `minQuantity=0`. `brandName`/`modelName` resolve từ id qua `/api/brands`, `/api/car-models` (server-side).

### 4.3. Kiểu dữ liệu TypeScript (khớp DTO backend)

```ts
// car.ts
export type CarStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'MAINTENANCE';
export interface Car {
  id: number; name: string; description: string | null;
  price: number; quantity: number; vin: string | null; year: number | null;
  color: string | null; status: CarStatus;
  modelId: number | null; modelName: string | null; brandName: string | null;
}
// brand.ts
export interface Brand { id: number; name: string; country: string | null; description: string | null; }
// carModel.ts
export interface CarModel { id: number; name: string; bodyType: string | null; description: string | null; brandId: number | null; brandName: string | null; }
// carSpec.ts
export interface CarSpecification {
  id: string; carId: number; engine: string | null; transmission: string | null; fuelType: string | null;
  seats: number | null; dimensions: string | null; weightKg: number | null;
  safetyFeatures: string[]; extra: Record<string, unknown>; updatedAt: string;
}
// carMedia.ts
export interface CarMedia { id: string; carId: number; thumbnail: string | null; images: string[]; videos: string[]; updatedAt: string; }
// carReview.ts
export interface Reply { author: string; content: string; createdAt: string; }
export interface CarReview {
  id: string; carId: number; customerId: number; customerName: string;
  rating: number; title: string | null; content: string; createdAt: string; replies: Reply[];
}
export interface CarReviewSummary { carId: number; totalReviews: number; averageRating: number; }
// error.ts (trong types/index.ts)
export interface ApiErrorBody { timestamp: string; status: number; error: string; message: string; path: string; }
```

---

## 5. Checklist công việc theo file

### Nhóm A — Backend (tùy chọn, không phá mã mẫu)
- [ ] `config/WebConfig.java` (mới, **tùy chọn**): `WebMvcConfigurer#addCorsMappings` cho `/api/**`,
      `allowedOrigins = http://localhost:3000` (đọc từ property `app.cors.allowed-origins`, có default),
      `allowedMethods = GET, POST, PUT, DELETE`, `allowedHeaders = *`.
- [ ] Xác nhận `.\mvnw.cmd test` vẫn 47 test xanh (CORS không ảnh hưởng test).

### Nhóm B — Khởi tạo dự án frontend
- [ ] `npx create-next-app@latest frontend --typescript --tailwind --app --src-dir --eslint --import-alias "@/*"`.
- [ ] `next.config.ts`: `rewrites()` → `/api/:path*` tới `${process.env.API_BASE_URL}` (bỏ hậu tố `/api`
      khi nối, tránh trùng); (nếu dùng `next/image` với ảnh ngoài → `images.remotePatterns`).
- [ ] `.env.local` + `.env.example`: `API_BASE_URL=http://localhost:8080/api`.
- [ ] `.gitignore` đã có sẵn từ scaffold (đảm bảo bỏ `.env*.local`).
- [ ] `app/globals.css`: Tailwind + class nền (màu, container).

### Nhóm C — Types & API layer (`lib/api`)
- [ ] `src/types/*` như mục 4.3.
- [ ] `src/lib/api/http.ts`:
  - `fetchJson<T>(path, init?)`: nối `API_BASE_URL`, `cache: 'no-store'` cho dữ liệu động (hoặc `next: { revalidate }`),
    `!res.ok` → ném `ApiError` (đọc `ErrorResponse`).
  - `fetchOptional<T>(path)`: trả `null` khi **404** (dùng cho specs/media).
- [ ] `cars.ts`: `listCars`, `getCar`, `searchByName`, `searchByPrice`, `searchByStock`,
      `searchAdvanced`, `searchAdvancedByBrand`, `getCarsByBrand`, `getCarsByModel`.
- [ ] `brands.ts`, `carModels.ts`: `listBrands`, `getBrand`, `listCarModels`, `getCarModelsByBrand`, `searchCarModelsByName`.
- [ ] `carSpecs.ts`: `getCarSpecs(carId)` → `CarSpecification | null`.
- [ ] `carMedia.ts`: `getCarMedia(carId)` → `CarMedia | null`.
- [ ] `carReviews.ts`: `getCarReviews(carId)`, `getCarReviewSummary(carId)`.
- [ ] `hooks/useDebounce.ts` (client).

### Nhóm D — Layout & routing
- [ ] `app/layout.tsx`: `<html lang="vi">`, `metadata` (title/mô tả), `Header` + `{children}` + `Footer`, `globals.css`.
- [ ] `Header.tsx`: logo "Car Dealer", nav (Trang chủ, Danh sách xe), `SearchBar` nhanh (điều hướng `/cars?q=`).
- [ ] `Footer.tsx`: thông tin liên hệ/đại lý.
- [ ] `MobileNav.tsx` (client): menu thu gọn trên mobile.
- [ ] `app/loading.tsx`: skeleton; `app/not-found.tsx`: 404.

### Nhóm E — Trang chủ (`app/page.tsx`)
- [ ] RSC: `listCars()` → 6 xe đầu → `CarGrid`/`CarCard`; `listBrands()` hiển thị hãng.
- [ ] `export const metadata` cho SEO.
- [ ] Có `Loading`/`ErrorMessage`/`EmptyState`.

### Nhóm F — Danh sách xe + tìm kiếm/lọc (`app/cars/page.tsx`)
- [ ] RSC: nhận `searchParams`; resolve `brand`/`model` (id → name) khi cần; gọi API theo bảng 4.2.
- [ ] Phân trang client-side trên kết quả (`page`, `size=9`) → render `Pagination`.
- [ ] `SearchBar.tsx` (client): nhập tên (debounce) → `router.push('/cars?...')`.
- [ ] `FilterPanel.tsx` (client): chọn hãng (→ nạp dòng theo hãng), dòng, khoảng giá, tồn kho tối thiểu; nút "Xóa lọc".
- [ ] `CarCard.tsx`: ảnh (thumbnail nếu có, else placeholder), tên, hãng/dòng, `PriceTag`, `StatusBadge`, link `/cars/{id}`.
- [ ] `CarGrid.tsx`, `Pagination.tsx`, `EmptyState.tsx`.
- [ ] Dùng `useSearchParams` để giữ đồng bộ URL khi lọc/phân trang.
- [ ] `export const dynamic = 'force-dynamic'` (hoặc `cache: 'no-store'`) để dữ liệu luôn mới theo query.

### Nhóm G — Chi tiết xe (`app/cars/[id]/page.tsx`)
- [ ] RSC: `params.id`; `getCar(id)`; nếu 404 → gọi `notFound()`.
- [ ] Fetch song song: `Promise.all([getCar, getCarMedia, getCarSpecs, getCarReviews, getCarReviewSummary])`
      (media/specs dùng `fetchOptional` → `null` khi thiếu).
- [ ] Gallery: `thumbnail` + `images[]` (ảnh chính + thumbnail strip), video nếu có, else placeholder.
- [ ] Khối thông số: bảng từ `CarSpecificationResponse` (engine/transmission/fuelType/seats/dimensions/weightKg/safetyFeatures/extra).
- [ ] Khối đánh giá: điểm TB + số lượng (`summary`) + danh sách review (`RatingStars`, tiêu đề, nội dung, ngày, replies).
- [ ] `generateMetadata({ params })`: title = tên xe.
- [ ] `RatingStars.tsx`, `StatusBadge.tsx`, `PriceTag.tsx`.

### Nhóm H — Tiện ích & hoàn thiện
- [ ] `lib/utils/format.ts`: `formatPrice` (triệu VND), `formatDate` (dd/MM/yyyy).
- [ ] Ẩn/hiển thị nút "Đặt mua"/"Lái thử" theo `status` (placeholder, disabled).
- [ ] Tailwind: responsive (mobile-first), trạng thái rỗng/skeleton.

### Nhóm I — Kiểm thử & tài liệu
- [ ] `npm run build` (next build) thành công, không lỗi type/lint.
- [ ] Chạy thực tế: backend `.\mvnw.cmd spring-boot:run` + `npm run dev` (Next cổng 3000);
      kiểm tra: trang chủ → danh sách → lọc theo tên/giá/tồn kho/hãng/dòng → phân trang → chi tiết xe.
- [ ] Cập nhật `README.md`: mục "Frontend" (cách chạy, cấu trúc, env `API_BASE_URL`, rewrite).
- [ ] Cập nhật `plan.md`: đánh dấu Buổi 3 `[HOÀN THÀNH]` khi xong.

---

## 6. Acceptance criteria (buổi 3)

- [ ] `frontend/` chạy được bằng `npm run dev` (Next cổng 3000); rewrite `/api/*` thông tới backend 8080.
- [ ] Trang chủ hiển thị danh sách xe + hãng từ backend (không hard-code).
- [ ] Danh sách xe: tìm theo tên, lọc theo giá/tồn kho/hãng/dòng, **phân trang** hoạt động; URL phản ánh bộ lọc.
- [ ] Chi tiết xe hiển thị: ảnh (từ `car-media`), thông số (từ `car-specs`), đánh giá + điểm TB (từ `car-reviews`).
- [ ] Trang chi tiết **không sập** khi thiếu media/specs/reviews (hiển thị placeholder/thông báo).
- [ ] `npm run build` thành công; `.\mvnw.cmd test` backend vẫn xanh 47 test.
- [ ] UI tiếng Việt (Tailwind), không lỗi font; responsive cơ bản.

---

## 7. Rủi ro & lưu ý

- **Mongo rỗng:** đã nêu — mọi khối dữ liệu phụ phải chịu được 404/empty. Có thể bổ sung seed Mongo ở buổi 5.
- **`advanced*` yêu cầu đủ tham số:** luôn gửi default (xem 4.2), tránh lỗi 400.
- **Không có endpoint phân trang server-side:** chấp nhận phân trang client (seed nhỏ). Ghi chú buổi 5.
- **`GET /api/cars/{id}` có cache Redis:** cần Redis chạy khi dev thật; đảm bảo MySQL + Mongo + Redis đang chạy.
- **RSC fetch & môi trường:** Server Component chạy trên Node của Next → dùng `API_BASE_URL` **tuyệt đối**
  (`http://localhost:8080/api`). Không dùng đường dẫn tương đối `/api` trong RSC (sẽ lỗi).
- **Rewrite & biến môi trường:** `rewrites()` đọc env lúc build/khởi động; đảm bảo `.env.local` tồn tại khi `next dev`.
- **Tailwind phiên bản:** v4 (CSS-first) vs v3 (config file) — bám theo scaffold, tránh trộn cấu hình.
- **`PriceTag`/đơn vị giá** chưa được backend quy định rõ → chốt hiển thị "triệu VND" và ghi chú trong code.
- **Encoding:** mọi file mới lưu UTF-8.

---

## 8. Ngoài phạm vi buổi 3 (để buổi 4/5)

- Giỏ cấu hình (`/api/cart`), đặt hàng (`/api/orders`), thanh toán, lái thử, gửi đánh giá.
- Đăng nhập/đăng xuất (session Redis), phân quyền.
- Trang quản trị CRUD.
- Upload media, dashboard báo cáo, docker-compose, `openapi.yaml`.
