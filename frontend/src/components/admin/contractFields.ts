import { getOrderOptions } from '@/lib/api';
import { formatVnd } from '@/lib/utils/format';
import type { FieldConfig } from '@/components/admin/ResourceManager';

export const contractFields: FieldConfig[] = [
  {
    name: 'orderId',
    label: 'Đơn hàng',
    type: 'select',
    required: true,
    hideOnEdit: true,
    placeholder: '— Chọn đơn chưa có hợp đồng —',
    optionsLoader: () =>
      getOrderOptions('contract').then((orders) =>
        orders.map((o) => ({
          value: o.orderId,
          label: `${o.orderCode} — ${o.customerName ?? 'Khách lẻ'} — ${formatVnd(o.totalAmount)}`,
        })),
      ),
  },
  { name: 'contractNumber', label: 'Số hợp đồng', required: true },
  { name: 'signedAt', label: 'Ngày ký', type: 'datetime' },
  { name: 'content', label: 'Nội dung', type: 'textarea' },
];
