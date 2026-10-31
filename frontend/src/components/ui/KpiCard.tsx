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
      className={`group h-full rounded-2xl border p-5 shadow-sm transition-all duration-200 hover:-translate-y-0.5 hover:border-[#c58e00]/60 hover:bg-[#fffdf6] hover:shadow-lg hover:shadow-[#c58e00]/10 ${
        accent ? 'border-[#c58e00]/40 bg-[#fffbeb]' : 'border-[#e5e7eb] bg-white'
      }`}
    >
      <div className="flex items-start justify-between">
        <p className="text-[11px] font-semibold text-[#6b7280] uppercase tracking-wider transition-colors group-hover:text-[#8a6d00]">{label}</p>
        {icon && <div className="text-[#c58e00]">{icon}</div>}
      </div>
      <p className="mt-2 text-2xl font-serif font-bold text-[#0a0a0a] tracking-tight transition-colors group-hover:text-[#8a6d00]">{value}</p>
      {hint && <p className="mt-1 text-xs text-[#9ca3af]">{hint}</p>}
    </div>
  );
}
