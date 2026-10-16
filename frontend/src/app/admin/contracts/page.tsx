'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import { listContracts, createContract, updateContract, deleteContract } from '@/lib/api';
import { formatDateTime } from '@/lib/utils/format';
import type { Contract } from '@/types';

export default function AdminContractsPage() {
  return (
    <ResourceManager<Contract>
      title="Hợp đồng"
      description="Quản lý hợp đồng mua bán."
      list={listContracts}
      create={createContract}
      update={updateContract}
      remove={deleteContract}
      getId={(row) => row.id}
      toForm={(row) => ({
        orderId: row.orderId,
        contractNumber: row.contractNumber,
        signedAt: row.signedAt ?? '',
        content: row.content ?? '',
      })}
      fields={[
        { name: 'orderId', label: 'ID đơn hàng', type: 'number', required: true },
        { name: 'contractNumber', label: 'Số hợp đồng', required: true },
        { name: 'signedAt', label: 'Ngày ký', type: 'datetime' },
        { name: 'content', label: 'Nội dung', type: 'textarea' },
      ]}
      columns={[
        { key: 'id', label: 'ID' },
        { key: 'contractNumber', label: 'Số hợp đồng' },
        { key: 'orderCode', label: 'Mã đơn', render: (r) => r.orderCode ?? '—' },
        {
          key: 'signedAt',
          label: 'Ngày ký',
          render: (r) => (r.signedAt ? formatDateTime(r.signedAt) : '—'),
        },
      ]}
    />
  );
}
