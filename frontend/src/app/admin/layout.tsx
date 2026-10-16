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
      <div className="max-w-[1400px] mx-auto px-4 lg:px-8 py-8 flex gap-8">
        <AdminSidebar />
        <main className="flex-1 min-w-0">{children}</main>
      </div>
    </div>
  );
}
