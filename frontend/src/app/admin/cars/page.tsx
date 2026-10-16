'use client';

import { useEffect, useState } from 'react';
import ResourceManager, { type FieldConfig } from '@/components/admin/ResourceManager';
import StatusPill from '@/components/ui/StatusPill';
import {
  listCars,
  createCar,
  updateCar,
  deleteCar,
  listCarModels,
} from '@/lib/api';
import { formatVnd } from '@/lib/utils/format';
import type { Car, CarModel } from '@/types';

const statusOptions = [
  { value: 'AVAILABLE', label: 'Sẵn sàng' },
  { value: 'RESERVED', label: 'Đã đặt' },
  { value: 'SOLD', label: 'Đã bán' },
  { value: 'MAINTENANCE', label: 'Bảo trì' },
];

export default function AdminCarsPage() {
  const [models, setModels] = useState<CarModel[]>([]);

  useEffect(() => {
    listCarModels().then(setModels).catch(() => setModels([]));
  }, []);

  const fields: FieldConfig[] = [
    { name: 'name', label: 'Tên xe', required: true },
    {
      name: 'modelId',
      label: 'Dòng xe',
      type: 'select',
      required: true,
      options: models.map((m) => ({
        value: m.id,
        label: m.brandName ? `${m.name} (${m.brandName})` : m.name,
      })),
    },
    { name: 'price', label: 'Giá (triệu)', type: 'number', required: true, step: '0.1' },
    { name: 'quantity', label: 'Số lượng', type: 'number', required: true },
    { name: 'vin', label: 'VIN' },
    { name: 'year', label: 'Năm sản xuất', type: 'number' },
    { name: 'color', label: 'Màu sắc' },
    { name: 'status', label: 'Trạng thái', type: 'select', options: statusOptions, required: true },
    { name: 'description', label: 'Mô tả', type: 'textarea' },
  ];

  return (
    <ResourceManager<Car>
      title="Xe"
      description="Quản lý danh mục xe trong hệ thống."
      list={listCars}
      create={createCar}
      update={updateCar}
      remove={deleteCar}
      getId={(row) => row.id}
      toForm={(row) => ({
        name: row.name,
        modelId: row.modelId ?? '',
        price: row.price,
        quantity: row.quantity,
        vin: row.vin ?? '',
        year: row.year ?? '',
        color: row.color ?? '',
        status: row.status,
        description: row.description ?? '',
      })}
      fields={fields}
      columns={[
        { key: 'name', label: 'Tên xe' },
        {
          key: 'modelName',
          label: 'Dòng xe',
          render: (r) => r.modelName ?? '—',
        },
        {
          key: 'brandName',
          label: 'Hãng',
          render: (r) => r.brandName ?? '—',
        },
        {
          key: 'price',
          label: 'Giá',
          className: 'text-right',
          render: (r) => formatVnd(r.price),
        },
        { key: 'quantity', label: 'Tồn', className: 'text-center' },
        {
          key: 'status',
          label: 'Trạng thái',
          render: (r) => <StatusPill status={r.status} />,
        },
      ]}
    />
  );
}
