'use client';

import { useEffect, useState } from 'react';
import ResourceManager, { type FieldConfig } from '@/components/admin/ResourceManager';
import {
  listCarModels,
  createCarModel,
  updateCarModel,
  deleteCarModel,
  listBrands,
  searchCarModelsByName,
} from '@/lib/api';
import type { Brand, CarModel } from '@/types';

export default function AdminCarModelsPage() {
  const [brands, setBrands] = useState<Brand[]>([]);

  useEffect(() => {
    listBrands().then(setBrands).catch(() => setBrands([]));
  }, []);

  const fields: FieldConfig[] = [
    { name: 'name', label: 'Tên dòng xe', required: true },
    {
      name: 'brandId',
      label: 'Hãng xe',
      type: 'select',
      required: true,
      options: brands.map((b) => ({ value: b.id, label: b.name })),
    },
    { name: 'bodyType', label: 'Kiểu dáng' },
    { name: 'description', label: 'Mô tả', type: 'textarea' },
  ];

  return (
    <ResourceManager<CarModel>
      title="Dòng xe"
      description="Quản lý các dòng xe theo hãng."
      list={listCarModels}
      create={createCarModel}
      update={updateCarModel}
      remove={deleteCarModel}
      search={searchCarModelsByName}
      getId={(row) => row.id}
      toForm={(row) => ({
        name: row.name,
        brandId: row.brandId ?? '',
        bodyType: row.bodyType ?? '',
        description: row.description ?? '',
      })}
      fields={fields}
      columns={[
        { key: 'name', label: 'Dòng xe' },
        { key: 'brandName', label: 'Hãng', render: (r) => r.brandName ?? '—' },
        { key: 'bodyType', label: 'Kiểu dáng', render: (r) => r.bodyType ?? '—' },
      ]}
    />
  );
}
