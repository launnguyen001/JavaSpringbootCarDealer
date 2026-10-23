'use client';

import { usePathname } from 'next/navigation';
import Footer from '@/components/layout/Footer';

/**
 * Footer chỉ hiển thị ở các trang công khai.
 * Đang ẩn khi đang ở khu vực admin (/admin/*).
 */
export default function ConditionalFooter() {
  const pathname = usePathname();
  const isAdmin = pathname.startsWith('/admin');
  return isAdmin ? null : <Footer />;
}
