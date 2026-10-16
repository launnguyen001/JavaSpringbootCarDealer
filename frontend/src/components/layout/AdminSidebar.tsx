'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';

const items = [
  { href: '/admin', label: 'Dashboard' },
  { href: '/admin/cars', label: 'Xe' },
  { href: '/admin/brands', label: 'Hãng xe' },
  { href: '/admin/car-models', label: 'Dòng xe' },
  { href: '/admin/customers', label: 'Khách hàng' },
  { href: '/admin/employees', label: 'Nhân viên' },
  { href: '/admin/orders', label: 'Đơn hàng' },
  { href: '/admin/test-drives', label: 'Lịch lái thử' },
  { href: '/admin/payments', label: 'Thanh toán' },
  { href: '/admin/contracts', label: 'Hợp đồng' },
  { href: '/admin/invoices', label: 'Hóa đơn' },
  { href: '/admin/reviews', label: 'Đánh giá' },
  { href: '/admin/media', label: 'Media xe' },
  { href: '/admin/specs', label: 'Thông số xe' },
  { href: '/admin/activity-logs', label: 'Nhật ký' },
];

export default function AdminSidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden lg:block w-60 flex-shrink-0">
      <div className="sticky top-24 bg-white rounded-2xl border border-[#e5e7eb] p-4 shadow-sm">
        <p className="px-3 py-2 text-[10px] font-semibold text-[#9ca3af] uppercase tracking-widest">
          Quản trị
        </p>
        <nav className="space-y-1">
          {items.map((item) => {
            const active =
              item.href === '/admin'
                ? pathname === '/admin'
                : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`block px-3 py-2 rounded-xl text-sm font-medium transition-colors ${
                  active
                    ? 'bg-[#0a0a0a] text-white'
                    : 'text-[#4b5563] hover:bg-[#f3f4f6] hover:text-[#0a0a0a]'
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
      </div>
    </aside>
  );
}
