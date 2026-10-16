'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import StatusPill from '@/components/ui/StatusPill';
import {
  listTestDrives,
  createTestDrive,
  updateTestDrive,
  deleteTestDrive,
} from '@/lib/api';
import { formatDateTime } from '@/lib/utils/format';
import type { TestDrive } from '@/types';

const statusOptions = [
  { value: 'SCHEDULED', label: 'Đã lên lịch' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'COMPLETED', label: 'Hoàn tất' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];

export default function AdminTestDrivesPage() {
  return (
    <ResourceManager<TestDrive>
      title="Lịch lái thử"
      description="Quản lý lịch hẹn lái thử của khách hàng."
      list={listTestDrives}
      create={createTestDrive}
      update={updateTestDrive}
      remove={deleteTestDrive}
      getId={(row) => row.id}
      toForm={(row) => ({
        scheduledAt: row.scheduledAt,
        status: row.status,
        customerId: row.customerId,
        carId: row.carId,
        employeeId: row.employeeId ?? '',
        note: row.note ?? '',
      })}
      fields={[
        { name: 'scheduledAt', label: 'Thời gian hẹn', type: 'datetime', required: true },
        { name: 'status', label: 'Trạng thái', type: 'select', options: statusOptions },
        { name: 'customerId', label: 'ID khách hàng', type: 'number', required: true },
        { name: 'carId', label: 'ID xe', type: 'number', required: true },
        { name: 'employeeId', label: 'ID nhân viên phụ trách', type: 'number' },
        { name: 'note', label: 'Ghi chú', type: 'textarea' },
      ]}
      columns={[
        { key: 'id', label: 'ID' },
        {
          key: 'scheduledAt',
          label: 'Thời gian',
          render: (r) => formatDateTime(r.scheduledAt),
        },
        { key: 'customerName', label: 'Khách hàng' },
        { key: 'carName', label: 'Xe' },
        {
          key: 'employeeName',
          label: 'Nhân viên',
          render: (r) => r.employeeName ?? '—',
        },
        { key: 'status', label: 'Trạng thái', render: (r) => <StatusPill status={r.status} /> },
      ]}
    />
  );
}
