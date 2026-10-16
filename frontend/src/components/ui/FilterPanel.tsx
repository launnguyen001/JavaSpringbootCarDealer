'use client';

import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { Brand } from '@/types/brand';
import { CarModel } from '@/types/carModel';

interface FilterPanelProps {
  brands: Brand[];
  models: CarModel[];
  selectedBrand?: string | null;
  selectedModel?: string | null;
  minPrice?: string | null;
  maxPrice?: string | null;
  minQuantity?: string | null;
}

const SELECT_CLASS =
  'w-full pl-3 pr-9 py-2 rounded-xl border border-[#e5e7eb] bg-white text-[#0a0a0a] text-sm ' +
  'focus:outline-none focus:ring-2 focus:ring-[#c58e00] focus:border-transparent appearance-none transition-shadow';

const INPUT_CLASS =
  'w-full px-3 py-2 rounded-xl border border-[#e5e7eb] bg-white text-[#0a0a0a] text-sm ' +
  'focus:outline-none focus:ring-2 focus:ring-[#c58e00] focus:border-transparent transition-shadow';

const LABEL_CLASS =
  'block text-[10px] font-semibold text-[#6b7280] uppercase mb-1 tracking-wider';

function Chevron() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#9ca3af"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none"
      aria-hidden="true"
    >
      <polyline points="6 9 12 15 18 9" />
    </svg>
  );
}

export default function FilterPanel({
  brands,
  models,
  selectedBrand,
  selectedModel,
  minPrice,
  maxPrice,
  minQuantity,
}: FilterPanelProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const pathname = usePathname();

  const filteredModels = selectedBrand
    ? models.filter((m) => String(m.brandId) === selectedBrand)
    : models;

  const hasFilter = Boolean(
    searchParams.get('q') ||
      selectedBrand ||
      selectedModel ||
      minPrice ||
      maxPrice ||
      minQuantity,
  );

  const pushParams = (params: URLSearchParams) => {
    const qs = params.toString();
    router.push(qs ? `${pathname}?${qs}` : pathname);
  };

  const setFilter = (key: string, value: string | null) => {
    const params = new URLSearchParams(searchParams.toString());
    if (value === null || value === '') {
      params.delete(key);
    } else {
      params.set(key, value);
    }
    // đổi bộ lọc thì quay về trang 1
    params.delete('page');
    pushParams(params);
  };

  const clearFilters = () => {
    const params = new URLSearchParams(searchParams.toString());
    ['q', 'brand', 'model', 'minPrice', 'maxPrice', 'minQuantity', 'page'].forEach((k) =>
      params.delete(k),
    );
    pushParams(params);
  };

  return (
    // Thanh ngang: các trường xếp trên 1 hàng ở màn rộng, tự xuống dòng ở màn nhỏ
    <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl px-4 py-3 shadow-sm">
      <div className="grid grid-cols-2 md:grid-cols-4 xl:grid-cols-6 gap-x-4 gap-y-3 items-end">
        {/* Hãng xe */}
        <div>
          <label htmlFor="filter-brand" className={LABEL_CLASS}>
            Hãng xe
          </label>
          <div className="relative">
            <select
              id="filter-brand"
              value={selectedBrand ?? ''}
              onChange={(e) => {
                const value = e.target.value || null;
                const params = new URLSearchParams(searchParams.toString());
                if (value === null) {
                  params.delete('brand');
                } else {
                  params.set('brand', value);
                }
                // đổi sang hãng khác thì bỏ dòng xe không còn thuộc hãng đó
                const currentModel = params.get('model');
                if (value && currentModel) {
                  const stillValid = models.some(
                    (m) => String(m.id) === currentModel && String(m.brandId) === value,
                  );
                  if (!stillValid) {
                    params.delete('model');
                  }
                }
                params.delete('page');
                pushParams(params);
              }}
              className={SELECT_CLASS}
            >
              <option value="">Tất cả</option>
              {brands.map((b) => (
                <option key={b.id} value={String(b.id)}>
                  {b.name}
                </option>
              ))}
            </select>
            <Chevron />
          </div>
        </div>

        {/* Dòng xe */}
        <div>
          <label htmlFor="filter-model" className={LABEL_CLASS}>
            Dòng xe
          </label>
          <div className="relative">
            <select
              id="filter-model"
              value={selectedModel ?? ''}
              onChange={(e) => setFilter('model', e.target.value || null)}
              className={SELECT_CLASS}
            >
              <option value="">Tất cả</option>
              {filteredModels.map((m) => (
                <option key={m.id} value={String(m.id)}>
                  {m.name}
                </option>
              ))}
            </select>
            <Chevron />
          </div>
        </div>

        {/* Khoảng giá — chiếm 2 cột */}
        <div className="col-span-2">
          <label htmlFor="filter-min-price" className={LABEL_CLASS}>
            Khoảng giá (triệu)
          </label>
          <div className="flex items-center gap-2">
            <input
              id="filter-min-price"
              type="number"
              min="0"
              value={minPrice ?? ''}
              onChange={(e) => setFilter('minPrice', e.target.value || null)}
              className={INPUT_CLASS}
              placeholder="Từ"
            />
            <span className="text-[#9ca3af] text-sm" aria-hidden="true">
              –
            </span>
            <input
              id="filter-max-price"
              type="number"
              min="0"
              value={maxPrice ?? ''}
              onChange={(e) => setFilter('maxPrice', e.target.value || null)}
              className={INPUT_CLASS}
              placeholder="Đến"
            />
          </div>
        </div>

        {/* Số xe còn hàng */}
        <div>
          <label htmlFor="filter-min-quantity" className={LABEL_CLASS}>
            Số xe còn hàng (tối thiểu)
          </label>
          <input
            id="filter-min-quantity"
            type="number"
            min="0"
            value={minQuantity ?? ''}
            onChange={(e) => setFilter('minQuantity', e.target.value || null)}
            className={INPUT_CLASS}
            placeholder="0"
          />
        </div>

        {/* Trạng thái bộ lọc + nút xóa */}
        <div className="flex items-center justify-between md:justify-end gap-3 min-h-[36px]">
          <span className="text-xs font-medium text-[#6b7280] whitespace-nowrap">
            {hasFilter ? (
              <>
                <span className="text-[#c58e00] mr-1">●</span>Đang lọc
              </>
            ) : (
              'Chưa chọn bộ lọc'
            )}
          </span>
          <button
            type="button"
            onClick={clearFilters}
            disabled={!hasFilter}
            className="text-xs font-medium text-[#6b7280] underline underline-offset-4 transition-colors
                       hover:text-[#c58e00] disabled:opacity-40 disabled:no-underline disabled:cursor-not-allowed whitespace-nowrap"
          >
            Xóa lọc
          </button>
        </div>
      </div>
    </div>
  );
}
