'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import StatusPill from '@/components/ui/StatusPill';
import {
  listPayments,
  createPayment,
  updatePayment,
  deletePayment,
  getOrderOptions,
} from '@/lib/api';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import type { Payment } from '@/types';

const methodOptions = [
  { value: 'CASH', label: 'Tiền mặt' },
  { value: 'BANK_TRANSFER', label: 'Chuyển khoản' },
  { value: 'CARD', label: 'Thẻ' },
  { value: 'INSTALLMENT', label: 'Trả góp' },
];

const statusOptions = [
  { value: 'UNPAID', label: 'Chưa thanh toán' },
  { value: 'PARTIAL', label: 'Một phần' },
  { value: 'PAID', label: 'Đã thanh toán' },
  { value: 'REFUNDED', label: 'Đã hoàn tiền' },
];

export default function AdminPaymentsPage() {
  return (
    <ResourceManager<Payment>
      title="Thanh toán"
      description="Quản lý giao dịch thanh toán của đơn hàng."
      list={listPayments}
      create={createPayment}
      update={updatePayment}
      remove={deletePayment}
      getId={(row) => row.id}
      toForm={(row) => ({
        orderId: row.orderId,
        amount: row.amount,
        method: row.method,
        status: row.status,
      })}
      fields={[
        {
          name: 'orderId',
          label: 'Đơn hàng',
          type: 'select',
          required: true,
          hideOnEdit: true,
          placeholder: '— Chọn đơn hàng —',
          optionsLoader: () =>
            getOrderOptions('payment').then((orders) =>
              orders.map((o) => ({
                value: o.orderId,
                label: `${o.orderCode} — ${o.customerName ?? 'Khách lẻ'} — ${formatVnd(o.totalAmount)} · ${
                  o.remainingAmount > 0
                    ? `còn thiếu ${formatVnd(o.remainingAmount)}`
                    : 'đã thanh toán đủ'
                }`,
              })),
            ),
        },
        { name: 'amount', label: 'Số tiền (triệu)', type: 'number', required: true, step: '0.1' },
        { name: 'method', label: 'Phương thức', type: 'select', options: methodOptions, required: true },
        { name: 'status', label: 'Trạng thái', type: 'select', options: statusOptions, required: true },
      ]}
      columns={[
        { key: 'id', label: 'ID' },
        { key: 'orderCode', label: 'Mã đơn', render: (r) => r.orderCode ?? '—' },
        {
          key: 'amount',
          label: 'Số tiền',
          className: 'text-right',
          render: (r) => formatVnd(r.amount),
        },
        { key: 'method', label: 'Phương thức' },
        { key: 'status', label: 'Trạng thái', render: (r) => <StatusPill status={r.status} /> },
        {
          key: 'paidAt',
          label: 'Thời điểm',
          render: (r) => (r.paidAt ? formatDateTime(r.paidAt) : '—'),
        },
      ]}
    />
  );
}
