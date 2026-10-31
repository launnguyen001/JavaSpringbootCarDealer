'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import {
  listPayments,
  createPayment,
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

export default function AdminPaymentsPage() {
  return (
    <ResourceManager<Payment>
      title="Thanh toán"
      description="Ghi nhận các khoản tiền thực thu. Giao dịch đã ghi nhận không thể sửa hoặc xóa; công nợ được tính từ tổng tiền đã thu."
      list={listPayments}
      create={createPayment}
      canEdit={false}
      canDelete={false}
      getId={(row) => row.id}
      toForm={(row) => ({
        orderId: row.orderId,
        amount: row.amount,
        method: row.method,
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
        {
          key: 'paidAt',
          label: 'Thời điểm',
          render: (r) => (r.paidAt ? formatDateTime(r.paidAt) : '—'),
        },
      ]}
    />
  );
}
