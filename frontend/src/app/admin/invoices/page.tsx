'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';
import ResourceManager from '@/components/admin/ResourceManager';
import Button from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import {
  listInvoices,
  getInvoiceByVin,
  createInvoice,
  updateInvoice,
  deleteInvoice,
  getOrderOptions,
  downloadFile,
} from '@/lib/api';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import StatusPill from '@/components/ui/StatusPill';
import type { Invoice } from '@/types';

export default function AdminInvoicesPage() {
  const toast = useToast();
  const [vin, setVin] = useState('');
  const [vinInvoice, setVinInvoice] = useState<Invoice | null>(null);
  const [vinError, setVinError] = useState('');
  const [vinLoading, setVinLoading] = useState(false);

  const searchByVin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!vin.trim()) {
      setVinError('Vui lòng nhập mã VIN.');
      setVinInvoice(null);
      return;
    }
    setVinLoading(true);
    setVinError('');
    setVinInvoice(null);
    try {
      setVinInvoice(await getInvoiceByVin(vin.trim()));
    } catch (err) {
      setVinError(getErrorMessage(err));
    } finally {
      setVinLoading(false);
    }
  };

  const downloadPdf = async (invoice: Invoice) => {
    try {
      await downloadFile(`/invoices/${invoice.id}/pdf`, `hoa-don-${invoice.id}.pdf`);
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-[#e5e7eb] bg-white p-4">
        <h2 className="font-semibold text-[#111827]">Tra cứu hóa đơn theo VIN</h2>
        <form onSubmit={searchByVin} className="mt-3 flex flex-col gap-2 sm:flex-row">
          <input value={vin} onChange={(event) => setVin(event.target.value)} placeholder="Nhập mã VIN của xe" className="min-w-0 flex-1 rounded-lg border border-[#d1d5db] px-3 py-2 text-sm outline-none focus:border-[#c58e00]" />
          <button type="submit" disabled={vinLoading} className="rounded-lg bg-[#c58e00] px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">{vinLoading ? 'Đang tra cứu…' : 'Tra cứu'}</button>
        </form>
        {vinError && <p className="mt-2 text-sm text-red-600">{vinError}</p>}
        {vinInvoice && (
          <div className="mt-3 grid gap-3 rounded-xl bg-[#fafafa] p-4 sm:grid-cols-3">
            <div><p className="text-xs text-[#6b7280]">Số hóa đơn</p><p className="font-semibold">{vinInvoice.invoiceNumber}</p></div>
            <div><p className="text-xs text-[#6b7280]">Mã đơn</p><p className="font-semibold">{vinInvoice.orderCode}</p></div>
            <div><p className="text-xs text-[#6b7280]">Khách hàng</p><p className="font-semibold">{vinInvoice.customerName ?? '—'}</p></div>
            <div><p className="text-xs text-[#6b7280]">Tổng hóa đơn</p><p className="font-semibold">{formatVnd(vinInvoice.total)}</p></div>
            <div><p className="text-xs text-[#6b7280]">Đã thu / còn nợ</p><p className="font-semibold">{formatVnd(vinInvoice.paidAmount)} / {formatVnd(vinInvoice.remainingAmount)}</p></div>
            <div className="flex items-center gap-3"><StatusPill status={vinInvoice.paymentStatus}/><Link className="text-sm font-medium text-[#a87900] hover:underline" href={`/admin/orders?orderId=${vinInvoice.orderId}`}>Mở đơn hàng →</Link></div>
          </div>
        )}
      </section>
      <div className="flex justify-end"><Link href="/admin/collections" className="rounded-lg border border-[#e5e7eb] px-4 py-2 text-sm font-medium text-[#374151] hover:bg-[#fafafa]">Danh sách cần thu →</Link></div>
      <ResourceManager<Invoice>
      title="Hóa đơn"
      description="Quản lý hóa đơn bán hàng và tải bản PDF."
      list={listInvoices}
      create={createInvoice}
      update={updateInvoice}
      remove={deleteInvoice}
      rowActions={(row) => (
        <Button size="sm" variant="ghost" onClick={() => downloadPdf(row)}>
          PDF
        </Button>
      )}
      getId={(row) => row.id}
      toForm={(row) => ({
        orderId: row.orderId,
        invoiceNumber: row.invoiceNumber,
        total: row.total,
        issuedAt: row.issuedAt ?? '',
        dueDate: row.dueDate ?? '',
      })}
      fields={[
        {
          name: 'orderId',
          label: 'Đơn hàng',
          type: 'select',
          required: true,
          hideOnEdit: true,
          placeholder: '— Chọn đơn chưa có hóa đơn —',
          optionsLoader: () =>
            getOrderOptions('invoice').then((orders) =>
              orders.map((o) => ({
                value: o.orderId,
                label: `${o.orderCode} — ${o.customerName ?? 'Khách lẻ'} — ${formatVnd(o.totalAmount)}`,
                meta: { totalAmount: o.totalAmount },
              })),
            ),
          onPick: (option) => ({ total: String(option.meta?.totalAmount ?? '') }),
        },
        { name: 'invoiceNumber', label: 'Số hóa đơn', required: true },
        { name: 'total', label: 'Tổng tiền (triệu)', type: 'number', required: true, step: '0.1' },
        { name: 'issuedAt', label: 'Ngày phát hành', type: 'datetime' },
        { name: 'dueDate', label: 'Hạn thanh toán', type: 'date' },
      ]}
      columns={[
        { key: 'id', label: 'ID' },
        { key: 'invoiceNumber', label: 'Số hóa đơn' },
        { key: 'orderCode', label: 'Mã đơn', render: (r) => r.orderCode ?? '—' },
        {
          key: 'total',
          label: 'Tổng tiền',
          className: 'text-right',
          render: (r) => formatVnd(r.total),
        },
        { key: 'paidAmount', label: 'Đã thu', className: 'text-right', render: (r) => formatVnd(r.paidAmount) },
        { key: 'remainingAmount', label: 'Còn nợ', className: 'text-right', render: (r) => formatVnd(r.remainingAmount) },
        { key: 'paymentStatus', label: 'Thu tiền', render: (r) => <StatusPill status={r.paymentStatus} /> },
        {
          key: 'issuedAt',
          label: 'Ngày phát hành',
          render: (r) => (r.issuedAt ? formatDateTime(r.issuedAt) : '—'),
        },
        { key: 'dueDate', label: 'Hạn thanh toán', render: (r) => r.dueDate ?? '—' },
      ]}
      />
    </div>
  );
}
