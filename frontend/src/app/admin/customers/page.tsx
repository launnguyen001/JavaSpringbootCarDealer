'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import {
  listCustomers,
  createCustomer,
  updateCustomer,
  deleteCustomer,
  searchCustomersByName,
} from '@/lib/api';
import type { Customer } from '@/types';

export default function AdminCustomersPage() {
  return (
    <ResourceManager<Customer>
      title="Khách hàng"
      description="Quản lý hồ sơ khách hàng."
      list={listCustomers}
      create={createCustomer}
      update={updateCustomer}
      remove={deleteCustomer}
      search={searchCustomersByName}
      getId={(row) => row.id}
      toForm={(row) => ({
        fullName: row.fullName,
        email: row.email ?? '',
        phone: row.phone ?? '',
        address: row.address ?? '',
        nationalId: row.nationalId ?? '',
      })}
      fields={[
        { name: 'fullName', label: 'Họ và tên', required: true },
        { name: 'phone', label: 'Số điện thoại' },
        { name: 'email', label: 'Email' },
        { name: 'nationalId', label: 'CMND/CCCD' },
        { name: 'address', label: 'Địa chỉ', type: 'textarea' },
      ]}
      columns={[
        { key: 'fullName', label: 'Họ và tên' },
        { key: 'phone', label: 'Điện thoại', render: (r) => r.phone ?? '—' },
        { key: 'email', label: 'Email', render: (r) => r.email ?? '—' },
        { key: 'nationalId', label: 'CMND/CCCD', render: (r) => r.nationalId ?? '—' },
      ]}
    />
  );
}
