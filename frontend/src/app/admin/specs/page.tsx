'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import {
  listCarSpecs,
  upsertCarSpecs,
  deleteCarSpecs,
  type CarSpecificationRequest,
} from '@/lib/api';
import { formatDateTime } from '@/lib/utils/format';
import type { CarSpecification } from '@/types';

function toArray(value: unknown): string[] {
  if (Array.isArray(value)) return value as string[];
  if (typeof value === 'string') {
    return value
      .split(/[\n,]/)
      .map((s) => s.trim())
      .filter(Boolean);
  }
  return [];
}

function num(value: unknown): number | null {
  if (value === '' || value === null || value === undefined) return null;
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
}

function toPayload(payload: Record<string, unknown>): CarSpecificationRequest {
  return {
    carId: Number(payload.carId),
    engine: (payload.engine as string) || null,
    transmission: (payload.transmission as string) || null,
    fuelType: (payload.fuelType as string) || null,
    seats: num(payload.seats),
    dimensions: (payload.dimensions as string) || null,
    weightKg: num(payload.weightKg),
    safetyFeatures: toArray(payload.safetyFeatures),
    extra: {},
  };
}

export default function AdminSpecsPage() {
  return (
    <ResourceManager<CarSpecification>
      title="Thông số xe"
      description="Thông số kỹ thuật của xe (lưu MongoDB)."
      list={listCarSpecs}
      create={(p) => upsertCarSpecs(toPayload(p))}
      update={(_id, p) => upsertCarSpecs(toPayload(p))}
      remove={deleteCarSpecs}
      getId={(row) => row.carId}
      toForm={(row) => ({
        carId: row.carId,
        engine: row.engine ?? '',
        transmission: row.transmission ?? '',
        fuelType: row.fuelType ?? '',
        seats: row.seats ?? '',
        dimensions: row.dimensions ?? '',
        weightKg: row.weightKg ?? '',
        safetyFeatures: (row.safetyFeatures ?? []).join(', '),
      })}
      fields={[
        { name: 'carId', label: 'ID xe', type: 'number', required: true },
        { name: 'engine', label: 'Động cơ' },
        { name: 'transmission', label: 'Hộp số' },
        { name: 'fuelType', label: 'Nhiên liệu' },
        { name: 'seats', label: 'Số chỗ', type: 'number' },
        { name: 'dimensions', label: 'Kích thước' },
        { name: 'weightKg', label: 'Cân nặng (kg)', type: 'number' },
        { name: 'safetyFeatures', label: 'Tính năng an toàn (phân cách bởi dấu phẩy)' },
      ]}
      columns={[
        { key: 'carId', label: 'ID xe' },
        { key: 'engine', label: 'Động cơ', render: (r) => r.engine ?? '—' },
        { key: 'transmission', label: 'Hộp số', render: (r) => r.transmission ?? '—' },
        { key: 'fuelType', label: 'Nhiên liệu', render: (r) => r.fuelType ?? '—' },
        { key: 'seats', label: 'Số chỗ', className: 'text-center', render: (r) => r.seats ?? '—' },
        {
          key: 'updatedAt',
          label: 'Cập nhật',
          render: (r) => (r.updatedAt ? formatDateTime(r.updatedAt) : '—'),
        },
      ]}
    />
  );
}
