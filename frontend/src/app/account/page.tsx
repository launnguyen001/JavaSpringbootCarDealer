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
import { getCustomerId, setCustomerId } from '@/lib/auth/token';
import {
  getCustomer,
  createCustomer,
  getOrdersByCustomer,
  getTestDrivesByCustomer,
} from '@/lib/api';
import type { Customer, Order, TestDrive } from '@/types';

type Tab = 'orders' | 'test-drives' | 'profile';

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
  { key: 'carName', label: 'Xe' },
  { key: 'status', label: 'Trạng thái', render: (r) => <StatusPill status={r.status} /> },
  { key: 'note', label: 'Ghi chú', render: (r) => r.note ?? '—' },
];

export default function AccountPage() {
  const { user, isAuthenticated, loading: authLoading } = useAuth();
  const toast = useToast();

  const [tab, setTab] = useState<Tab>('orders');
  const [customerId, setCustomerIdState] = useState<number | null>(null);
  const [profile, setProfile] = useState<Customer | null>(null);
  const [orders, setOrders] = useState<Order[]>([]);
  const [testDrives, setTestDrives] = useState<TestDrive[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');

  useEffect(() => {
    setCustomerIdState(getCustomerId());
  }, []);

  const load = useCallback(async (id: number) => {
    setLoading(true);
    try {
      const [ordersData, drivesData] = await Promise.all([
        getOrdersByCustomer(id),
        getTestDrivesByCustomer(id),
      ]);
      setOrders(ordersData);
      setTestDrives(drivesData);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    if (!authLoading && isAuthenticated && customerId) {
      getCustomer(customerId).then(setProfile).catch(() => setProfile(null));
      load(customerId);
    }
    if (!authLoading && (!isAuthenticated || !customerId)) setLoading(false);
  }, [authLoading, isAuthenticated, customerId, load]);

  const submitProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      const created = await createCustomer({
        fullName,
        phone: phone || null,
        address: address || null,
        email: user?.email ?? null,
      });
      setCustomerId(created.id);
      setCustomerIdState(created.id);
      setProfile(created);
      toast.success('Đã tạo hồ sơ khách hàng');
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
          <p className="text-sm text-[#6b7280]">
            {user?.email ?? 'Chưa có email'} • Vai trò: {user?.role}
          </p>
        </div>
      </div>

      <div className="flex gap-2 border-b border-[#e5e7eb] mb-6">
        {(
          [
            ['orders', `Đơn hàng (${orders.length})`],
            ['test-drives', `Lịch lái thử (${testDrives.length})`],
            ['profile', 'Hồ sơ'],
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
          {customerId ? (
            <DataTable columns={orderColumns} rows={orders} rowKey={(r) => r.id} empty="Bạn chưa có đơn hàng nào." />
          ) : (
            <ProfilePrompt onSubmit={submitProfile} busy={busy} fullName={fullName} setFullName={setFullName} phone={phone} setPhone={setPhone} address={address} setAddress={setAddress} email={user?.email ?? ''} />
          )}
        </div>
      )}

      {tab === 'test-drives' && (
        <div>
          {customerId ? (
            <DataTable columns={testDriveColumns} rows={testDrives} rowKey={(r) => r.id} empty="Bạn chưa có lịch lái thử nào." />
          ) : (
            <ProfilePrompt onSubmit={submitProfile} busy={busy} fullName={fullName} setFullName={setFullName} phone={phone} setPhone={setPhone} address={address} setAddress={setAddress} email={user?.email ?? ''} />
          )}
        </div>
      )}

      {tab === 'profile' && (
        <div>
          {profile ? (
            <div className="rounded-2xl border border-[#e5e7eb] bg-white p-6 max-w-xl space-y-3 text-sm">
              <Row label="Mã khách hàng" value={`#${profile.id}`} />
              <Row label="Họ và tên" value={profile.fullName} />
              <Row label="Điện thoại" value={profile.phone ?? '—'} />
              <Row label="Email" value={profile.email ?? '—'} />
              <Row label="Địa chỉ" value={profile.address ?? '—'} />
              <Row label="CMND/CCCD" value={profile.nationalId ?? '—'} />
            </div>
          ) : (
            <ProfilePrompt onSubmit={submitProfile} busy={busy} fullName={fullName} setFullName={setFullName} phone={phone} setPhone={setPhone} address={address} setAddress={setAddress} email={user?.email ?? ''} />
          )}
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

interface ProfilePromptProps {
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

function ProfilePrompt({
  onSubmit,
  busy,
  fullName,
  setFullName,
  phone,
  setPhone,
  address,
  setAddress,
  email,
}: ProfilePromptProps) {
  return (
    <div className="rounded-2xl border border-[#e5e7eb] bg-white p-6 max-w-xl">
      <h2 className="font-serif font-bold text-lg mb-1">Tạo hồ sơ khách hàng</h2>
      <p className="text-sm text-[#6b7280] mb-5">
        Bạn cần có hồ sơ khách hàng để đặt hàng và đăng ký lái thử.
      </p>
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
