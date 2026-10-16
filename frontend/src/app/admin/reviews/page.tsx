'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import {
  listCarReviews,
  createCarReview,
  updateCarReview,
  deleteCarReview,
} from '@/lib/api';
import { formatDateTime } from '@/lib/utils/format';
import type { CarReview } from '@/types';

export default function AdminReviewsPage() {
  return (
    <ResourceManager<CarReview>
      title="Đánh giá"
      description="Quản lý đánh giá của khách hàng về xe."
      list={listCarReviews}
      create={createCarReview}
      update={updateCarReview}
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
      fields={[
        { name: 'carId', label: 'ID xe', type: 'number', required: true },
        { name: 'customerId', label: 'ID khách hàng', type: 'number', required: true },
        { name: 'customerName', label: 'Tên khách hàng', required: true },
        { name: 'rating', label: 'Điểm (1-5)', type: 'number', required: true },
        { name: 'title', label: 'Tiêu đề' },
        { name: 'content', label: 'Nội dung', type: 'textarea', required: true },
      ]}
      columns={[
        { key: 'carId', label: 'ID xe' },
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
