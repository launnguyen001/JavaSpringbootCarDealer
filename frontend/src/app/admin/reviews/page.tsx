'use client';

import { useEffect, useState } from 'react';
import ResourceManager, { type FieldConfig } from '@/components/admin/ResourceManager';
import {
  listCarReviews,
  createCarReview,
  updateCarReview,
  deleteCarReview,
  listCars,
  type CarReviewRequest,
} from '@/lib/api';
import { formatDateTime } from '@/lib/utils/format';
import type { Car, CarReview } from '@/types';

function toPayload(payload: Record<string, unknown>): CarReviewRequest {
  return {
    carId: Number(payload.carId),
    customerId: Number(payload.customerId),
    customerName: String(payload.customerName ?? ''),
    rating: Number(payload.rating),
    title: (payload.title as string) || null,
    content: String(payload.content ?? ''),
  };
}

export default function AdminReviewsPage() {
  const [cars, setCars] = useState<Car[]>([]);

  useEffect(() => {
    listCars().then(setCars).catch(() => setCars([]));
  }, []);

  const carOptions = cars.map((c) => ({ value: c.id, label: c.name }));

  const fields: FieldConfig[] = [
    {
      name: 'carId',
      label: 'Xe',
      type: 'select',
      required: true,
      placeholder: '— Chọn xe —',
      options: carOptions,
    },
    { name: 'customerId', label: 'ID khách hàng', type: 'number', required: true },
    { name: 'customerName', label: 'Tên khách hàng', required: true },
    { name: 'rating', label: 'Điểm (1-5)', type: 'number', required: true },
    { name: 'title', label: 'Tiêu đề' },
    { name: 'content', label: 'Nội dung', type: 'textarea', required: true },
  ];

  return (
    <ResourceManager<CarReview>
      title="Đánh giá"
      description="Quản lý đánh giá của khách hàng về xe."
      list={listCarReviews}
      create={(p) => createCarReview(toPayload(p))}
      update={(id, p) => updateCarReview(id, toPayload(p))}
      remove={deleteCarReview}
      getId={(row) => row.id}
      toForm={(row) => ({
        carId: row.carId,
        customerId: row.customerId,
        customerName: row.customerName,
        rating: row.rating,
        title: row.title ?? '',
        content: row.content,
      })}
      fields={fields}
      columns={[
        { key: 'carId', label: 'Xe', render: (r) => r.carName ?? `ID ${r.carId}` },
        { key: 'customerName', label: 'Khách hàng' },
        { key: 'rating', label: 'Điểm', className: 'text-center' },
        { key: 'title', label: 'Tiêu đề', render: (r) => r.title ?? '—' },
        {
          key: 'content',
          label: 'Nội dung',
          render: (r) => (
            <span className="line-clamp-1 max-w-xs inline-block align-middle">{r.content}</span>
          ),
        },
        { key: 'createdAt', label: 'Ngày', render: (r) => formatDateTime(r.createdAt) },
      ]}
    />
  );
}
