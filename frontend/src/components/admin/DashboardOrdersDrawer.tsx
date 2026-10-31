'use client';

import { useEffect, useState } from 'react';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
import { getOrdersByStatus } from '@/lib/api';
import { formatDateTime, formatVnd } from '@/lib/utils/format';
import type { OrderSummary } from '@/types';

type Props = {
  status: string;
  title: string;
  count: number;
  filters: { from: string; to: string; brandId?: number; modelId?: number };
  onClose: () => void;
  onSelectOrder: (orderId: number) => void;
};

const columns: Column<OrderSummary>[] = [
  { key: 'orderCode', label: 'Mã đơn' },
  { key: 'customerName', label: 'Khách hàng', render: (row) => row.customerName ?? '—' },
  { key: 'orderDate', label: 'Ngày đặt', render: (row) => formatDateTime(row.orderDate) },
  { key: 'totalAmount', label: 'Tổng tiền', className: 'text-right', render: (row) => formatVnd(row.totalAmount) },
  { key: 'status', label: 'Trạng thái', render: (row) => <StatusPill status={row.status} /> },
];

export default function DashboardOrdersDrawer({ status, title, count, filters, onClose, onSelectOrder }: Props) {
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    getOrdersByStatus(status, filters)
      .then((rows) => { if (active) setOrders(rows); })
      .catch(() => { if (active) setError('Không thể tải danh sách đơn hàng. Vui lòng thử lại.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [status, filters]);

  useEffect(() => {
    const key = (event: KeyboardEvent) => { if (event.key === 'Escape') onClose(); };
    window.addEventListener('keydown', key);
    return () => window.removeEventListener('keydown', key);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-[110]">
      <button aria-label="Đóng danh sách đơn hàng" className="absolute inset-0 bg-black/40" onClick={onClose} />
      <aside className="absolute right-0 top-0 flex h-full w-full max-w-[min(1100px,82vw)] flex-col bg-white shadow-2xl">
        <header className="flex items-center justify-between border-b border-[#e5e7eb] px-6 py-5">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-wider text-[#9ca3af]">Drilldown đơn hàng</p>
            <h2 className="mt-1 text-xl font-semibold text-[#111827]">{title}</h2>
            <p className="mt-1 text-sm text-[#6b7280]">{count.toLocaleString('vi-VN')} đơn theo biểu đồ</p>
          </div>
          <button onClick={onClose} aria-label="Đóng" className="rounded-xl p-2 text-xl text-[#6b7280] hover:bg-[#f3f4f6]">×</button>
        </header>
        <div className="flex-1 overflow-auto p-6">
          {loading ? <p className="py-16 text-center text-sm text-[#9ca3af]">Đang tải danh sách đơn hàng…</p>
            : error ? <p className="py-16 text-center text-sm text-red-600">{error}</p>
              : <DataTable columns={columns} rows={orders} rowKey={(row) => row.orderId} empty="Không có đơn hàng phù hợp." onRowClick={(row) => onSelectOrder(row.orderId)} />}
        </div>
        <footer className="border-t border-[#e5e7eb] px-6 py-3 text-xs text-[#9ca3af]">
          Danh sách kế thừa khoảng ngày, hãng và model đang chọn trên Dashboard. Chọn một dòng để mở popup chi tiết đơn hàng.
        </footer>
      </aside>
    </div>
  );
}
