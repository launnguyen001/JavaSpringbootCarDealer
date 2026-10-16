'use client';

export default function Loading() {
  return (
    <div className="flex flex-col items-center justify-center py-20 gap-3">
      <div className="relative w-12 h-12">
        <svg
          className="absolute inset-0 w-full h-full animate-spin text-[#c58e00]"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
        >
          <circle cx="12" cy="12" r="10" />
        </svg>
      </div>
      <p className="text-[#6b7280] text-sm tracking-wide uppercase font-medium">Đang tải...</p>
    </div>
  );
}
