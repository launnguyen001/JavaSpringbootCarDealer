'use client';

import { useCallback, useEffect, useState } from 'react';
import Button from '@/components/ui/Button';
import DataTable, { type Column } from '@/components/ui/DataTable';
import { Select } from '@/components/ui/Field';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/context/AuthContext';
import { getErrorMessage } from '@/lib/utils/error';
import { formatDateTime } from '@/lib/utils/format';
import { listUsers, updateUserStatus, updateUserRole } from '@/lib/api';
import type { AuthUser, UserRole } from '@/types';

const ROOT_ADMIN = 'admin';

const roleOptions: { value: UserRole; label: string }[] = [
  { value: 'ADMIN', label: 'Quản trị' },
  { value: 'EMPLOYEE', label: 'Nhân viên' },
  { value: 'USER', label: 'Người dùng' },
];

export default function AdminUsersPage() {
  const toast = useToast();
  const { user } = useAuth();
  const [users, setUsers] = useState<AuthUser[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setUsers(await listUsers());
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const changeRole = async (u: AuthUser, role: UserRole) => {
    if (role === u.role) return;
    setBusyId(u.id);
    try {
      const updated = await updateUserRole(u.id, role);
      setUsers((prev) => prev.map((x) => (x.id === updated.id ? updated : x)));
      toast.success('Đã đổi vai trò');
    } catch (err) {
      toast.error(getErrorMessage(err));
      await load();
    } finally {
      setBusyId(null);
    }
  };

  const toggleStatus = async (u: AuthUser) => {
    setBusyId(u.id);
    try {
      const updated = await updateUserStatus(u.id, !u.enabled);
      setUsers((prev) => prev.map((x) => (x.id === updated.id ? updated : x)));
      toast.success(updated.enabled ? 'Đã mở khoá tài khoản' : 'Đã khoá tài khoản');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  };

  const columns: Column<AuthUser>[] = [
    { key: 'id', label: 'ID', className: 'text-center' },
    {
      key: 'username',
      label: 'Tên đăng nhập',
      render: (r) => (
        <span className="font-medium text-[#0a0a0a]">
          {r.username}
          {r.username === ROOT_ADMIN && (
            <span className="ml-2 text-[10px] uppercase tracking-wider text-[#c58e00]">
              gốc
            </span>
          )}
        </span>
      ),
    },
    { key: 'email', label: 'Email', render: (r) => r.email ?? '—' },
    {
      key: 'role',
      label: 'Vai trò',
      render: (r) => {
        const isRoot = r.username === ROOT_ADMIN;
        const isSelf = user?.id === r.id;
        return (
          <Select
            value={r.role}
            disabled={busyId === r.id || isRoot || isSelf}
            onChange={(e) => changeRole(r, e.target.value as UserRole)}
            className="!py-1 !px-2 !text-xs !w-32"
          >
            {roleOptions.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </Select>
        );
      },
    },
    {
      key: 'enabled',
      label: 'Trạng thái',
      render: (r) => (
        <span
          className={`inline-flex items-center px-2.5 py-1 rounded-full text-[10px] font-medium uppercase tracking-wide border ${
            r.enabled
              ? 'bg-[#ecfdf5] text-[#10b981] border-[#10b981]/30'
              : 'bg-[#fef2f2] text-[#ef4444] border-[#ef4444]/30'
          }`}
        >
          {r.enabled ? 'Hoạt động' : 'Đã khoá'}
        </span>
      ),
    },
    {
      key: 'createdAt',
      label: 'Ngày tạo',
      render: (r) => (r.createdAt ? formatDateTime(r.createdAt) : '—'),
    },
    {
      key: '__actions',
      label: 'Thao tác',
      className: 'text-right whitespace-nowrap',
      render: (r) => {
        const isRoot = r.username === ROOT_ADMIN;
        const isSelf = user?.id === r.id;
        const disabled = busyId === r.id || isRoot || isSelf;
        return (
          <Button
            size="sm"
            variant={r.enabled ? 'outline' : 'gold'}
            disabled={disabled}
            onClick={() => toggleStatus(r)}
            title={isSelf ? 'Không thể tự khoá' : isRoot ? 'Không thể khoá tài khoản gốc' : ''}
          >
            {r.enabled ? 'Khoá' : 'Mở khoá'}
          </Button>
        );
      },
    },
  ];

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-serif font-bold text-[#0a0a0a]">Người dùng</h1>
          <p className="text-sm text-[#6b7280] mt-1">
            Quản lý tài khoản: đổi vai trò, khoá/mở khoá. Không thể tự khoá hoặc hạ quyền tài khoản gốc.
          </p>
        </div>
        <Button variant="ghost" size="sm" onClick={load} title="Tải lại">
          ↻
        </Button>
      </div>

      {loading ? (
        <div className="py-16 text-center text-[#9ca3af] text-sm">Đang tải…</div>
      ) : (
        <DataTable columns={columns} rows={users} rowKey={(r) => r.id} />
      )}
    </div>
  );
}
