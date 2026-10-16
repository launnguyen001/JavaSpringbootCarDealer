'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import ResourceManager, { type FieldConfig } from '@/components/admin/ResourceManager';
import StatusPill from '@/components/ui/StatusPill';
import { listCars, createCar, updateCar, deleteCar, listCarModels } from '@/lib/api';
import { formatVnd } from '@/lib/utils/format';
import type { Car, CarModel } from '@/types';

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Cũ' };

export default function AdminCarsPage() {
  const [models, setModels] = useState<CarModel[]>([]);

  useEffect(() => {
    listCarModels().then(setModels).catch(() => setModels([]));
  }, []);

  // Sau khi tách bảng: tên dòng xe + mô tả + giá niêm yết; từng chiếc quản lý ở "Xe trong kho".
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
    { name: 'basePrice', label: 'Giá niêm yết (triệu)', type: 'number', required: true, step: '0.1' },
    { name: 'description', label: 'Mô tả', type: 'textarea' },
  ];

  return (
    <ResourceManager<Car>
      title="Xe"
      description="Quản lý dòng xe (sản phẩm). Từng chiếc cụ thể nằm ở mục “Xe trong kho”."
      list={listCars}
      create={createCar}
      update={updateCar}
      remove={deleteCar}
      getId={(row) => row.id}
      toForm={(row) => ({
        name: row.name,
        modelId: row.modelId ?? '',
        basePrice: row.basePrice,
        description: row.description ?? '',
      })}
      fields={fields}
      columns={[
        { key: 'name', label: 'Tên xe' },
        { key: 'modelName', label: 'Dòng xe', render: (r) => r.modelName ?? '—' },
        { key: 'brandName', label: 'Hãng', render: (r) => r.brandName ?? '—' },
        {
          key: 'priceFrom',
          label: 'Giá',
          className: 'text-right',
          render: (r) => {
            const from = r.priceFrom ?? r.basePrice;
            const to = r.priceTo ?? r.priceFrom ?? r.basePrice;
            return from !== to ? `${formatVnd(from)} – ${formatVnd(to)}` : formatVnd(from);
          },
        },
        {
          key: 'totalItems',
          label: 'Tồn',
          className: 'text-center',
          render: (r) => `${r.availableItems}/${r.totalItems}`,
        },
        {
          key: 'conditions',
          label: 'Tình trạng',
          render: (r) =>
            r.conditions.length > 0
              ? r.conditions.map((c) => CONDITION_LABEL[c] ?? c).join(' • ')
              : '—',
        },
        {
          key: 'status',
          label: 'Trạng thái',
          render: (r) => <StatusPill status={r.status} />,
        },
        {
          key: 'id',
          label: '',
          className: 'text-right',
          render: (r) => (
            <Link href={`/admin/car-items?carId=${r.id}`} className="text-[#c58e00] hover:underline text-xs">
              Chi tiết xe
            </Link>
          ),
        },
      ]}
    />
  );
}
