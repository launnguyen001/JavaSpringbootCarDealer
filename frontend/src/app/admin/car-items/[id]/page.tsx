'use client';

import { useEffect, useState, type ReactNode } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import StatusPill from '@/components/ui/StatusPill';
import Button from '@/components/ui/Button';
import OrderDetailModal from '@/components/admin/OrderDetailModal';
import { getCarItemDetail } from '@/lib/api';
import { formatDateTime, formatVnd } from '@/lib/utils/format';
import { getErrorMessage } from '@/lib/utils/error';
import type { CarItemDetail } from '@/types';

function Info({ label, value }: { label: string; value: ReactNode }) {
  return <div><dt className="text-[10px] font-semibold uppercase tracking-wider text-[#9ca3af]">{label}</dt><dd className="mt-1 break-words text-sm font-medium text-[#111827]">{value ?? '—'}</dd></div>;
}

export default function AdminCarItemDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const [detail, setDetail] = useState<CarItemDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [detailOrderId, setDetailOrderId] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    getCarItemDetail(params.id)
      .then((value) => { if (active) setDetail(value); })
      .catch((err) => { if (active) setError(getErrorMessage(err)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [params.id]);

  if (loading) return <p className="py-16 text-center text-sm text-[#9ca3af]">Đang tải thông tin xe…</p>;
  if (error || !detail) return <div className="space-y-4"><p className="text-sm text-red-600">{error || 'Không tìm thấy xe.'}</p><Button variant="outline" onClick={() => router.back()}>Quay lại</Button></div>;

  const { vehicle, specifications, relatedOrder } = detail;
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <Link href="/admin/car-items" className="text-xs font-medium text-[#a87900] hover:underline">← Danh sách xe</Link>
          <h1 className="mt-2 text-2xl font-serif font-bold text-[#111827]">{vehicle.carName}</h1>
          <p className="mt-1 text-sm text-[#6b7280]">Chi tiết xe vật lý · VIN {vehicle.vin ?? 'chưa cập nhật'}</p>
        </div>
        <StatusPill status={vehicle.status} />
      </div>

      <section className="rounded-2xl border border-[#e5e7eb] bg-white p-5">
        <h2 className="mb-4 font-semibold text-[#111827]">Thông tin xe</h2>
        <dl className="grid grid-cols-2 gap-4 md:grid-cols-3 xl:grid-cols-4">
          <Info label="VIN" value={vehicle.vin} />
          <Info label="Hãng" value={vehicle.brandName} />
          <Info label="Model" value={vehicle.modelName} />
          <Info label="Kiểu thân xe" value={vehicle.bodyType} />
          <Info label="Màu sắc" value={vehicle.color} />
          <Info label="Năm sản xuất" value={vehicle.year} />
          <Info label="Tình trạng" value={vehicle.condition === 'NEW' ? 'Mới' : 'Đã qua sử dụng'} />
          <Info label="Số km đã đi" value={vehicle.mileageKm?.toLocaleString('vi-VN')} />
          <Info label="Giá bán" value={formatVnd(vehicle.price)} />
          <Info label="Giá niêm yết dòng xe" value={vehicle.basePrice == null ? '—' : formatVnd(vehicle.basePrice)} />
          <Info label="Trạng thái kho" value={vehicle.status} />
          <Info label="Ghi chú" value={vehicle.note} />
        </dl>
        {vehicle.carDescription && <p className="mt-4 border-t border-[#f0f0f0] pt-4 text-sm leading-relaxed text-[#4b5563]">{vehicle.carDescription}</p>}
      </section>

      <section className="rounded-2xl border border-[#e5e7eb] bg-white p-5">
        <h2 className="mb-4 font-semibold text-[#111827]">Thông số kỹ thuật</h2>
        {!specifications ? <p className="text-sm text-[#9ca3af]">Dòng xe chưa có dữ liệu thông số kỹ thuật.</p> : (
          <>
            <dl className="grid grid-cols-2 gap-4 md:grid-cols-3">
              <Info label="Động cơ" value={specifications.engine} />
              <Info label="Hộp số" value={specifications.transmission} />
              <Info label="Nhiên liệu" value={specifications.fuelType} />
              <Info label="Số chỗ" value={specifications.seats} />
              <Info label="Kích thước" value={specifications.dimensions} />
              <Info label="Khối lượng" value={specifications.weightKg == null ? '—' : `${specifications.weightKg} kg`} />
              <Info label="Cập nhật thông số" value={specifications.updatedAt ? formatDateTime(specifications.updatedAt) : '—'} />
            </dl>
            <div className="mt-4">
              <p className="mb-2 text-[10px] font-semibold uppercase tracking-wider text-[#9ca3af]">Trang bị an toàn</p>
              {specifications.safetyFeatures?.length ? <div className="flex flex-wrap gap-2">{specifications.safetyFeatures.map((feature) => <span key={feature} className="rounded-full bg-[#f3f4f6] px-3 py-1 text-xs text-[#374151]">{feature}</span>)}</div> : <p className="text-sm text-[#9ca3af]">Chưa khai báo.</p>}
            </div>
            {Object.keys(specifications.extra ?? {}).length > 0 && (
              <div className="mt-4 border-t border-[#f0f0f0] pt-4">
                <p className="mb-2 text-[10px] font-semibold uppercase tracking-wider text-[#9ca3af]">Thông số mở rộng</p>
                <dl className="grid grid-cols-2 gap-4 md:grid-cols-3">
                  {Object.entries(specifications.extra).map(([key, value]) => <Info key={key} label={key} value={typeof value === 'object' ? JSON.stringify(value) : String(value ?? '—')} />)}
                </dl>
              </div>
            )}
          </>
        )}
      </section>

      <section className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#e5e7eb] bg-white p-5">
        <div>
          <h2 className="font-semibold text-[#111827]">Đơn hàng liên quan</h2>
          <p className="mt-1 text-sm text-[#6b7280]">{relatedOrder ? `Xe này thuộc đơn hàng ${relatedOrder.orderCode}.` : 'Xe chưa được gắn với đơn hàng nào.'}</p>
        </div>
        {relatedOrder && <Button onClick={() => setDetailOrderId(relatedOrder.orderId)}>Mở đơn hàng</Button>}
      </section>

      <OrderDetailModal orderId={detailOrderId} onClose={() => setDetailOrderId(null)} />
    </div>
  );
}
