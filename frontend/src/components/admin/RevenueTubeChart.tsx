'use client';

import type { RevenueByMonth, RevenueCategory } from '@/types';
import { formatCurrentcy } from '@/lib/utils/format';

interface Props {
  data: RevenueByMonth[];
  onSelect?: (period: string, category: RevenueCategory, amount: number) => void;
}

const SERIES: {
  key: RevenueCategory;
  label: string;
  title: string;
  text: string;
  pill: string;
  dot: string;
  from: string;
  to: string;
  hover: string;
}[] = [
  {
    key: 'PENDING',
    label: 'Doanh thu chờ',
    title: 'Doanh thu chờ',
    text: 'text-[#8a6d00]',
    pill: 'border-[#ead9a8] bg-[#fffdf5]',
    dot: 'bg-[#d6a63b]',
    from: '#c58e00',
    to: '#f1d48a',
    hover: 'hover:bg-[#d6a63b]/10',
  },
  {
    key: 'RECOGNIZED',
    label: 'Đã ghi nhận',
    title: 'Doanh thu đã ghi nhận',
    text: 'text-[#315da8]',
    pill: 'border-[#c9d8f5] bg-[#f7faff]',
    dot: 'bg-[#5277c3]',
    from: '#315da8',
    to: '#94b6f0',
    hover: 'hover:bg-[#5277c3]/10',
  },
  {
    key: 'ACTUAL',
    label: 'Thực nhận lũy kế',
    title: 'Thực nhận lũy kế',
    text: 'text-[#0b8055]',
    pill: 'border-[#bfe8d5] bg-[#f6fdf9]',
    dot: 'bg-[#16a36a]',
    from: '#0b8055',
    to: '#74d8aa',
    hover: 'hover:bg-[#16a36a]/10',
  },
];

const PLOT_HEIGHT = 148;
const LABEL_HEIGHT = 30;

function amountFor(item: RevenueByMonth, category: RevenueCategory): number {
  if (category === 'PENDING') return item.pendingRevenue;
  if (category === 'RECOGNIZED') return item.recognizedRevenue;
  return item.actualRevenue;
}

export default function RevenueTubeChart({ data, onSelect }: Props) {
  if (data.length === 0) {
    return <p className="py-8 text-center text-sm text-[#9ca3af]">Không có dữ liệu doanh thu.</p>;
  }

  const maxRevenue = Math.max(
    1,
    ...data.flatMap((item) => [
      item.pendingRevenue,
      item.recognizedRevenue,
      item.actualRevenue,
    ]),
  );

  const totals = data.reduce(
    (sum, item) => ({
      pending: sum.pending + item.pendingRevenue,
      recognized: sum.recognized + item.recognizedRevenue,
      actual: sum.actual + item.actualRevenue,
    }),
    { pending: 0, recognized: 0, actual: 0 },
  );

  const barHeight = (value: number) =>
    Math.max(value > 0 ? 7 : 2, Math.round((value / maxRevenue) * (PLOT_HEIGHT - 10)));

  const ticks = [0, 1, 2, 3, 4].map((index) => maxRevenue - (maxRevenue * index) / 4);

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-1.5">
          {SERIES.map((series) => (
            <span
              key={series.key}
              className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[10px] font-medium ${series.pill} ${series.text}`}
            >
              <i className={`h-2 w-2 rounded-full ${series.dot}`} />
              {series.label}
            </span>
          ))}
        </div>
        {onSelect && (
          <span className="text-[10px] font-medium text-[#9ca3af]">Nhấn vào cột để xem đơn</span>
        )}
      </div>

      <div className="rounded-2xl border border-[#f0ede4] bg-gradient-to-b from-[#fcfcfa] to-white p-3 sm:p-4">
        <div className="flex gap-2 sm:gap-3">
          {/* Trục tung */}
          <div
            className="flex w-11 shrink-0 flex-col justify-between text-right text-[9px] tabular-nums text-[#9ca3af]"
            style={{ height: PLOT_HEIGHT }}
            aria-hidden="true"
          >
            {ticks.map((tick, index) => (
              <span key={index} className="leading-none">
                {formatCurrentcy(tick)}
              </span>
            ))}
          </div>

          <div className="min-w-0 flex-1 overflow-x-auto">
            <div className="relative" style={{ minWidth: data.length > 6 ? `${data.length * 64}px` : undefined }}>
              {/* Lưới ngang */}
              <div
                className="pointer-events-none absolute inset-x-0 top-0 flex flex-col justify-between"
                style={{ height: PLOT_HEIGHT }}
                aria-hidden="true"
              >
                {[0, 1, 2, 3, 4].map((line) => (
                  <span key={line} className="block border-t border-dashed border-[#f0efe9]" />
                ))}
              </div>

              {/* Cột */}
              <div
                className="relative z-10 flex items-end gap-2 sm:gap-4"
                style={{ height: PLOT_HEIGHT }}
              >
                {data.map((item) => (
                  <div
                    key={item.period}
                    className="group/month flex h-full min-w-[48px] flex-1 flex-col items-center justify-end"
                  >
                    <div className="flex h-full w-full items-end justify-center gap-1">
                      {SERIES.map((series) => {
                        const value = amountFor(item, series.key);
                        return (
                          <button
                            key={series.key}
                            type="button"
                            onClick={() => onSelect?.(item.period, series.key, value)}
                            aria-label={`Xem đơn đóng góp ${series.title.toLowerCase()} tháng ${item.period}`}
                            className={`group/bar flex h-full items-end rounded-t-lg px-0.5 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#c58e00]/50 ${series.hover}`}
                            title={`${series.title}: ${formatCurrentcy(value)} · Click để xem đơn`}
                          >
                            <span
                              className="w-2.5 rounded-t-md transition-all duration-300 group-hover/bar:-translate-y-0.5 group-hover/bar:brightness-105 sm:w-3.5"
                              style={{
                                height: barHeight(value),
                                background: `linear-gradient(180deg, ${series.from} 0%, ${series.to} 100%)`,
                                boxShadow: `0 8px 16px -12px ${series.from}`,
                              }}
                            />
                          </button>
                        );
                      })}
                    </div>
                  </div>
                ))}
              </div>

              {/* Nhãn tháng */}
              <div
                className="relative z-10 flex gap-2 border-t border-[#f0efe9] pt-2 sm:gap-4"
                style={{ height: LABEL_HEIGHT }}
              >
                {data.map((item) => (
                  <div key={item.period} className="min-w-[48px] flex-1 text-center">
                    <span className="block truncate text-[10px] font-medium text-[#6b7280] transition-colors group-hover/month:text-[#111827]">
                      {item.period}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-3">
        <div className="rounded-xl border border-[#ead9a8] bg-[#fffdf5] px-3 py-2.5">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-[#9a6b00]">
            Doanh thu chờ
          </p>
          <p className="mt-0.5 text-sm font-bold tabular-nums text-[#8a6d00]">
            {formatCurrentcy(totals.pending)}
          </p>
        </div>
        <div className="rounded-xl border border-[#c9d8f5] bg-[#f7faff] px-3 py-2.5">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-[#315da8]">
            Đã ghi nhận
          </p>
          <p className="mt-0.5 text-sm font-bold tabular-nums text-[#315da8]">
            {formatCurrentcy(totals.recognized)}
          </p>
        </div>
        <div className="rounded-xl border border-[#bfe8d5] bg-[#f6fdf9] px-3 py-2.5">
          <p className="text-[10px] font-semibold uppercase tracking-wider text-[#0b8055]">
            Thực nhận
          </p>
          <p className="mt-0.5 text-sm font-bold tabular-nums text-[#0b8055]">
            {formatCurrentcy(totals.actual)}
          </p>
        </div>
      </div>

      <p className="text-[10px] leading-relaxed text-[#9ca3af]">
        Thực nhận được cộng dồn theo tháng xác nhận của đơn hàng, không theo ngày thanh toán.
        Khi khách thanh toán ở tháng sau, số thực nhận của tháng ghi nhận đơn sẽ tăng lên.
        Phần còn thiếu = doanh thu đã ghi nhận − thực nhận, không bao gồm đơn đang chờ.
      </p>
    </div>
  );
}
