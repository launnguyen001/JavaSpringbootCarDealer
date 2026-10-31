'use client';

import { useEffect, useState } from 'react';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
import { getOrdersByRevenue } from '@/lib/api';
import { formatDateTime, formatVnd } from '@/lib/utils/format';
import type { RevenueCategory, RevenueOrderSummary } from '@/types';

type Props = {
  period: string;
  category: RevenueCategory;
  title: string;
  amount: number;
  filters: { brandId?: number; modelId?: number };
  onClose: () => void;
  onSelectOrder: (orderId: number) => void;
};

const columns: Column<RevenueOrderSummary>[] = [
  { key: 'orderCode', label: 'Mã đơn' },
  { key: 'customerName', label: 'Khách hàng', render: row => row.customerName ?? '—' },
  { key: 'orderDate', label: 'Ngày đặt', render: row => formatDateTime(row.orderDate) },
  { key: 'status', label: 'Trạng thái', render: row => <StatusPill status={row.status} /> },
  { key: 'revenueAmount', label: 'Đóng góp doanh thu', className: 'text-right', render: row => <b>{formatVnd(row.revenueAmount)}</b> },
  { key: 'totalAmount', label: 'Tổng giá trị đơn', className: 'text-right', render: row => formatVnd(row.totalAmount) },
];

export default function DashboardRevenueOrdersDrawer({
  period, category, title, amount, filters, onClose, onSelectOrder,
}: Props) {
  const [orders, setOrders] = useState<RevenueOrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    getOrdersByRevenue(period, category, filters)
      .then(rows => { if (active) setOrders(rows); })
      .catch(() => { if (active) setError('Không thể tải danh sách đơn hàng. Vui lòng thử lại.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [period, category, filters]);

  useEffect(() => {
    const key = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose(); };
    window.addEventListener('keydown', key);
    return () => window.removeEventListener('keydown', key);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-[110]">
      <button aria-label="Đóng danh sách đơn hàng" className="absolute inset-0 bg-black/40" onClick={onClose} />
      <aside className="absolute right-0 top-0 flex h-full w-full max-w-[min(1200px,90vw)] flex-col bg-white shadow-2xl">
        <header className="flex items-center justify-between border-b border-[#e5e7eb] px-6 py-5">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wider text-[#9ca3af]">Drilldown doanh thu · {period}</p>
            <h2 className="mt-1 text-xl font-semibold text-[#111827]">{title}</h2>
            <p className="mt-1 text-sm text-[#6b7280]">Tổng trên biểu đồ: <b>{formatVnd(amount)}</b> · {orders.length.toLocaleString('vi-VN')} đơn đóng góp</p>
          </div>
          <button onClick={onClose} aria-label="Đóng" className="rounded-xl p-2 text-xl text-[#6b7280] hover:bg-[#f3f4f6]">×</button>
        </header>
        <div className="flex-1 overflow-auto p-6">
          {loading ? <p className="py-16 text-center text-sm text-[#9ca3af]">Đang tải danh sách đơn hàng…</p>
            : error ? <p className="py-16 text-center text-sm text-red-600">{error}</p>
              : <DataTable columns={columns} rows={orders} rowKey={row => row.orderId} empty="Không có đơn hàng đóng góp cho nhóm doanh thu này." onRowClick={row => onSelectOrder(row.orderId)} />}
        </div>
        <footer className="border-t border-[#e5e7eb] px-6 py-3 text-xs text-[#9ca3af]">
          Giá trị đóng góp được phân bổ theo cùng quy tắc tính biểu đồ; chọn một dòng để mở chi tiết đơn hàng tại chỗ.
        </footer>
      </aside>
    </div>
  );
}
