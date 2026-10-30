'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import Button from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import {
  listInvoices,
  createInvoice,
  updateInvoice,
  deleteInvoice,
  getOrderOptions,
  downloadFile,
} from '@/lib/api';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import type { Invoice } from '@/types';

export default function AdminInvoicesPage() {
  const toast = useToast();

  const downloadPdf = async (invoice: Invoice) => {
    try {
      await downloadFile(`/invoices/${invoice.id}/pdf`, `hoa-don-${invoice.id}.pdf`);
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  return (
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
        {
          key: 'issuedAt',
          label: 'Ngày phát hành',
          render: (r) => (r.issuedAt ? formatDateTime(r.issuedAt) : '—'),
        },
      ]}
    />
  );
}
