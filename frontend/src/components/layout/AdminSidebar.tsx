'use client';

import { useState, type ReactNode } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

/** Icon line-style dùng chung (24px, stroke = currentColor). */
function Icon({ children }: { children: ReactNode }) {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className="shrink-0"
    >
      {children}
    </svg>
  );
}

const items = [
  {
    href: '/admin',
    label: 'Dashboard',
    icon: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="1.5" />
        <rect x="14" y="3" width="7" height="7" rx="1.5" />
        <rect x="3" y="14" width="7" height="7" rx="1.5" />
        <rect x="14" y="14" width="7" height="7" rx="1.5" />
      </>
    ),
  },
  {
    href: '/admin/brands',
    label: 'Hãng xe',
    icon: (
      <>
        <circle cx="12" cy="9" r="5" />
        <path d="m8.5 13-2 8 5.5-3 5.5 3-2-8" />
      </>
    ),
  },
  {
    href: '/admin/car-models',
    label: 'Dòng xe',
    icon: (
      <>
        <path d="m12 3 9 5-9 5-9-5z" />
        <path d="m3 13 9 5 9-5" />
      </>
    ),
  },
  {
    href: '/admin/cars',
    label: 'Xe',
    icon: (
      <>
        <rect x="3" y="11" width="18" height="6" rx="2" />
        <path d="M6.5 11 8 6.5A2 2 0 0 1 9.9 5h4.2a2 2 0 0 1 1.9 1.5L17.5 11" />
        <circle cx="7" cy="17.5" r="1.5" />
        <circle cx="17" cy="17.5" r="1.5" />
      </>
    ),
  },
  {
    href: '/admin/car-items',
    label: 'Xe trong kho',
    icon: (
      <>
        <path d="M21 8 12 3 3 8v8l9 5 9-5z" />
        <path d="M3 8l9 5 9-5" />
        <path d="M12 13v8" />
      </>
    ),
  },
  {
    href: '/admin/media',
    label: 'Media xe',
    icon: (
      <>
        <rect x="3" y="4" width="18" height="16" rx="2" />
        <circle cx="8.5" cy="9.5" r="1.5" />
        <path d="m21 16-5-5-9 9" />
      </>
    ),
  },
  {
    href: '/admin/specs',
    label: 'Thông số xe',
    icon: (
      <>
        <path d="M4 7h16M4 12h16M4 17h16" />
        <path d="M9 5v4M15 10v4M8 15v4" />
      </>
    ),
  },
  {
    href: '/admin/orders',
    label: 'Đơn hàng',
    icon: (
      <>
        <path d="M9 4H7a2 2 0 0 0-2 2v13a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2h-2" />
        <rect x="9" y="2" width="6" height="4" rx="1" />
        <path d="M9 12h6M9 16h6" />
      </>
    ),
  },
  {
    href: '/admin/contracts',
    label: 'Hợp đồng',
    icon: (
      <>
        <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
        <path d="M14 3v5h5" />
        <path d="M9 13h6M9 17h6" />
      </>
    ),
  },
  {
    href: '/admin/invoices',
    label: 'Hóa đơn',
    icon: (
      <>
        <path d="M6 3h12v18l-3-2-3 2-3-2-3 2z" />
        <path d="M9 8h6M9 12h6" />
      </>
    ),
  },
  {
    href: '/admin/payments',
    label: 'Thanh toán',
    icon: (
      <>
        <rect x="2" y="5" width="20" height="14" rx="2" />
        <path d="M2 10h20" />
      </>
    ),
  },
  {
    href: '/admin/collections',
    label: 'Công nợ',
    icon: (
      <>
        <rect x="3" y="4" width="18" height="16" rx="2" />
        <path d="M7 9h10M7 13h5M7 17h3" />
        <path d="m15 15 2 2 3-4" />
      </>
    ),
  },
  {
    href: '/admin/test-drives',
    label: 'Lịch lái thử',
    icon: (
      <>
        <rect x="3" y="5" width="18" height="16" rx="2" />
        <path d="M8 3v4M16 3v4M3 10h18" />
      </>
    ),
  },
  {
    href: '/admin/customers',
    label: 'Khách hàng',
    icon: (
      <>
        <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <path d="M22 21v-2a4 4 0 0 0-3-3.87" />
        <path d="M16 3.13a4 4 0 0 1 0 7.75" />
      </>
    ),
  },
  {
    href: '/admin/employees',
    label: 'Nhân viên',
    icon: (
      <>
        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
        <circle cx="12" cy="7" r="4" />
      </>
    ),
  },
  {
    href: '/admin/reviews',
    label: 'Đánh giá',
    icon: (
      <path d="m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6-5.4-2.8-5.4 2.8 1-6L3.2 9.4l6.1-.9z" />
    ),
  },
  {
    href: '/admin/users',
    label: 'Người dùng',
    icon: (
      <>
        <path d="M12 3 5 6v5c0 4.5 3 8 7 10 4-2 7-5.5 7-10V6z" />
        <path d="m9 12 2 2 4-4" />
      </>
    ),
  },
  {
    href: '/admin/activity-logs',
    label: 'Nhật ký',
    icon: (
      <>
        <path d="M3 12a9 9 0 1 0 3-6.7" />
        <path d="M3 4v4h4" />
        <path d="M12 8v4l3 2" />
      </>
    ),
  },
];

/**
 * Sidebar quản trị dạng hover:
 * - Bình thường: dải icon dọc hẹp (w-16) cố định bên trái, dưới header.
 * - Di chuột vào (hoặc focus bằng bàn phím): trượt rộng ra (w-64) hiện nhãn để chọn.
 * - Rời chuột: tự thu gọn lại về icon.
 */
export default function AdminSidebar() {
  const pathname = usePathname();
  const [expanded, setExpanded] = useState(false);

  return (
    <aside
      onMouseEnter={() => setExpanded(true)}
      onMouseLeave={() => setExpanded(false)}
      onFocus={() => setExpanded(true)}
      onBlur={(e) => {
        // Chỉ thu gọn khi focus rời khỏi toàn bộ sidebar (điều hướng bàn phím).
        if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setExpanded(false);
      }}
      aria-label="Menu quản trị"
      className={`fixed left-0 top-16 bottom-0 z-40 flex flex-col overflow-hidden border-r border-[#e5e7eb] bg-white transition-[width] duration-200 ease-out ${
        expanded ? 'w-64 shadow-xl' : 'w-16'
      }`}
    >
      <nav className="flex-1 overflow-y-auto overflow-x-hidden px-2 py-4 space-y-1">
        {items.map((item) => {
          const active =
            item.href === '/admin' ? pathname === '/admin' : pathname.startsWith(item.href);
          return (
            <Link
              key={item.href}
              href={item.href}
              title={expanded ? undefined : item.label}
              aria-label={item.label}
              className={`flex h-10 items-center gap-3 rounded-xl px-3 text-sm font-medium transition-colors ${
                active
                  ? 'bg-[#0a0a0a] text-white'
                  : 'text-[#4b5563] hover:bg-[#f3f4f6] hover:text-[#0a0a0a]'
              }`}
            >
              <Icon>{item.icon}</Icon>
              {/* Ẩn nhãn khi thu gọn: opacity-0 giữ icon không bị nhảy vị trí */}
              <span
                className={`whitespace-nowrap transition-opacity duration-150 ${
                  expanded ? 'opacity-100' : 'opacity-0'
                }`}
              >
                {item.label}
              </span>
            </Link>
          );
        })}
      </nav>

      <div
        className={`border-t border-[#e5e7eb] px-5 py-3 text-[10px] font-semibold uppercase tracking-widest text-[#9ca3af] transition-opacity duration-150 ${
          expanded ? 'opacity-100' : 'opacity-0'
        }`}
      >
        Quản trị
      </div>
    </aside>
  );
}
