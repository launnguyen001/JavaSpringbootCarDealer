'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import {
  listCarMedia,
  upsertCarMedia,
  deleteCarMedia,
  type CarMediaRequest,
} from '@/lib/api';
import type { CarMedia } from '@/types';

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

function toPayload(payload: Record<string, unknown>): CarMediaRequest {
  return {
    carId: Number(payload.carId),
    thumbnail: (payload.thumbnail as string) || null,
    images: toArray(payload.images),
    videos: toArray(payload.videos),
  };
}

export default function AdminMediaPage() {
  return (
    <ResourceManager<CarMedia>
      title="Media xe"
      description="Hình ảnh & video của xe (lưu MongoDB)."
      list={listCarMedia}
      create={(p) => upsertCarMedia(toPayload(p))}
      update={(_id, p) => upsertCarMedia(toPayload(p))}
      remove={deleteCarMedia}
      getId={(row) => row.carId}
      toForm={(row) => ({
        carId: row.carId,
        thumbnail: row.thumbnail ?? '',
        images: (row.images ?? []).join('\n'),
        videos: (row.videos ?? []).join('\n'),
      })}
      fields={[
        { name: 'carId', label: 'ID xe', type: 'number', required: true },
        { name: 'thumbnail', label: 'Ảnh đại diện (URL)' },
        { name: 'images', label: 'Danh sách ảnh (mỗi dòng 1 URL)', type: 'textarea' },
        { name: 'videos', label: 'Danh sách video (mỗi dòng 1 URL)', type: 'textarea' },
      ]}
      columns={[
        { key: 'carId', label: 'ID xe' },
        {
          key: 'thumbnail',
          label: 'Ảnh đại diện',
          render: (r) =>
            r.thumbnail ? (
              <a
                href={r.thumbnail}
                target="_blank"
                rel="noreferrer"
                title="Bấm để xem ảnh đầy đủ"
                className="inline-block rounded-lg"
              >
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={r.thumbnail}
                  alt=""
                  className="w-14 h-10 object-cover rounded-lg ring-1 ring-[#e5e7eb] transition hover:ring-2 hover:ring-[#c58e00]"
                />
              </a>
            ) : (
              '—'
            ),
        },
        {
          key: 'images',
          label: 'Số ảnh',
          className: 'text-center',
          render: (r) => r.images?.length ?? 0,
        },
        {
          key: 'videos',
          label: 'Số video',
          className: 'text-center',
          render: (r) => r.videos?.length ?? 0,
        },
      ]}
    />
  );
}
