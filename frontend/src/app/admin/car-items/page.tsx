'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import ResourceManager, { type FieldConfig } from '@/components/admin/ResourceManager';
import StatusPill from '@/components/ui/StatusPill';
import { Field, Select } from '@/components/ui/Field';
import {
  listCars,
  listCarItems,
  createCarItem,
  updateCarItem,
  deleteCarItem,
} from '@/lib/api';
import { formatVnd } from '@/lib/utils/format';
import type { Car, CarItem } from '@/types';

const conditionOptions = [
  { value: 'NEW', label: 'Mới' },
  { value: 'USED', label: 'Cũ' },
];

const statusOptions = [
  { value: 'AVAILABLE', label: 'Sẵn sàng' },
  { value: 'RESERVED', label: 'Đã đặt' },
  { value: 'SOLD', label: 'Đã bán' },
  { value: 'MAINTENANCE', label: 'Bảo trì' },
];

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Cũ' };

export default function AdminCarItemsPage() {
  const router = useRouter();
  const [cars, setCars] = useState<Car[]>([]);
  const [carId, setCarId] = useState<number | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    listCars()
      .then((data) => {
        setCars(data);
        // Ưu tiên dòng xe trên query string (?carId=…), nếu không có thì lấy dòng đầu.
        const q = Number(new URLSearchParams(window.location.search).get('carId'));
        const initial =
          Number.isFinite(q) && q > 0 && data.some((c) => c.id === q) ? q : (data[0]?.id ?? null);
        setCarId(initial);
      })
      .catch(() => setCars([]))
      .finally(() => setReady(true));
  }, []);

  const fields: FieldConfig[] = [
    { name: 'vin', label: 'VIN', placeholder: 'Mã khung (duy nhất)' },
    { name: 'color', label: 'Màu sắc' },
    { name: 'year', label: 'Năm sản xuất', type: 'number' },
    { name: 'condition', label: 'Tình trạng', type: 'select', options: conditionOptions, required: true },
    { name: 'price', label: 'Giá (triệu)', type: 'number', required: true, step: '0.1' },
    { name: 'status', label: 'Trạng thái', type: 'select', options: statusOptions, required: true },
    { name: 'mileageKm', label: 'Số km đã đi', type: 'number' },
    { name: 'note', label: 'Ghi chú', type: 'textarea' },
  ];

  if (!ready) return null;

  if (!carId) {
    return (
      <div>
        <h1 className="text-2xl font-serif font-bold text-[#0a0a0a]">Xe trong kho</h1>
        <p className="text-sm text-[#6b7280] mt-2">
          Chưa có dòng xe nào. Hãy tạo dòng xe ở mục “Xe” trước.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="max-w-md">
        <Field label="Chọn dòng xe">
          <Select value={carId} onChange={(e) => setCarId(Number(e.target.value))}>
            {cars.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
                {c.brandName ? ` (${c.brandName})` : ''} • {c.availableItems}/{c.totalItems} sẵn sàng
              </option>
            ))}
          </Select>
        </Field>
      </div>

      {/* key={carId} buộc ResourceManager tải lại khi đổi dòng xe */}
      <ResourceManager<CarItem>
        key={carId}
        title="Xe trong kho"
        description="Quản lý từng chiếc xe vật lý: VIN, màu, năm, tình trạng, giá, trạng thái."
        list={() => listCarItems(carId)}
        create={(payload) => createCarItem(carId, payload)}
        update={updateCarItem}
        remove={deleteCarItem}
        getId={(row) => row.id}
        onRowClick={(row) => router.push(`/admin/car-items/${row.id}`)}
        toForm={(row) => ({
          vin: row.vin ?? '',
          color: row.color ?? '',
          year: row.year ?? '',
          condition: row.condition,
          price: row.price,
          status: row.status,
          mileageKm: row.mileageKm ?? '',
          note: row.note ?? '',
        })}
        fields={fields}
        columns={[
          { key: 'vin', label: 'VIN', render: (r) => r.vin ?? '—' },
          { key: 'color', label: 'Màu', render: (r) => r.color ?? '—' },
          { key: 'year', label: 'Năm', className: 'text-center', render: (r) => r.year ?? '—' },
          {
            key: 'condition',
            label: 'Tình trạng',
            render: (r) => CONDITION_LABEL[r.condition] ?? r.condition,
          },
          {
            key: 'price',
            label: 'Giá',
            className: 'text-right',
            render: (r) => formatVnd(r.price),
          },
          {
            key: 'mileageKm',
            label: 'Km',
            className: 'text-right',
            render: (r) => (r.mileageKm != null ? r.mileageKm.toLocaleString('vi-VN') : '—'),
          },
          {
            key: 'status',
            label: 'Trạng thái',
            render: (r) => <StatusPill status={r.status} />,
          },
        ]}
      />
    </div>
  );
}
