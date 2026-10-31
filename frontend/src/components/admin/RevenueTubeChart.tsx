'use client';

import type { RevenueByMonth, RevenueCategory } from '@/types';
import { formatCurrentcy } from '@/lib/utils/format';

interface Props {
  data: RevenueByMonth[];
  onSelect?: (period: string, category: RevenueCategory, amount: number) => void;
}

export default function RevenueTubeChart({ data, onSelect }: Props) {
  const maxRevenue = Math.max(
    1,
    ...data.flatMap((item) => [
      item.pendingRevenue,
      item.recognizedRevenue,
      item.actualRevenue,
    ]),
  );

  if (data.length === 0) {
    return <p className="py-8 text-center text-sm text-[#9ca3af]">Không có dữ liệu doanh thu.</p>;
  }

  const totals = data.reduce(
    (sum, item) => ({
      pending: sum.pending + item.pendingRevenue,
      recognized: sum.recognized + item.recognizedRevenue,
      actual: sum.actual + item.actualRevenue,
    }),
    { pending: 0, recognized: 0, actual: 0 },
  );

  const barHeight = (value: number) => Math.max(value > 0 ? 5 : 2, (value / maxRevenue) * 132);

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-[#6b7280]">
        <span className="inline-flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-sm bg-[#d6a63b]" /> Doanh thu chờ</span>
        <span className="inline-flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-sm bg-[#5277c3]" /> Đã ghi nhận</span>
        <span className="inline-flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-sm bg-[#16a36a]" /> Thực nhận lũy kế</span>
      </div>

      <div className="flex h-[205px] items-end gap-2 overflow-x-auto sm:gap-4">
        {data.map((item) => (
          <div key={item.period} className="flex h-full min-w-[42px] flex-1 flex-col items-center justify-end gap-2">
            <div className="flex h-[150px] items-end gap-1">
              <button type="button" onClick={() => onSelect?.(item.period, 'PENDING', item.pendingRevenue)} aria-label={`Xem đơn đóng góp doanh thu chờ tháng ${item.period}`} className="group/bar flex items-end rounded-t-md px-0.5 hover:bg-[#d6a63b]/10 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#d6a63b]" title={`Doanh thu chờ: ${formatCurrentcy(item.pendingRevenue)} · Click để xem đơn`}>
                <span className="w-3 rounded-t-md bg-[#f8edcf] ring-1 ring-inset ring-[#d6a63b]/70 transition-colors group-hover/bar:bg-[#d6a63b] sm:w-4" style={{ height: barHeight(item.pendingRevenue) }} />
              </button>
              <button type="button" onClick={() => onSelect?.(item.period, 'RECOGNIZED', item.recognizedRevenue)} aria-label={`Xem đơn đã ghi nhận tháng ${item.period}`} className="group/bar flex items-end rounded-t-md px-0.5 hover:bg-[#5277c3]/10 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#5277c3]" title={`Doanh thu đã ghi nhận: ${formatCurrentcy(item.recognizedRevenue)} · Click để xem đơn`}>
                <span className="w-3 rounded-t-md bg-[#dce7fb] ring-1 ring-inset ring-[#5277c3]/70 transition-colors group-hover/bar:bg-[#5277c3] sm:w-4" style={{ height: barHeight(item.recognizedRevenue) }} />
              </button>
              <button type="button" onClick={() => onSelect?.(item.period, 'ACTUAL', item.actualRevenue)} aria-label={`Xem đơn thực nhận tháng ${item.period}`} className="group/bar flex items-end rounded-t-md px-0.5 hover:bg-[#16a36a]/10 focus-visible:outline focus-visible:outline-2 focus-visible:outline-[#16a36a]" title={`Thực nhận lũy kế: ${formatCurrentcy(item.actualRevenue)} · Click để xem đơn`}>
                <span className="w-3 rounded-t-md bg-[#d9f3e5] ring-1 ring-inset ring-[#16a36a]/70 transition-colors group-hover/bar:bg-[#16a36a] sm:w-4" style={{ height: barHeight(item.actualRevenue) }} />
              </button>
            </div>
            <span className="w-full truncate text-center text-[10px] text-[#6b7280]">{item.period}</span>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 gap-2 border-t border-[#f0f0f0] pt-3 text-xs sm:grid-cols-3">
        <span className="text-[#6b7280]">Doanh thu chờ: <b className="text-[#9a6b00]">{formatCurrentcy(totals.pending)}</b></span>
        <span className="text-[#6b7280]">Đã ghi nhận: <b className="text-[#315da8]">{formatCurrentcy(totals.recognized)}</b></span>
        <span className="text-[#6b7280]">Thực nhận: <b className="text-[#0b8055]">{formatCurrentcy(totals.actual)}</b></span>
      </div>
      <p className="text-[10px] leading-relaxed text-[#9ca3af]">
        Thực nhận được cộng dồn theo tháng xác nhận của đơn hàng, không theo ngày thanh toán.
        Khi khách thanh toán ở tháng sau, số thực nhận của tháng ghi nhận đơn sẽ tăng lên.
        Phần còn thiếu = doanh thu đã ghi nhận − thực nhận, không bao gồm đơn đang chờ.
      </p>
    </div>
  );
}
