'use client';

import { useEffect, useState } from 'react';
import Button from '@/components/ui/Button';
import ResourceManager, { type FieldConfig } from '@/components/admin/ResourceManager';
import ReviewReplyModal from '@/components/admin/ReviewReplyModal';
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
  const [replyReview, setReplyReview] = useState<CarReview | null>(null);
  const [replyModalOpen, setReplyModalOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

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
    <>
      <ResourceManager<CarReview>
        key={refreshKey}
        title="Đánh giá"
        description="Quản lý đánh giá và phản hồi của khách hàng về xe."
        list={listCarReviews}
        create={(p) => createCarReview(toPayload(p))}
        update={(id, p) => updateCarReview(id, toPayload(p))}
        remove={deleteCarReview}
        getId={(row) => row.id}
        rowActions={(row) => (
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setReplyReview(row);
              setReplyModalOpen(true);
            }}
            title="Xem và gửi phản hồi"
          >
            Phản hồi {(row.replies?.length ?? 0) > 0 ? `(${row.replies.length})` : ''}
          </Button>
        )}
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
          {
            key: 'replies',
            label: 'Phản hồi',
            className: 'text-center whitespace-nowrap',
            render: (r) =>
              (r.replies?.length ?? 0) > 0 ? (
                <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200">
                  {r.replies.length} phản hồi
                </span>
              ) : (
                <span className="text-xs text-[#9ca3af]">Chưa có</span>
              ),
          },
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

      <ReviewReplyModal
        open={replyModalOpen}
        review={replyReview}
        onClose={() => setReplyModalOpen(false)}
        onSuccess={(updated) => {
          setReplyReview(updated);
          setRefreshKey((k) => k + 1);
        }}
      />
    </>
  );
}
