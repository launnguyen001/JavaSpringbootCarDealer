'use client';

import type { ReactNode } from 'react';

interface KpiCardProps {
  label: string;
  value: ReactNode;
  hint?: string;
  icon?: ReactNode;
  accent?: boolean;
}

export default function KpiCard({ label, value, hint, icon, accent = false }: KpiCardProps) {
  return (
    <div
      className={`rounded-2xl border p-5 shadow-sm transition-shadow hover:shadow-md ${
        accent ? 'border-[#c58e00]/40 bg-[#fffbeb]' : 'border-[#e5e7eb] bg-white'
      }`}
    >
      <div className="flex items-start justify-between">
        <p className="text-[11px] font-semibold text-[#6b7280] uppercase tracking-wider">{label}</p>
        {icon && <div className="text-[#c58e00]">{icon}</div>}
      </div>
      <p className="mt-2 text-2xl font-serif font-bold text-[#0a0a0a] tracking-tight">{value}</p>
      {hint && <p className="mt-1 text-xs text-[#9ca3af]">{hint}</p>}
    </div>
  );
}
