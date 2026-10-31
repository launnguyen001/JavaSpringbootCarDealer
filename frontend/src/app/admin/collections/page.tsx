'use client';

import { useCallback, useEffect, useState } from 'react';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
import Button from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import { listInvoicesForCollection } from '@/lib/api';
import { formatVnd } from '@/lib/utils/format';
import type { Invoice } from '@/types';

export default function AdminCollectionsPage() {
  const toast = useToast();
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setInvoices(await listInvoicesForCollection(status || undefined));
    } catch (error) {
      toast.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }, [status, toast]);

  useEffect(() => { load(); }, [load]);

  const columns: Column<Invoice>[] = [
    { key: 'invoiceNumber', label: 'Hóa đơn' },
    { key: 'orderCode', label: 'Mã đơn' },
    { key: 'customerName', label: 'Khách hàng', render: (row) => row.customerName ?? '—' },
    { key: 'total', label: 'Phải thu', className: 'text-right', render: (row) => formatVnd(row.total) },
    { key: 'paidAmount', label: 'Đã thu', className: 'text-right', render: (row) => formatVnd(row.paidAmount) },
    { key: 'remainingAmount', label: 'Còn phải thu', className: 'text-right font-semibold', render: (row) => formatVnd(row.remainingAmount) },
    { key: 'dueDate', label: 'Hạn thanh toán', render: (row) => row.dueDate ?? '—' },
    { key: 'paymentStatus', label: 'Trạng thái', render: (row) => <StatusPill status={row.paymentStatus} /> },
  ];

  const totalOutstanding = invoices.reduce((sum, invoice) => sum + invoice.remainingAmount, 0);

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-serif font-bold text-[#111827]">Theo dõi công nợ</h1>
          <p className="mt-1 text-sm text-[#6b7280]">Các hóa đơn còn số dư cần thu từ khách hàng.</p>
        </div>
        <div className="flex items-center gap-2">
          <select value={status} onChange={(event) => setStatus(event.target.value)} className="rounded-lg border border-[#e5e7eb] bg-white px-3 py-2 text-sm">
            <option value="">Tất cả khoản cần thu</option>
            <option value="UNPAID">Chưa thu</option>
            <option value="PARTIALLY_PAID">Đã thu một phần</option>
            <option value="OVERDUE">Quá hạn</option>
          </select>
          <Button size="sm" variant="outline" onClick={load}>↻ Tải lại</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
        <div className="rounded-xl border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs text-[#6b7280]">Số hóa đơn cần thu</p>
          <p className="mt-1 text-2xl font-semibold text-[#111827]">{invoices.length}</p>
        </div>
        <div className="rounded-xl border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs text-[#6b7280]">Tổng công nợ còn lại</p>
          <p className="mt-1 text-2xl font-semibold text-[#b45309]">{formatVnd(totalOutstanding)}</p>
        </div>
        <div className="rounded-xl border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs text-[#6b7280]">Cần ưu tiên truy thu</p>
          <p className="mt-1 text-2xl font-semibold text-[#dc2626]">{invoices.filter((invoice) => invoice.paymentStatus === 'OVERDUE').length}</p>
        </div>
      </div>

      {loading ? <p className="py-12 text-center text-sm text-[#9ca3af]">Đang tải công nợ…</p> : (
        <DataTable columns={columns} rows={invoices} rowKey={(row) => row.id} empty="Không có hóa đơn nào cần truy thu." />
      )}
    </div>
  );
}
