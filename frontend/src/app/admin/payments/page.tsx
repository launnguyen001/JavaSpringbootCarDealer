'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import {
  listPayments,
  createPayment,
} from '@/lib/api';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import type { Payment } from '@/types';
import { paymentFields } from '@/components/admin/paymentFields';

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
      fields={paymentFields}
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
