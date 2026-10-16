'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import { listBrands, createBrand, updateBrand, deleteBrand } from '@/lib/api';
import type { Brand } from '@/types';

export default function AdminBrandsPage() {
  return (
    <ResourceManager<Brand>
      title="Hãng xe"
      description="Quản lý các hãng xe (ADMIN)."
      list={listBrands}
      create={createBrand}
      update={updateBrand}
      remove={deleteBrand}
      getId={(row) => row.id}
      toForm={(row) => ({
        name: row.name,
        country: row.country ?? '',
        description: row.description ?? '',
      })}
      fields={[
        { name: 'name', label: 'Tên hãng', required: true },
        { name: 'country', label: 'Quốc gia' },
        { name: 'description', label: 'Mô tả', type: 'textarea' },
      ]}
      columns={[
        { key: 'name', label: 'Tên hãng' },
        { key: 'country', label: 'Quốc gia', render: (r) => r.country ?? '—' },
        { key: 'description', label: 'Mô tả', render: (r) => r.description ?? '—' },
      ]}
    />
  );
}
