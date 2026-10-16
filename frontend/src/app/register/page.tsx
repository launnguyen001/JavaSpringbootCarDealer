'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import Button from '@/components/ui/Button';
import { Field, Input } from '@/components/ui/Field';
import ErrorMessage from '@/components/ui/ErrorMessage';
import { useAuth } from '@/context/AuthContext';
import { getErrorMessage } from '@/lib/utils/error';

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    if (password.length < 4) {
      setError('Mật khẩu cần ít nhất 4 ký tự.');
      return;
    }
    setLoading(true);
    try {
      await register({ username, password, email: email || null });
      router.replace('/account');
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
          <h1 className="text-3xl font-serif font-bold text-[#0a0a0a]">Đăng ký</h1>
          <p className="text-sm text-[#6b7280] mt-2">Tạo tài khoản để trải nghiệm dịch vụ</p>
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
              placeholder="Tên đăng nhập"
              autoComplete="username"
              required
            />
          </Field>
          <Field label="Email" htmlFor="email">
            <Input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              autoComplete="email"
            />
          </Field>
          <Field label="Mật khẩu" required htmlFor="password">
            <Input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Ít nhất 4 ký tự"
              autoComplete="new-password"
              required
            />
          </Field>

          <Button type="submit" variant="gold" size="lg" loading={loading} className="w-full">
            Tạo tài khoản
          </Button>
        </form>

        <p className="text-center text-sm text-[#6b7280] mt-6">
          Đã có tài khoản?{' '}
          <Link href="/login" className="text-[#c58e00] font-medium hover:underline">
            Đăng nhập
          </Link>
        </p>
      </div>
    </div>
  );
}
