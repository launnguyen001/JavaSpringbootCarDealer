import { getOrderOptions } from '@/lib/api';
import { formatVnd } from '@/lib/utils/format';
import type { FieldConfig } from '@/components/admin/ResourceManager';

export const paymentMethodOptions = [
  { value: 'CASH', label: 'Tiền mặt' },
  { value: 'BANK_TRANSFER', label: 'Chuyển khoản' },
  { value: 'CARD', label: 'Thẻ' },
  { value: 'INSTALLMENT', label: 'Trả góp' },
];

export const paymentFields: FieldConfig[] = [
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
            o.remainingAmount > 0 ? `còn thiếu ${formatVnd(o.remainingAmount)}` : 'đã thanh toán đủ'
          }`,
        })),
      ),
  },
  { name: 'amount', label: 'Số tiền (triệu)', type: 'number', required: true, step: '0.1' },
  { name: 'method', label: 'Phương thức', type: 'select', options: paymentMethodOptions, required: true },
];
