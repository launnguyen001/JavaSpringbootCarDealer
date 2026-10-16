'use client';

import { formatPrice } from '@/lib/utils/format';

interface PriceTagProps {
  price: number | null | undefined;
  className?: string;
}

export default function PriceTag({ price, className }: PriceTagProps) {
  return (
    <span
      className={`text-lg font-serif font-bold text-[#0a0a0a] tracking-tighter ${className ?? ''}`}
    >
      {formatPrice(price)}
    </span>
  );
}
