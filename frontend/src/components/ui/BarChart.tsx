'use client';

interface BarChartProps {
  data: { label: string; value: number; color?: string }[];
  onSelect?: (label: string) => void;
  formatValue?: (value: number) => string;
  height?: number;
}

/** Màu theo trạng thái nghiệp vụ để biểu đồ dễ đọc hơn. */
const STATUS_COLORS: Record<string, string> = {
  PENDING: '#d6a63b',
  CONFIRMED: '#3b6fd4',
  COMPLETED: '#16a36a',
  CANCELLED: '#e05252',
  AVAILABLE: '#16a36a',
  RESERVED: '#d6a63b',
  SOLD: '#6b7280',
  MAINTENANCE: '#e05252',
};

const STATUS_LABELS: Record<string, string> = {
  PENDING: 'Chờ xử lý',
  CONFIRMED: 'Đã xác nhận',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
};

const FALLBACK_COLORS = ['#c58e00', '#3b6fd4', '#16a36a', '#e05252', '#8b5cf6'];

export default function BarChart({ data, onSelect, formatValue, height = 180 }: BarChartProps) {
  if (data.length === 0) {
    return <p className="py-8 text-center text-sm text-[#9ca3af]">Không có dữ liệu.</p>;
  }

  const max = Math.max(1, ...data.map((d) => d.value));
  const plotHeight = Math.max(96, height - 40);
  const barSpace = Math.max(40, plotHeight - 26);

  return (
    <div className="w-full">
      <div className="relative flex items-stretch gap-2 sm:gap-3">
        {/* Lưới ngang phía sau cột */}
        <div
          className="pointer-events-none absolute inset-x-0 top-0 flex flex-col justify-between"
          style={{ height: plotHeight }}
          aria-hidden="true"
        >
          {[0, 1, 2, 3, 4].map((line) => (
            <span key={line} className="block border-t border-dashed border-[#f0efe9]" />
          ))}
        </div>

        {data.map((d, index) => {
          const color = d.color ?? STATUS_COLORS[d.label] ?? FALLBACK_COLORS[index % FALLBACK_COLORS.length];
          const barHeight = Math.max(d.value > 0 ? 8 : 3, Math.round((d.value / max) * barSpace));

          return (
            <button
              key={`${d.label}-${index}`}
              type="button"
              onClick={() => onSelect?.(d.label)}
              className={`group relative z-10 flex min-w-0 flex-1 flex-col items-center justify-end rounded-xl px-1 pb-1.5 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#c58e00]/50 ${
                onSelect ? 'cursor-pointer hover:bg-[#fffbeb]/70' : 'cursor-default'
              }`}
              style={{ minHeight: plotHeight }}
              title={`${STATUS_LABELS[d.label] ?? d.label}: ${formatValue ? formatValue(d.value) : d.value}`}
            >
              <span className="mb-1.5 whitespace-nowrap rounded-full bg-white/90 px-1.5 py-0.5 text-[10px] font-semibold tabular-nums text-[#374151] ring-1 ring-[#f0efe9] transition-all group-hover:-translate-y-0.5 group-hover:text-[#8a6d00] group-hover:ring-[#c58e00]/40">
                {formatValue ? formatValue(d.value) : d.value}
              </span>
              <span className="flex w-full max-w-12 items-end justify-center">
                <span
                  className="w-full rounded-t-lg rounded-b-sm transition-all duration-300 group-hover:-translate-y-0.5 group-hover:brightness-110"
                  style={{
                    height: barHeight,
                    background: `linear-gradient(180deg, ${color} 0%, ${color}b3 100%)`,
                    boxShadow: `0 10px 20px -14px ${color}`,
                  }}
                />
              </span>
            </button>
          );
        })}
      </div>

      <div className="mt-2 flex gap-2 border-t border-[#f0efe9] pt-2 sm:gap-3">
        {data.map((d, index) => (
          <div key={`${d.label}-label-${index}`} className="min-w-0 flex-1 px-1 text-center">
            <span className="block truncate text-[10px] font-semibold uppercase tracking-wide text-[#6b7280]">
              {STATUS_LABELS[d.label] ?? d.label}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
