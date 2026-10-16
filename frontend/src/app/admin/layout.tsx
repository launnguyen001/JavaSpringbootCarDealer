'use client';

import { useEffect, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import AdminSidebar from '@/components/layout/AdminSidebar';

export default function AdminLayout({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && (!user || user.role !== 'ADMIN')) {
      router.replace('/login?next=/admin');
    }
  }, [loading, user, router]);

  if (loading || !user || user.role !== 'ADMIN') {
    return (
      <div className="min-h-[60vh] flex items-center justify-center text-[#9ca3af] text-sm">
        Đang kiểm tra quyền truy cập…
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#fafafa]">
      {/* Sidebar dạng dải icon dọc cố định bên trái (mở rộng khi hover) */}
      <AdminSidebar />
      {/* pl-16 = đúng chiều rộng dải icon khi thu gọn, không bị che nội dung */}
      <div className="pl-16">
        <div className="max-w-[1400px] mx-auto px-4 lg:px-8 py-8">
          <main>{children}</main>
        </div>
      </div>
    </div>
  );
}
