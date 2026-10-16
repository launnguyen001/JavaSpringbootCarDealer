'use client';

interface EmptyStateProps {
  title?: string;
  subtitle?: string;
}

export default function EmptyState({ title = 'Không có xe phù hợp', subtitle }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="w-16 h-16 rounded-full bg-[#f3f4f6] flex items-center justify-center mb-4">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#c58e00" strokeWidth="2">
          <circle cx="11" cy="8" r="6" />
          <path d="M21 21l-4-4" />
        </svg>
      </div>
      <h3 className="text-xl font-serif font-bold text-[#0a0a0a] mb-2">{title}</h3>
      {subtitle && <p className="text-[#6b7280] max-w-xs">{subtitle}</p>}
    </div>
  );
}
