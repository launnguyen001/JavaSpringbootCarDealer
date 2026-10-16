'use client';

import Link from 'next/link';
import { Car } from '@/types/car';
import PriceTag from '@/components/ui/PriceTag';
import StatusBadge from '@/components/ui/StatusBadge';
import RatingStars from '@/components/ui/RatingStars';
import Image from 'next/image';

interface CarCardProps {
  car: Car;
}

export default function CarCard({ car }: CarCardProps) {
  const isAvailable = car.status === 'AVAILABLE';
  const imageSrc = car.modelName?.toLowerCase().includes('camry')
    ? 'https://images.unsplash.com/photo-1607082348822-070a57f97f21?w=800&q=80'
    : 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=800&q=80';

  return (
    <Link href={`/cars/${car.id}`} className="block group">
      <div className="relative overflow-hidden rounded-2xl border border-[#e5e7eb] bg-white shadow-sm transition-all duration-300 hover:shadow-[0_20px_25px_-5px_rgba(0,0,0,0.1),0_10px_10px_-5px_rgba(0,0,0,0.04)] hover:-translate-y-1">
        <div className="relative h-48 bg-[#f3f4f6] overflow-hidden">
          <Image
            src={imageSrc}
            alt={car.name}
            fill
            sizes="(max-width: 768px) 100vw, 250px"
            className="object-cover object-center transition-transform duration-500 group-hover:scale-105"
            priority
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
        <div className="p-5">
          <div className="flex justify-between items-start mb-2">
            <h3 className="text-lg font-serif font-bold text-[#0a0a0a] leading-tight group-hover:text-[#c58e00] transition-colors">
              {car.name}
            </h3>
            <PriceTag price={car.price} className="text-[#c58e00]" />
          </div>
          {car.brandName && car.modelName && (
            <p className="text-xs text-[#6b7280] mb-3 tracking-wide uppercase font-medium">
              {car.brandName} • {car.modelName}
            </p>
          )}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-1 text-[#6b7280] text-xs">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M22 2H11l-4 4V2H2l8 8 8-8z" />
              </svg>
              {car.year ?? '—'}
            </div>
            <div className="flex items-center gap-1 text-[#6b7280] text-xs">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              {car.quantity} xe
            </div>
            <RatingStars rating={4.8} size={12} className="ml-auto" />
          </div>
        </div>
      </div>
    </Link>
  );
}
