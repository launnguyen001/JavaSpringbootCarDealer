'use client';

import type { RevenueByMonth } from '@/types';
import { formatCurrentcy } from '@/lib/utils/format';

interface Props {
  data: RevenueByMonth[];
}

export default function RevenueTubeChart({ data }: Props) {
  const maxRevenue = Math.max(1, ...data.map((item) => item.totalRevenue));
  if (data.length === 0) {
    return <p className="py-8 text-center text-sm text-[#9ca3af]">Không có dữ liệu doanh thu.</p>;
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-[#6b7280]">
        <span className="inline-flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-sm bg-[#d6a63b]" /> Doanh thu</span>
        <span className="inline-flex items-center gap-2"><i className="h-2.5 w-2.5 rounded-sm bg-[#16a36a]" /> Thực thu của nhóm đơn trong tháng</span>
      </div>
      <div className="flex h-[205px] items-end gap-2 sm:gap-4">
        {data.map((item) => {
          const revenueHeight = Math.max(item.totalRevenue > 0 ? 8 : 2, (item.totalRevenue / maxRevenue) * 142);
          const collectionRatio = item.totalRevenue > 0
            ? Math.min(100, Math.max(0, (item.totalCollected / item.totalRevenue) * 100))
            : 0;
          return (
            <div key={item.period} className="flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-1">
              <div className="text-center text-[10px] font-semibold text-[#374151] sm:text-[11px]" title={`Doanh thu: ${formatCurrentcy(item.totalRevenue)} · Thực thu: ${formatCurrentcy(item.totalCollected)}`}>
                {formatCurrentcy(item.totalRevenue)}
              </div>
              <div className="flex items-end gap-1">
                <div
                  className="relative w-4 overflow-hidden rounded-t-lg border border-[#d6a63b]/70 bg-[#f8edcf] shadow-inner"
                  style={{ height: revenueHeight }}
                  title={`Doanh thu ${formatCurrentcy(item.totalRevenue)}`}
                >
                  <div className="absolute left-1/2 top-1 h-[3px] w-1/3 -translate-x-1/2 rounded-full bg-[#b88724]/70" />
                </div>
                <div
                  className="relative w-4 overflow-hidden rounded-t-lg border border-[#16a36a]/50 bg-[#e7f7ef] shadow-inner"
                  style={{ height: revenueHeight }}
                  title={`Thực thu ${formatCurrentcy(item.totalCollected)}`}
                >
                  <div
                    className="absolute bottom-0 w-full bg-gradient-to-t from-[#0b8055] to-[#42c98b]"
                    style={{ height: `${collectionRatio}%`, transformOrigin: 'bottom', animation: 'piggy-fill 1100ms ease-out both' }}
                  />
                </div>
              </div>
              <span className="w-full truncate text-center text-[10px] text-[#6b7280]">{item.period}</span>
              <span className="text-[9px] font-medium text-[#0b8055]">{formatCurrentcy(item.totalCollected)}</span>
              <span className="text-[9px] font-medium text-[#0b8055]">{collectionRatio.toFixed(0)}%</span>
            </div>
          );
        })}
      </div>
      <div className="flex flex-wrap justify-between gap-3 border-t border-[#f0f0f0] pt-3 text-xs">
        <span className="text-[#6b7280]">Tổng doanh thu: <b className="text-[#111827]">{formatCurrentcy(data.reduce((sum, item) => sum + item.totalRevenue, 0))}</b></span>
        <span className="text-[#6b7280]">Tổng thực thu: <b className="text-[#0b8055]">{formatCurrentcy(data.reduce((sum, item) => sum + item.totalCollected, 0))}</b></span>
      </div>
      <p className="text-[10px] leading-relaxed text-[#9ca3af]">Thực thu được quy về tháng xác nhận đơn hàng, kể cả khi khách thanh toán ở tháng sau. Chọn danh sách công nợ để xem các hóa đơn còn phải thu.</p>
      <style jsx>{`@keyframes piggy-fill { from { transform: scaleY(0); } to { transform: scaleY(1); } }`}</style>
    </div>
  );
}
