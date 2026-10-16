'use client';

import { Suspense, useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import Button from '@/components/ui/Button';
import { Field, Input } from '@/components/ui/Field';
import ErrorMessage from '@/components/ui/ErrorMessage';
import { useAuth } from '@/context/AuthContext';
import { getErrorMessage } from '@/lib/utils/error';

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-[70vh]" />}>
      <LoginForm />
    </Suspense>
  );
}

function LoginForm() {
  const { login, isAuthenticated, isAdmin, loading: authLoading } = useAuth();
  const router = useRouter();
  const params = useSearchParams();
  const next = params.get('next') || '/';

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Đã đăng nhập thì KHÔNG cho mở form login nữa → tự chuyển hướng
  useEffect(() => {
    if (!authLoading && isAuthenticated) {
      router.replace(isAdmin ? '/admin' : next);
    }
  }, [authLoading, isAuthenticated, isAdmin, next, router]);

  if (isAuthenticated) {
    return (
      <div className="min-h-[80vh] flex items-center justify-center text-[#6b7280]">
        Đang chuyển hướng…
      </div>
    );
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const user = await login(username, password);
      if (user.role === 'ADMIN') {
        router.replace('/admin');
      } else {
        router.replace(next);
      }
      router.refresh();
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[80vh] flex items-center justify-center px-6 py-16 bg-[#fafafa]">
      <div className="w-full max-w-md bg-white rounded-2xl border border-[#e5e7eb] shadow-sm p-8">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-serif font-bold text-[#0a0a0a]">Đăng nhập</h1>
          <p className="text-sm text-[#6b7280] mt-2">
            Chào mừng trở lại Car Dealer
          </p>
        </div>

        {error && (
          <div className="mb-5">
            <ErrorMessage message={error} />
          </div>
        )}

        <form onSubmit={submit} className="space-y-4">
          <Field label="Tên đăng nhập" required htmlFor="username">
            <Input
              id="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Nhập tên đăng nhập"
              autoComplete="username"
              required
            />
          </Field>
          <Field label="Mật khẩu" required htmlFor="password">
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Nhập mật khẩu"
              autoComplete="current-password"
              required
            />
          </Field>

          <Button type="submit" variant="primary" size="lg" loading={loading} className="w-full">
            Đăng nhập
          </Button>
        </form>

        <p className="text-center text-sm text-[#6b7280] mt-6">
          Chưa có tài khoản?{' '}
          <Link href="/register" className="text-[#c58e00] font-medium hover:underline">
            Đăng ký ngay
          </Link>
        </p>

        <div className="mt-6 p-4 rounded-xl bg-[#f9fafb] border border-[#f3f4f6] text-xs text-[#6b7280]">
          <p className="font-semibold text-[#374151] mb-1">Tài khoản demo:</p>
          <p>Quản trị: <b>admin / admin</b></p>
          <p>Khách hàng: <b>user / user</b></p>
          <p>Nhân viên: <b>employee / employee</b></p>
        </div>
      </div>
    </div>
  );
}
