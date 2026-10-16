'use client';

interface BarChartProps {
  data: { label: string; value: number; color?: string }[];
  onSelect?: (label: string) => void;
  formatValue?: (value: number) => string;
  height?: number;
}

export default function BarChart({ data, onSelect, formatValue, height = 180 }: BarChartProps) {
  const max = Math.max(1, ...data.map((d) => d.value));

  if (data.length === 0) {
    return <p className="text-sm text-[#9ca3af] py-8 text-center">Không có dữ liệu.</p>;
  }

  return (
    <div className="flex items-end gap-3 w-full" style={{ height }}>
      {data.map((d) => {
        const h = Math.max(4, Math.round((d.value / max) * (height - 40)));
        return (
          <button
            key={d.label}
            type="button"
            onClick={() => onSelect?.(d.label)}
            className={`flex-1 flex flex-col items-center justify-end gap-1 group ${
              onSelect ? 'cursor-pointer' : 'cursor-default'
            }`}
            title={`${d.label}: ${formatValue ? formatValue(d.value) : d.value}`}
          >
            <span className="text-[11px] font-semibold text-[#0a0a0a]">
              {formatValue ? formatValue(d.value) : d.value}
            </span>
            <span
              className="w-full rounded-t-lg transition-all group-hover:opacity-80"
              style={{ height: h, backgroundColor: d.color ?? '#c58e00' }}
            />
            <span className="text-[10px] text-[#6b7280] text-center leading-tight truncate w-full">
              {d.label}
            </span>
          </button>
        );
      })}
    </div>
  );
}
