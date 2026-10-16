'use client';

import { CarStatus } from '@/types/car';

const statusConfig: Record<CarStatus, { label: string; color: string; bg: string }> = {
  AVAILABLE: { label: 'Sẵn sàng', color: '#10b981', bg: '#ecfdf5' },
  RESERVED: { label: 'Đã đặt', color: '#f59e0b', bg: '#fffbeb' },
  SOLD: { label: 'Đã bán', color: '#6b7280', bg: '#f3f4f6' },
  MAINTENANCE: { label: 'Bảo trì', color: '#ef4444', bg: '#fef2f2' },
};

interface StatusBadgeProps {
  status: CarStatus;
  className?: string;
}

export default function StatusBadge({ status, className }: StatusBadgeProps) {
  const cfg = statusConfig[status];
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[10px] font-medium tracking-wide uppercase border ${cfg.bg} ${cfg.color} border-${cfg.color === '#10b981' ? 'green' : cfg.color === '#f59e0b' ? 'yellow' : cfg.color === '#ef4444' ? 'red' : 'gray'}-200 ${className ?? ''}`}
    >
      <span
        className="w-1.5 h-1.5 rounded-full"
        style={{ backgroundColor: cfg.color }}
      ></span>
      {cfg.label}
    </span>
  );
}
