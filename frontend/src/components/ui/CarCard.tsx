'use client';

import Link from 'next/link';
import { Car } from '@/types/car';
import PriceTag from '@/components/ui/PriceTag';
import StatusBadge from '@/components/ui/StatusBadge';
import RatingStars from '@/components/ui/RatingStars';
import Image from "next/image";

interface CarCardProps {
  car: Car;
  /** Bố cục gọn (ảnh thấp, padding nhỏ) — dùng cho trang danh sách để vừa 1 view */
  compact?: boolean;
}

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Đã dùng' };

export default function CarCard({ car, compact = false }: CarCardProps) {
  const isAvailable = car.status === 'AVAILABLE';
  // Ảnh từ media (car-media.thumbnail) ưu tiên; nếu chưa có thì dùng fallback Unsplash theo mẫu xe.
  const fallbackImage = car.modelName?.toLowerCase().includes('camry')
    ? 'https://images.unsplash.com/photo-1607082348822-070a57f97f21?w=800&q=80'
    : 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=800&q=80';
  const imageSrc = car.thumbnail ?? fallbackImage;

  // Giá hiển thị: khoảng giá theo các chiếc thực tế, nếu chỉ một mức thì lấy mức đó.
  const from = car.priceFrom ?? car.basePrice;
  const to = car.priceTo ?? car.priceFrom ?? car.basePrice;
  const isRange = from != null && to != null && from !== to;

  return (
    <Link href={`/cars/${car.id}`} className="block group h-full">
      <div className="relative overflow-hidden rounded-2xl border border-[#e5e7eb] bg-white shadow-sm transition-all duration-300 hover:shadow-[0_20px_25px_-5px_rgba(0,0,0,0.1),0_10px_10px_-5px_rgba(0,0,0,0.04)] hover:-translate-y-1">
        <div className={`relative bg-[#f3f4f6] overflow-hidden ${compact ? 'aspect-[16/10]' : 'aspect-[4/3]'}`}>
          <img
            src={imageSrc}
            alt={car.name}
            loading="lazy"
            className="absolute inset-0 w-full h-full object-cover object-center transition-transform duration-500 group-hover:scale-105"
          />

          {!isAvailable && (
            <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
              <span className="text-white font-serif font-bold text-sm tracking-widest uppercase">
                {car.status === 'SOLD' ? 'Đã bán' : 'Không thể đặt'}
              </span>
            </div>
          )}
          <div className="absolute top-3 left-3 z-10">
            <StatusBadge status={car.status} className="shadow-md" />
          </div>
        </div>
        <div className={compact ? 'p-3.5' : 'p-5'}>
          <div className="flex justify-between items-start gap-2 mb-1">
            <h3
              className={`font-serif font-bold text-[#0a0a0a] leading-tight group-hover:text-[#c58e00] transition-colors ${
                compact ? 'text-base' : 'text-lg'
              }`}
            >
              {car.name}
            </h3>
            <PriceTag
              price={isRange ? from : (from ?? car.basePrice)}
              className={compact ? 'text-[#c58e00] whitespace-nowrap' : 'text-[#c58e00]'}
            />
          </div>
          {car.brandName && car.modelName && (
            <p
              className={`tracking-wide uppercase font-medium text-[#6b7280] ${
                compact ? 'text-[11px] mb-1.5' : 'text-xs mb-3'
              }`}
            >
              {car.brandName} • {car.modelName}
            </p>
          )}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1 text-[#6b7280] text-xs">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M20 6L9 17l-5-5" />
              </svg>
              {car.conditions.map((c) => CONDITION_LABEL[c] ?? c).join(' • ') || '—'}
            </div>
            <div className="flex items-center gap-1 text-[#6b7280] text-xs">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              {car.availableItems} xe
            </div>
            {!compact && <RatingStars rating={4.8} size={12} className="ml-auto" />}
          </div>
        </div>
      </div>
    </Link>
  );
}
