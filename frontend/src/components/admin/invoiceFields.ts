import { getOrderOptions } from '@/lib/api';
import { formatVnd } from '@/lib/utils/format';
import type { FieldConfig } from '@/components/admin/ResourceManager';

export const invoiceFields: FieldConfig[] = [
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
];
