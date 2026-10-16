'use client';

import { useRouter, useSearchParams, usePathname } from 'next/navigation';
import { useDebounce } from '@/hooks/useDebounce';
import { useState, useEffect, useRef } from 'react';

interface SearchBarProps {
  placeholder?: string;
}

export default function SearchBar({ placeholder = 'Tìm xe theo tên…' }: SearchBarProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const pathname = usePathname();

  const urlQuery = searchParams.get('q') ?? '';
  const [query, setQuery] = useState(urlQuery);

  // Luôn giữ giá trị searchParams mới nhất để debounce không phải thêm dependency.
  const searchParamsRef = useRef(searchParams);
  searchParamsRef.current = searchParams;

  // URL đổi từ bên ngoài (bộ lọc, back/forward, ô tìm kiếm ở header) → đồng bộ lại ô nhập.
  useEffect(() => {
    setQuery((prev) => (prev === urlQuery ? prev : urlQuery));
  }, [urlQuery]);

  const debouncedSearch = useDebounce((value: string) => {
    const params = new URLSearchParams(searchParamsRef.current.toString());
    const trimmed = value.trim();
    if (trimmed) {
      params.set('q', trimmed);
    } else {
      params.delete('q');
    }
    // Đổi từ khóa thì quay về trang 1
    params.delete('page');

    const next = params.toString();
    if (next === searchParamsRef.current.toString()) return; // URL không đổi → tránh điều hướng lặp
    router.push(next ? `${pathname}?${next}` : pathname);
  }, 400);

  const handleChange = (value: string) => {
    setQuery(value);
    debouncedSearch(value);
  };

  return (
    <div className="relative">
      <input
        type="text"
        value={query}
        onChange={(e) => handleChange(e.target.value)}
        placeholder={placeholder}
        className="w-full px-4 py-2.5 rounded-xl border border-[#e5e7eb] bg-[#fafafa] text-[#0a0a0a] text-sm focus:outline-none focus:ring-2 focus:ring-[#c58e00] placeholder:text-[#9ca3af]"
      />
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#9ca3af" strokeWidth="1.5" className="absolute right-3 top-1/2 -mt-[9px] text-[#6b7280]">
        <circle cx="11" cy="11" r="8" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
    </div>
  );
}
