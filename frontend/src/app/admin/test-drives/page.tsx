'use client';

import { useEffect, useState } from 'react';
import ResourceManager, { type FieldConfig } from '@/components/admin/ResourceManager';
import StatusPill from '@/components/ui/StatusPill';
import {
  listTestDrives,
  createTestDrive,
  updateTestDrive,
  deleteTestDrive,
  listCars,
  listCarItems,
} from '@/lib/api';
import { formatDateTime } from '@/lib/utils/format';
import type { CarItem, TestDrive } from '@/types';

const statusOptions = [
  { value: 'SCHEDULED', label: 'Đã lên lịch' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'COMPLETED', label: 'Hoàn tất' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];

export default function AdminTestDrivesPage() {
  // Danh sách tất cả chiếc xe để chọn khi tạo/sửa lịch lái thử.
  const [carItemOptions, setCarItemOptions] = useState<
    { value: number; label: string }[]
  >([]);

  useEffect(() => {
    listCars()
      .then(async (cars) => {
        const lists = await Promise.all(cars.map((c) => listCarItems(c.id).catch(() => [])));
        const flat: CarItem[] = lists.flat();
        setCarItemOptions(
          flat.map((it) => ({
            value: it.id,
            label: `${it.carName} • ${it.vin ?? `#${it.id}`}`,
          })),
        );
      })
      .catch(() => setCarItemOptions([]));
  }, []);

  const fields: FieldConfig[] = [
    { name: 'scheduledAt', label: 'Thời gian hẹn', type: 'datetime', required: true },
    { name: 'status', label: 'Trạng thái', type: 'select', options: statusOptions },
    { name: 'customerId', label: 'ID khách hàng', type: 'number', required: true },
    {
      name: 'carItemId',
      label: 'Xe (chiếc cụ thể)',
      type: 'select',
      required: true,
      options: carItemOptions,
    },
    { name: 'employeeId', label: 'ID nhân viên phụ trách', type: 'number' },
    { name: 'note', label: 'Ghi chú', type: 'textarea' },
  ];

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
        carItemId: row.carItemId,
        employeeId: row.employeeId ?? '',
        note: row.note ?? '',
      })}
      fields={fields}
      columns={[
        { key: 'id', label: 'ID' },
        {
          key: 'scheduledAt',
          label: 'Thời gian',
          render: (r) => formatDateTime(r.scheduledAt),
        },
        { key: 'customerName', label: 'Khách hàng' },
        {
          key: 'carName',
          label: 'Xe',
          render: (r) => (
            <span>
              {r.carName}
              {r.vin ? ` • ${r.vin}` : ''}
            </span>
          ),
        },
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
