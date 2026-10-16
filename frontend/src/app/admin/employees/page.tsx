'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import {
  listEmployees,
  createEmployee,
  updateEmployee,
  deleteEmployee,
  searchEmployeesByName,
} from '@/lib/api';
import type { Employee } from '@/types';

export default function AdminEmployeesPage() {
  return (
    <ResourceManager<Employee>
      title="Nhân viên"
      description="Quản lý nhân viên (ADMIN)."
      list={listEmployees}
      create={createEmployee}
      update={updateEmployee}
      remove={deleteEmployee}
      search={searchEmployeesByName}
      getId={(row) => row.id}
      toForm={(row) => ({
        fullName: row.fullName,
        email: row.email ?? '',
        phone: row.phone ?? '',
        position: row.position ?? '',
      })}
      fields={[
        { name: 'fullName', label: 'Họ và tên', required: true },
        { name: 'position', label: 'Chức vụ' },
        { name: 'phone', label: 'Số điện thoại' },
        { name: 'email', label: 'Email' },
      ]}
      columns={[
        { key: 'fullName', label: 'Họ và tên' },
        { key: 'position', label: 'Chức vụ', render: (r) => r.position ?? '—' },
        { key: 'phone', label: 'Điện thoại', render: (r) => r.phone ?? '—' },
        { key: 'email', label: 'Email', render: (r) => r.email ?? '—' },
      ]}
    />
  );
}
