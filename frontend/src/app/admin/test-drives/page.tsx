'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Modal from '@/components/ui/Modal';
import Button from '@/components/ui/Button';
import StatusPill from '@/components/ui/StatusPill';
import {
  listTestDrives, updateTestDrive, listCars, listCarItems,
  listCustomers, listEmployees,
} from '@/lib/api';
import type { AppointmentStatus, CarItem, TestDrive, TestDriveRequest } from '@/types';

type Option = { value: number; label: string };
const WEEKDAYS = ['Thứ 2', 'Thứ 3', 'Thứ 4', 'Thứ 5', 'Thứ 6', 'Thứ 7', 'Chủ nhật'];
const STATUSES: { value: AppointmentStatus; label: string }[] = [
  { value: 'SCHEDULED', label: 'Đã lên lịch' },
  { value: 'CONFIRMED', label: 'Đã xác nhận' },
  { value: 'COMPLETED', label: 'Hoàn tất' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];
const pad = (n: number) => String(n).padStart(2, '0');
const keyOf = (d: Date) => d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
const dayOnly = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate());
const dateLabel = (d: Date) => new Intl.DateTimeFormat('vi-VN', {
  weekday: 'long', day: '2-digit', month: 'long', year: 'numeric',
}).format(d);
const timeLabel = (value: string) => new Intl.DateTimeFormat('vi-VN', {
  hour: '2-digit', minute: '2-digit', hour12: false,
}).format(new Date(value));
const localInputValue = (value: string) => {
  const d = new Date(value);
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
    'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
};
const monthCells = (month: Date) => {
  const first = new Date(month.getFullYear(), month.getMonth(), 1);
  const offset = (first.getDay() + 6) % 7;
  const start = new Date(first.getFullYear(), first.getMonth(), first.getDate() - offset);
  return Array.from({ length: 42 }, (_, i) =>
    new Date(start.getFullYear(), start.getMonth(), start.getDate() + i));
};
const dotClass = (status: AppointmentStatus) => {
  if (status === 'CANCELLED') return 'bg-red-500';
  if (status === 'SCHEDULED' || status === 'CONFIRMED') return 'bg-sky-500';
  return 'bg-emerald-500';
};

export default function AdminTestDrivesPage() {
  const [drives, setDrives] = useState<TestDrive[]>([]);
  const [customers, setCustomers] = useState<Option[]>([]);
  const [employees, setEmployees] = useState<Option[]>([]);
  const [carItems, setCarItems] = useState<Option[]>([]);
  const [month, setMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const [editing, setEditing] = useState<TestDrive | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [editError, setEditError] = useState('');
  const [form, setForm] = useState({
    scheduledAt: '', status: 'SCHEDULED' as AppointmentStatus,
    customerId: '', carItemId: '', employeeId: '', note: '',
  });

  const reload = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setDrives(await listTestDrives());
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thể tải lịch lái thử.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
    listCustomers()
      .then(rows => setCustomers(rows.map(c => ({ value: c.id, label: c.fullName + (c.phone ? ' · ' + c.phone : '') }))))
      .catch(() => setCustomers([]));
    listEmployees()
      .then(rows => setEmployees(rows.map(e => ({ value: e.id, label: e.fullName + (e.position ? ' · ' + e.position : '') }))))
      .catch(() => setEmployees([]));
    listCars()
      .then(async cars => {
        const lists = await Promise.all(cars.map(c => listCarItems(c.id).catch(() => [])));
        const all: CarItem[] = lists.flat();
        setCarItems(all.map(item => ({
          value: item.id, label: item.carName + ' • ' + (item.vin ?? '#' + item.id),
        })));
      })
      .catch(() => setCarItems([]));
  }, [reload]);

  const cells = useMemo(() => monthCells(month), [month]);
  const byDate = useMemo(() => {
    const map = new Map<string, TestDrive[]>();
    drives.forEach(drive => {
      const key = keyOf(new Date(drive.scheduledAt));
      map.set(key, [...(map.get(key) ?? []), drive]);
    });
    map.forEach(rows => rows.sort((a, b) =>
      new Date(a.scheduledAt).getTime() - new Date(b.scheduledAt).getTime()));
    return map;
  }, [drives]);

  const selectedDrives = selectedDate ? byDate.get(keyOf(selectedDate)) ?? [] : [];
  const today = dayOnly(new Date());
  const monthTitle = new Intl.DateTimeFormat('vi-VN', { month: 'long', year: 'numeric' }).format(month);
  const monthDrives = drives.filter(d => {
    const date = new Date(d.scheduledAt);
    return date.getMonth() === month.getMonth() && date.getFullYear() === month.getFullYear();
  });

  function editDrive(drive: TestDrive) {
    setEditing(drive);
    setEditError('');
    setForm({
      scheduledAt: localInputValue(drive.scheduledAt),
      status: drive.status,
      customerId: String(drive.customerId),
      carItemId: String(drive.carItemId),
      employeeId: drive.employeeId == null ? '' : String(drive.employeeId),
      note: drive.note ?? '',
    });
  }

  async function saveDrive(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editing) return;
    setSaving(true);
    setEditError('');
    try {
      const payload: TestDriveRequest = {
        scheduledAt: new Date(form.scheduledAt).toISOString(),
        status: form.status,
        customerId: Number(form.customerId),
        carItemId: Number(form.carItemId),
        employeeId: form.employeeId ? Number(form.employeeId) : null,
        note: form.note.trim() || null,
      };
      const updated = await updateTestDrive(editing.id, payload);
      setDrives(current => current.map(d => d.id === updated.id ? updated : d));
      setEditing(null);
    } catch (e) {
      setEditError(e instanceof Error ? e.message : 'Không thể cập nhật lịch.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-[#c58e00]">Quản lý vận hành</p>
          <h1 className="mt-1 text-2xl font-bold text-[#111827]">Lịch lái thử</h1>
          <p className="mt-1 text-sm text-[#6b7280]">Theo dõi và quản lý lịch hẹn theo tháng.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Button size="sm" variant="outline" onClick={() => {
            const now = new Date();
            setMonth(new Date(now.getFullYear(), now.getMonth(), 1));
          }}>Hôm nay</Button>
          <div className="flex items-center rounded-xl border border-[#e5e7eb] bg-white p-1">
            <button type="button" aria-label="Tháng trước"
              onClick={() => setMonth(m => new Date(m.getFullYear(), m.getMonth() - 1, 1))}
              className="rounded-lg px-3 py-2 text-lg text-[#4b5563] hover:bg-[#f3f4f6]">‹</button>
            <span className="min-w-36 px-2 text-center text-sm font-semibold capitalize text-[#111827]">{monthTitle}</span>
            <button type="button" aria-label="Tháng sau"
              onClick={() => setMonth(m => new Date(m.getFullYear(), m.getMonth() + 1, 1))}
              className="rounded-lg px-3 py-2 text-lg text-[#4b5563] hover:bg-[#f3f4f6]">›</button>
          </div>
          <Button size="sm" variant="outline" disabled={loading} onClick={() => void reload()}>Làm mới</Button>
        </div>
      </header>

      <div className="flex flex-wrap gap-x-5 gap-y-2 rounded-xl border border-[#e5e7eb] bg-white px-4 py-3 text-xs text-[#6b7280]">
        <span className="inline-flex items-center gap-2"><i className="h-3 w-3 rounded-sm border-2 border-red-500" /> Ngày đã qua</span>
        <span className="inline-flex items-center gap-2"><i className="h-3 w-3 rounded-sm border-2 border-white bg-[#111827]" /> Hôm nay</span>
        <span className="inline-flex items-center gap-2"><i className="h-3 w-3 rounded-sm border-2 border-emerald-500" /> Ngày sắp tới</span>
        <span className="inline-flex items-center gap-2"><i className="h-2 w-2 rounded-full bg-sky-500" /> Đang chờ / xác nhận</span>
        <span className="inline-flex items-center gap-2"><i className="h-2 w-2 rounded-full bg-red-500" /> Đã hủy</span>
      </div>

      {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>}

      <section className="overflow-hidden rounded-2xl border border-[#e5e7eb] bg-white shadow-sm">
        <div className="grid grid-cols-7 border-b border-[#e5e7eb] bg-[#f9fafb]">
          {WEEKDAYS.map(day => <div key={day} className="px-1 py-3 text-center text-[10px] font-semibold uppercase tracking-wide text-[#6b7280] sm:px-3 sm:text-xs">{day}</div>)}
        </div>
        {loading ? (
          <div className="flex min-h-72 items-center justify-center text-sm text-[#6b7280]">Đang tải lịch lái thử...</div>
        ) : (
          <div className="grid grid-cols-7">
            {cells.map((date, index) => {
              const dayDrives = byDate.get(keyOf(date)) ?? [];
              const inMonth = date.getMonth() === month.getMonth();
              const day = dayOnly(date);
              const isToday = day.getTime() === today.getTime();
              const isPast = day.getTime() < today.getTime();
              const outline = isPast ? 'border-red-500' : isToday ? 'border-white' : 'border-emerald-500';
              return (
                <button type="button" key={keyOf(date)} onClick={() => setSelectedDate(date)}
                  aria-label={dateLabel(date) + ', ' + dayDrives.length + ' lịch lái thử'}
                  className={'group flex min-h-24 flex-col items-start border-b border-r border-[#e5e7eb] p-1.5 text-left transition hover:bg-[#f9fafb] focus:z-10 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-[#c58e00] sm:min-h-28 sm:p-2.5 ' +
                    (index % 7 === 6 ? 'border-r-0 ' : '') + (!inMonth ? 'bg-[#fafafa]' : 'bg-white')}>
                  <span className={'flex h-7 w-7 items-center justify-center rounded-full border-2 text-xs font-semibold sm:h-8 sm:w-8 sm:text-sm ' +
                    outline + ' ' + (isToday ? 'bg-[#111827] text-white' : 'text-[#111827]') + (!inMonth ? ' opacity-40' : '')}>
                    {date.getDate()}
                  </span>
                  <span className="mt-2 flex flex-wrap items-center gap-1">
                    {dayDrives.slice(0, 8).map(d => <i key={d.id} title={timeLabel(d.scheduledAt) + ' · ' + d.status} className={'h-2 w-2 rounded-full ' + dotClass(d.status)} />)}
                    {dayDrives.length > 8 && <span className="text-[10px] text-[#6b7280]">+{dayDrives.length - 8}</span>}
                  </span>
                  {dayDrives.length > 0 && <span className="mt-auto hidden pt-2 text-[10px] font-medium text-[#6b7280] sm:block">{dayDrives.length} lịch hẹn</span>}
                </button>
              );
            })}
          </div>
        )}
      </section>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="rounded-xl border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs text-[#6b7280]">Tổng lịch trong tháng</p>
          <p className="mt-1 text-2xl font-bold text-[#111827]">{monthDrives.length}</p>
        </div>
        <div className="rounded-xl border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs text-[#6b7280]">Đang chờ / xác nhận</p>
          <p className="mt-1 text-2xl font-bold text-sky-600">{monthDrives.filter(d => d.status === 'SCHEDULED' || d.status === 'CONFIRMED').length}</p>
        </div>
        <div className="rounded-xl border border-[#e5e7eb] bg-white p-4">
          <p className="text-xs text-[#6b7280]">Đã hủy</p>
          <p className="mt-1 text-2xl font-bold text-red-600">{monthDrives.filter(d => d.status === 'CANCELLED').length}</p>
        </div>
      </div>

      <Modal open={selectedDate !== null}
        title={selectedDate ? 'Lịch lái thử · ' + dateLabel(selectedDate) : undefined}
        onClose={() => setSelectedDate(null)} size="lg">
        {selectedDate && <div className="space-y-4">
          {selectedDrives.length === 0 ? (
            <div className="rounded-xl border border-dashed border-[#d1d5db] bg-[#f9fafb] px-4 py-10 text-center">
              <p className="font-medium text-[#374151]">Chưa có lịch lái thử</p>
              <p className="mt-1 text-sm text-[#6b7280]">Ngày này hiện chưa có lịch hẹn nào.</p>
            </div>
          ) : selectedDrives.map(drive => (
            <article key={drive.id} className="rounded-xl border border-[#e5e7eb] p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <strong className="text-base text-[#111827]">{timeLabel(drive.scheduledAt)}</strong>
                    <StatusPill status={drive.status} />
                  </div>
                  <p className="mt-2 text-sm font-semibold text-[#374151]">{drive.customerName}</p>
                  <p className="mt-1 text-sm text-[#6b7280]">{drive.carName}{drive.vin ? ' · ' + drive.vin : ''}</p>
                  <p className="mt-1 text-xs text-[#9ca3af]">Nhân viên: {drive.employeeName ?? 'Chưa phân công'}</p>
                  {drive.note && <p className="mt-2 whitespace-pre-wrap text-sm text-[#6b7280]">{drive.note}</p>}
                </div>
                <Button size="sm" variant="outline" onClick={() => editDrive(drive)}>Cập nhật</Button>
              </div>
            </article>
          ))}
        </div>}
      </Modal>

      <Modal open={editing !== null} title={editing ? 'Cập nhật lịch #' + editing.id : undefined}
        onClose={() => !saving && setEditing(null)} size="lg"
        footer={<>
          <Button variant="outline" disabled={saving} onClick={() => setEditing(null)}>Đóng</Button>
          <Button type="submit" form="test-drive-edit-form" loading={saving}>Lưu thay đổi</Button>
        </>}>
        <form id="test-drive-edit-form" onSubmit={saveDrive} className="space-y-4">
          {editError && <div role="alert" className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">{editError}</div>}
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block text-sm font-medium text-[#374151]">Thời gian hẹn
              <input required type="datetime-local" value={form.scheduledAt}
                onChange={e => setForm(f => ({ ...f, scheduledAt: e.target.value }))}
                className="mt-1.5 w-full rounded-xl border border-[#d1d5db] px-3 py-2.5 text-sm outline-none focus:border-[#c58e00]" />
            </label>
            <label className="block text-sm font-medium text-[#374151]">Trạng thái
              <select value={form.status} onChange={e => setForm(f => ({ ...f, status: e.target.value as AppointmentStatus }))}
                className="mt-1.5 w-full rounded-xl border border-[#d1d5db] bg-white px-3 py-2.5 text-sm">
                {STATUSES.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </label>
            <label className="block text-sm font-medium text-[#374151]">Khách hàng
              <select required value={form.customerId} onChange={e => setForm(f => ({ ...f, customerId: e.target.value }))}
                className="mt-1.5 w-full rounded-xl border border-[#d1d5db] bg-white px-3 py-2.5 text-sm">
                {customers.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </label>
            <label className="block text-sm font-medium text-[#374151]">Xe lái thử
              <select required value={form.carItemId} onChange={e => setForm(f => ({ ...f, carItemId: e.target.value }))}
                className="mt-1.5 w-full rounded-xl border border-[#d1d5db] bg-white px-3 py-2.5 text-sm">
                {carItems.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </label>
            <label className="block text-sm font-medium text-[#374151] sm:col-span-2">Nhân viên phụ trách
              <select value={form.employeeId} onChange={e => setForm(f => ({ ...f, employeeId: e.target.value }))}
                className="mt-1.5 w-full rounded-xl border border-[#d1d5db] bg-white px-3 py-2.5 text-sm">
                <option value="">Chưa phân công</option>
                {employees.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </label>
            <label className="block text-sm font-medium text-[#374151] sm:col-span-2">Ghi chú
              <textarea rows={3} value={form.note} onChange={e => setForm(f => ({ ...f, note: e.target.value }))}
                className="mt-1.5 w-full rounded-xl border border-[#d1d5db] px-3 py-2.5 text-sm outline-none focus:border-[#c58e00]" />
            </label>
          </div>
        </form>
      </Modal>
    </div>
  );
}
