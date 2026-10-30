'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import Button from '@/components/ui/Button';
import { Field, Input, Textarea } from '@/components/ui/Field';
import Loading from '@/components/ui/Loading';
import StatusPill from '@/components/ui/StatusPill';
import DataTable, { type Column } from '@/components/ui/DataTable';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/context/AuthContext';
import { getErrorMessage } from '@/lib/utils/error';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import { setCustomerId } from '@/lib/auth/token';
import {
  getMyProfile,
  updateMyProfile,
  updateMe,
  getOrdersByCustomer,
  getTestDrivesByCustomer,
  listNotifications,
  markNotificationRead,
  markAllNotificationsRead,
} from '@/lib/api';
import type { AppNotification, Customer, Order, TestDrive } from '@/types';

type Tab = 'orders' | 'test-drives' | 'notifications' | 'profile' | 'account';

const orderColumns: Column<Order>[] = [
  { key: 'orderCode', label: 'Mã đơn' },
  { key: 'orderDate', label: 'Ngày', render: (r) => formatDateTime(r.orderDate) },
  {
    key: 'totalAmount',
    label: 'Tổng tiền',
    className: 'text-right',
    render: (r) => formatVnd(r.totalAmount),
  },
  { key: 'items', label: 'Mặt hàng', className: 'text-center', render: (r) => r.items?.length ?? 0 },
  { key: 'status', label: 'Trạng thái', render: (r) => <StatusPill status={r.status} /> },
];

const testDriveColumns: Column<TestDrive>[] = [
  { key: 'scheduledAt', label: 'Thời gian', render: (r) => formatDateTime(r.scheduledAt) },
  {
    key: 'carName',
    label: 'Xe',
    render: (r) => (
      <span>
        {r.carName}
        {r.vin ? ` • ${r.vin}` : ''}
      </span>
    ),
  },
  { key: 'status', label: 'Trạng thái', render: (r) => <StatusPill status={r.status} /> },
  { key: 'note', label: 'Ghi chú', render: (r) => r.note ?? '—' },
];

export default function AccountPage() {
  const { user, isAuthenticated, loading: authLoading, refresh } = useAuth();
  const toast = useToast();

  const [tab, setTab] = useState<Tab>('orders');
  const [profile, setProfile] = useState<Customer | null>(null);
  const [orders, setOrders] = useState<Order[]>([]);
  const [testDrives, setTestDrives] = useState<TestDrive[]>([]);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');

  // Tài khoản đăng nhập
  const [loginEmail, setLoginEmail] = useState('');
  const [newPassword, setNewPassword] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setNotifications(await listNotifications().catch(() => [] as AppNotification[]));

      // Hồ sơ khách hàng gắn với tài khoản đang đăng nhập.
      let current: Customer | null = null;
      try {
        current = await getMyProfile();
        setProfile(current);
        setCustomerId(current.id);
        setFullName(current.fullName);
        setPhone(current.phone ?? '');
        setAddress(current.address ?? '');
      } catch {
        current = null;
        setProfile(null);
      }
      if (current) {
        const [ordersData, drivesData] = await Promise.all([
          getOrdersByCustomer(current.id),
          getTestDrivesByCustomer(current.id),
        ]);
        setOrders(ordersData);
        setTestDrives(drivesData);
      } else {
        setOrders([]);
        setTestDrives([]);
      }
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    if (!authLoading && isAuthenticated) load();
    if (!authLoading && !isAuthenticated) setLoading(false);
  }, [authLoading, isAuthenticated, load]);

  useEffect(() => {
    setLoginEmail(user?.email ?? '');
  }, [user?.email]);

  const submitProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      // PUT /api/customers/me: tạo mới nếu chưa có, cập nhật nếu đã có.
      const saved = await updateMyProfile({
        fullName,
        phone: phone || null,
        address: address || null,
        email: user?.email ?? null,
      });
      setProfile(saved);
      setCustomerId(saved.id);
      toast.success('Đã lưu hồ sơ khách hàng');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const readNotification = async (notification: AppNotification) => {
    if (notification.read) return;
    try {
      const updated = await markNotificationRead(notification.id);
      setNotifications((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const readAllNotifications = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));
      toast.success('Đã đánh dấu tất cả là đã đọc');
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const submitAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await updateMe({
        email: loginEmail || null,
        password: newPassword || null,
      });
      await refresh();
      setNewPassword('');
      toast.success('Đã cập nhật thông tin tài khoản');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (authLoading || loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <Loading />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="max-w-2xl mx-auto px-6 py-20 text-center">
        <h1 className="text-3xl font-serif font-bold mb-4">Tài khoản</h1>
        <p className="text-[#6b7280] mb-8">Vui lòng đăng nhập để xem thông tin tài khoản.</p>
        <Link
          href="/login?next=/account"
          className="inline-block px-8 py-3.5 rounded-xl bg-[#0a0a0a] text-white font-medium hover:bg-[#c58e00] transition-colors"
        >
          Đăng nhập
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-6 lg:px-8 py-12">
      <div className="flex items-center gap-4 mb-8">
        <div className="w-14 h-14 rounded-full bg-[#c58e00] text-white flex items-center justify-center text-xl font-bold uppercase">
          {user?.username.charAt(0)}
        </div>
        <div>
          <h1 className="text-2xl font-serif font-bold text-[#0a0a0a]">{user?.username}</h1>
          <p className="text-sm text-[#6b7280] flex items-center gap-2 flex-wrap">
            <span>{user?.email ?? 'Chưa có email'}</span>
            <span>• Vai trò: {user?.role}</span>
            <span>•</span>
            <span
              className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-medium uppercase tracking-wide border ${
                user?.enabled
                  ? 'bg-[#ecfdf5] text-[#10b981] border-[#10b981]/30'
                  : 'bg-[#fef2f2] text-[#ef4444] border-[#ef4444]/30'
              }`}
            >
              {user?.enabled ? 'Đang hoạt động' : 'Đã bị khoá'}
            </span>
          </p>
        </div>
      </div>

      <div className="flex gap-2 border-b border-[#e5e7eb] mb-6">
        {(
          [
            ['orders', `Đơn hàng (${orders.length})`],
            ['test-drives', `Lịch lái thử (${testDrives.length})`],
            ['notifications', `Thông báo (${notifications.filter((n) => !n.read).length})`],
            ['profile', 'Hồ sơ khách hàng'],
            ['account', 'Tài khoản'],
          ] as [Tab, string][]
        ).map(([key, label]) => (
          <button
            key={key}
            onClick={() => setTab(key)}
            className={`px-4 py-3 text-sm font-medium border-b-2 -mb-px transition-colors ${
              tab === key
                ? 'border-[#c58e00] text-[#0a0a0a]'
                : 'border-transparent text-[#6b7280] hover:text-[#0a0a0a]'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {tab === 'orders' && (
        <div>
          {profile ? (
            <DataTable columns={orderColumns} rows={orders} rowKey={(r) => r.id} empty="Bạn chưa có đơn hàng nào." />
          ) : (
            <ProfilePrompt
              onSubmit={submitProfile}
              busy={busy}
              fullName={fullName}
              setFullName={setFullName}
              phone={phone}
              setPhone={setPhone}
              address={address}
              setAddress={setAddress}
              email={user?.email ?? ''}
            />
          )}
        </div>
      )}

      {tab === 'test-drives' && (
        <div>
          {profile ? (
            <DataTable columns={testDriveColumns} rows={testDrives} rowKey={(r) => r.id} empty="Bạn chưa có lịch lái thử nào." />
          ) : (
            <ProfilePrompt
              onSubmit={submitProfile}
              busy={busy}
              fullName={fullName}
              setFullName={setFullName}
              phone={phone}
              setPhone={setPhone}
              address={address}
              setAddress={setAddress}
              email={user?.email ?? ''}
            />
          )}
        </div>
      )}

      {tab === 'notifications' && (
        <div className="max-w-3xl">
          <div className="flex items-center justify-between mb-4">
            <p className="text-sm text-[#6b7280]">
              Thông báo về đơn hàng, thanh toán, hợp đồng, hóa đơn và lịch lái thử.
            </p>
            {notifications.some((n) => !n.read) && (
              <Button size="sm" variant="outline" onClick={readAllNotifications}>
                Đọc tất cả
              </Button>
            )}
          </div>

          {notifications.length === 0 ? (
            <div className="rounded-2xl border border-[#e5e7eb] bg-white py-16 text-center text-sm text-[#9ca3af]">
              Bạn chưa có thông báo nào.
            </div>
          ) : (
            <div className="space-y-3">
              {notifications.map((notification) => (
                <button
                  key={notification.id}
                  onClick={() => readNotification(notification)}
                  className={`w-full text-left rounded-2xl border p-4 transition-colors ${
                    notification.read
                      ? 'border-[#e5e7eb] bg-white'
                      : 'border-[#c58e00]/40 bg-[#fffbeb] hover:bg-[#fef3c7]'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-[#0a0a0a]">
                        {notification.title}
                      </p>
                      <p className="mt-1 text-sm text-[#4b5563] leading-relaxed">
                        {notification.message}
                      </p>
                    </div>
                    {!notification.read && (
                      <span className="mt-1 h-2 w-2 rounded-full bg-[#c58e00] shrink-0" />
                    )}
                  </div>
                  <p className="mt-2 text-xs text-[#9ca3af]">
                    {notification.type} • {formatDateTime(notification.createdAt)}
                  </p>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'profile' && (
        <div>
          {profile ? (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="rounded-2xl border border-[#e5e7eb] bg-white p-6 space-y-3 text-sm">
                <Row label="Mã khách hàng" value={`#${profile.id}`} />
                <Row label="Họ và tên" value={profile.fullName} />
                <Row label="Điện thoại" value={profile.phone ?? '—'} />
                <Row label="Email" value={profile.email ?? '—'} />
                <Row label="Địa chỉ" value={profile.address ?? '—'} />
                <Row label="CMND/CCCD" value={profile.nationalId ?? '—'} />
              </div>
              <ProfileForm
                title="Cập nhật hồ sơ"
                subtitle="Chỉnh sửa thông tin hồ sơ khách hàng của bạn."
                onSubmit={submitProfile}
                busy={busy}
                fullName={fullName}
                setFullName={setFullName}
                phone={phone}
                setPhone={setPhone}
                address={address}
                setAddress={setAddress}
                email={user?.email ?? ''}
              />
            </div>
          ) : (
            <ProfilePrompt
              onSubmit={submitProfile}
              busy={busy}
              fullName={fullName}
              setFullName={setFullName}
              phone={phone}
              setPhone={setPhone}
              address={address}
              setAddress={setAddress}
              email={user?.email ?? ''}
            />
          )}
        </div>
      )}

      {tab === 'account' && (
        <div className="rounded-2xl border border-[#e5e7eb] bg-white p-6 max-w-xl">
          <h2 className="font-serif font-bold text-lg mb-1">Thông tin đăng nhập</h2>
          <p className="text-sm text-[#6b7280] mb-5">
            Cập nhật email hoặc đổi mật khẩu. Bỏ trống phần không muốn thay đổi.
          </p>
          <form onSubmit={submitAccount} className="space-y-4">
            <Field label="Tên đăng nhập">
              <Input value={user?.username ?? ''} disabled />
            </Field>
            <Field label="Email">
              <Input
                type="email"
                value={loginEmail}
                onChange={(e) => setLoginEmail(e.target.value)}
                placeholder="you@example.com"
              />
            </Field>
            <Field label="Mật khẩu mới" hint="Tối thiểu 4 ký tự. Bỏ trống nếu không đổi.">
              <Input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="••••••"
              />
            </Field>
            <Button type="submit" variant="gold" loading={busy}>
              Lưu thay đổi
            </Button>
          </form>
        </div>
      )}
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b border-[#f3f4f6] pb-2">
      <span className="text-[#6b7280]">{label}</span>
      <span className="font-medium text-[#0a0a0a]">{value}</span>
    </div>
  );
}

interface ProfileFormProps {
  title: string;
  subtitle: string;
  onSubmit: (e: React.FormEvent) => void;
  busy: boolean;
  fullName: string;
  setFullName: (v: string) => void;
  phone: string;
  setPhone: (v: string) => void;
  address: string;
  setAddress: (v: string) => void;
  email: string;
}

function ProfileForm({
  title,
  subtitle,
  onSubmit,
  busy,
  fullName,
  setFullName,
  phone,
  setPhone,
  address,
  setAddress,
  email,
}: ProfileFormProps) {
  return (
    <div className="rounded-2xl border border-[#e5e7eb] bg-white p-6 max-w-xl">
      <h2 className="font-serif font-bold text-lg mb-1">{title}</h2>
      <p className="text-sm text-[#6b7280] mb-5">{subtitle}</p>
      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="Họ và tên" required>
          <Input value={fullName} onChange={(e) => setFullName(e.target.value)} required />
        </Field>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Field label="Số điện thoại">
            <Input value={phone} onChange={(e) => setPhone(e.target.value)} />
          </Field>
          <Field label="Email">
            <Input value={email} disabled />
          </Field>
        </div>
        <Field label="Địa chỉ">
          <Textarea value={address} onChange={(e) => setAddress(e.target.value)} />
        </Field>
        <Button type="submit" variant="gold" loading={busy}>
          Lưu hồ sơ
        </Button>
      </form>
    </div>
  );
}

function ProfilePrompt(props: Omit<ProfileFormProps, 'title' | 'subtitle'>) {
  return (
    <ProfileForm
      {...props}
      title="Tạo hồ sơ khách hàng"
      subtitle="Bạn cần có hồ sơ khách hàng để đặt hàng và đăng ký lái thử."
    />
  );
}
