'use client';

import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';

interface PaginationProps {
  totalItems: number;
  pageSize?: number;
}

export default function Pagination({ totalItems, pageSize = 9 }: PaginationProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const pathname = usePathname();
  const [currentPage, setCurrentPage] = useState(() => {
    const p = parseInt(searchParams.get('page') ?? '1', 10);
    return Number.isNaN(p) || p < 1 ? 1 : p;
  });

  const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));

  useEffect(() => {
    const p = parseInt(searchParams.get('page') ?? '1', 10);
    setCurrentPage(Number.isNaN(p) || p < 1 ? 1 : p);
  }, [searchParams]);

  const setPage = (page: number) => {
    if (page < 1 || page > totalPages) return;
    const params = new URLSearchParams(searchParams.toString());
    if (page === 1) {
      params.delete('page');
    } else {
      params.set('page', String(page));
    }
    router.push(`${pathname}?${params.toString()}`);
    setCurrentPage(page);
  };

  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-center gap-2 mt-10">
      <button
        onClick={() => setPage(currentPage - 1)}
        disabled={currentPage === 1}
        className="px-4 py-2 rounded-xl border border-[#e5e7eb] text-[#0a0a0a] text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f3f4f6] transition-colors"
      >
        Trước
      </button>
      <div className="flex items-center gap-1">
        {[1, 2, 3, 4, 5].map((i) => {
          const page = currentPage - 3 + i;
          if (page < 1 || page > totalPages) return null;
          return (
            <button
              key={page}
              onClick={() => setPage(page)}
              className={`w-8 h-8 rounded-xl text-sm font-medium flex items-center justify-center transition-colors ${
                page === currentPage
                  ? 'bg-[#0a0a0a] text-white'
                  : 'text-[#6b7280] hover:bg-[#f3f4f6] hover:text-[#0a0a00]'
              }`}
            >
              {page}
            </button>
          );
        })}
      </div>
      <button
        onClick={() => setPage(currentPage + 1)}
        disabled={currentPage === totalPages}
        className="px-4 py-2 rounded-xl border border-[#e5e7eb] text-[#0a0a0a] text-sm font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#f3f4f6] transition-colors"
      >
        Tiếp
      </button>
    </div>
  );
}
