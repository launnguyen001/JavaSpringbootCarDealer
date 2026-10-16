'use client';

import PriceTag from '@/components/ui/PriceTag';
import { useSelectedItem } from '@/components/car/SelectedItemContext';

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Đã dùng' };

function formatMileage(km: number | null | undefined): string {
  if (km == null || !Number.isFinite(km)) return '—';
  return `${km.toLocaleString('vi-VN')} km`;
}

/**
 * "Tổng tiền" luôn theo chiếc đang chọn, không phải giá thấp nhất của cả lô.
 * `fallback` dùng khi dòng xe chưa có chiếc nào khả dụng.
 */
export function SelectedItemTotal({ fallback }: { fallback: number | null | undefined }) {
  const { selected } = useSelectedItem();
  return <PriceTag price={selected?.price ?? fallback} className="text-xl" />;
}

/** Tình trạng + số km đã chạy của chiếc đang chọn (km chỉ có ý nghĩa với xe cũ). */
export function SelectedItemSpecs() {
  const { selected } = useSelectedItem();
  const condition = selected ? (CONDITION_LABEL[selected.condition] ?? selected.condition) : '—';
  const mileage = selected && selected.condition === 'USED' ? formatMileage(selected.mileageKm) : '—';

  return (
    <>
      <div className="flex items-start gap-4">
        <div className="w-12 h-12 rounded-xl bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6b7280" strokeWidth="1.5">
            <path d="M20 12V8H6a2 2 0 0 1-2-2c0-1.1.9-2 2-2h12v4" />
            <path d="M4 6v12c0 1.1.9 2 2 2h14v-4" />
            <path d="M18 12a2 2 0 0 0-2 2c0 1.1.9 2 2 2h4v-4z" />
          </svg>
        </div>
        <div>
          <p className="text-xs text-[#6b7280] uppercase tracking-widest font-medium">Tình trạng</p>
          <p className="text-[#0a0a0a] font-medium">{condition}</p>
        </div>
      </div>
      <div className="flex items-start gap-4">
        <div className="w-12 h-12 rounded-xl bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6b7280" strokeWidth="1.5">
            <path d="M12 20a8 8 0 1 0 0-16 8 8 0 0 0 0 16z" />
            <path d="m12 12 3.5-3.5" />
            <circle cx="12" cy="12" r="1" />
          </svg>
        </div>
        <div>
          <p className="text-xs text-[#6b7280] uppercase tracking-widest font-medium">Số km đã chạy</p>
          <p className="text-[#0a0a0a] font-medium">{mileage}</p>
        </div>
      </div>
    </>
  );
}
