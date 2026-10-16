'use client';

import { Car } from '@/types/car';
import CarCard from '@/components/ui/CarCard';

interface CarGridProps {
  cars: Car[];
  /** Bố cục gọn hơn (trang danh sách) để lưới vừa 1 view */
  compact?: boolean;
}

export default function CarGrid({ cars, compact = false }: CarGridProps) {
  if (cars.length === 0) return null;
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 xl:gap-5">
      {cars.map((car) => (
        <CarCard key={car.id} car={car} compact={compact} />
      ))}
    </div>
  );
}
