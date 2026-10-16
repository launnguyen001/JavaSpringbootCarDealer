'use client';

import { useCallback, useEffect, useState } from 'react';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import { Field, Input, Select } from '@/components/ui/Field';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import {
  listOrders,
  createOrder,
  updateOrderStatus,
  cancelOrder,
  deleteOrder,
} from '@/lib/api';
import type { Order, OrderStatus } from '@/types';

const statuses: OrderStatus[] = ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'];

interface ItemRow {
  carId: string;
  quantity: string;
}

export default function AdminOrdersPage() {
  const toast = useToast();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Order | null>(null);

  const [customerId, setCustomerId] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [items, setItems] = useState<ItemRow[]>([{ carId: '', quantity: '1' }]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setOrders(await listOrders());
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const changeStatus = async (order: Order, status: OrderStatus) => {
    try {
      await updateOrderStatus(order.id, status);
      toast.success('Đã cập nhật trạng thái');
      await load();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const doCancel = async (order: Order) => {
    try {
      await cancelOrder(order.id);
      toast.success('Đã hủy đơn');
      await load();
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const doDelete = async () => {
    if (!confirmDelete) return;
    setSubmitting(true);
    try {
      await deleteOrder(confirmDelete.id);
      toast.success('Đã xóa đơn');
      setConfirmDelete(null);
      await load();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const openCreate = () => {
    setCustomerId('');
    setEmployeeId('');
    setItems([{ carId: '', quantity: '1' }]);
    setOpen(true);
  };

  const submitCreate = async () => {
    if (!customerId) {
      toast.error('Vui lòng nhập ID khách hàng');
      return;
    }
    const parsed = items
      .filter((i) => i.carId)
      .map((i) => ({ carId: Number(i.carId), quantity: Number(i.quantity) || 1 }));
    if (parsed.length === 0) {
      toast.error('Cần ít nhất một xe trong đơn');
      return;
    }
    setSubmitting(true);
    try {
      await createOrder({
        customerId: Number(customerId),
        employeeId: employeeId ? Number(employeeId) : null,
        items: parsed,
      });
      toast.success('Tạo đơn hàng thành công');
      setOpen(false);
      await load();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const columns: Column<Order>[] = [
    { key: 'orderCode', label: 'Mã đơn' },
    { key: 'customerName', label: 'Khách hàng', render: (r) => r.customerName ?? '—' },
    { key: 'orderDate', label: 'Ngày', render: (r) => formatDateTime(r.orderDate) },
    {
      key: 'totalAmount',
      label: 'Tổng tiền',
      className: 'text-right',
      render: (r) => formatVnd(r.totalAmount),
    },
    {
      key: 'items',
      label: 'Mặt hàng',
      className: 'text-center',
      render: (r) => r.items?.length ?? 0,
    },
    { key: 'status', label: 'Trạng thái', render: (r) => <StatusPill status={r.status} /> },
    {
      key: '__actions',
      label: 'Thao tác',
      className: 'text-right whitespace-nowrap',
      render: (r) => (
        <div className="flex justify-end items-center gap-2">
          <Select
            value={r.status}
            onChange={(e) => changeStatus(r, e.target.value as OrderStatus)}
            className="!py-1 !px-2 !text-xs !w-32"
          >
            {statuses.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </Select>
          {r.status !== 'CANCELLED' && (
            <Button size="sm" variant="outline" onClick={() => doCancel(r)}>
              Hủy
            </Button>
          )}
          <Button size="sm" variant="danger" onClick={() => setConfirmDelete(r)}>
            Xóa
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-serif font-bold text-[#0a0a0a]">Đơn hàng</h1>
          <p className="text-sm text-[#6b7280] mt-1">Quản lý đơn hàng, đổi trạng thái, hủy đơn.</p>
        </div>
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={load} title="Tải lại">
            ↻
          </Button>
          <Button variant="gold" onClick={openCreate}>
            + Tạo đơn
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="py-16 text-center text-[#9ca3af] text-sm">Đang tải…</div>
      ) : (
        <DataTable columns={columns} rows={orders} rowKey={(r) => r.id} />
      )}

      <Modal
        open={open}
        title="Tạo đơn hàng"
        onClose={() => setOpen(false)}
        size="lg"
        footer={
          <>
            <Button variant="outline" onClick={() => setOpen(false)}>
              Hủy
            </Button>
            <Button variant="gold" onClick={submitCreate} loading={submitting}>
              Tạo đơn
            </Button>
          </>
        }
      >
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <Field label="ID khách hàng" required>
            <Input
              type="number"
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value)}
              placeholder="VD: 1"
            />
          </Field>
          <Field label="ID nhân viên phụ trách">
            <Input
              type="number"
              value={employeeId}
              onChange={(e) => setEmployeeId(e.target.value)}
              placeholder="Có thể bỏ trống"
            />
          </Field>
        </div>

        <div className="mt-5">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-semibold text-[#6b7280] uppercase tracking-wider">
              Mặt hàng
            </span>
            <Button
              size="sm"
              variant="outline"
              onClick={() => setItems((prev) => [...prev, { carId: '', quantity: '1' }])}
            >
              + Thêm xe
            </Button>
          </div>
          <div className="space-y-3">
            {items.map((item, index) => (
              <div key={index} className="flex items-center gap-3">
                <Input
                  type="number"
                  value={item.carId}
                  placeholder="ID xe"
                  onChange={(e) =>
                    setItems((prev) =>
                      prev.map((it, i) => (i === index ? { ...it, carId: e.target.value } : it)),
                    )
                  }
                />
                <Input
                  type="number"
                  value={item.quantity}
                  placeholder="SL"
                  className="w-28"
                  onChange={(e) =>
                    setItems((prev) =>
                      prev.map((it, i) => (i === index ? { ...it, quantity: e.target.value } : it)),
                    )
                  }
                />
                {items.length > 1 && (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() => setItems((prev) => prev.filter((_, i) => i !== index))}
                  >
                    ✕
                  </Button>
                )}
              </div>
            ))}
          </div>
        </div>
      </Modal>

      <Modal
        open={!!confirmDelete}
        title="Xác nhận xóa"
        onClose={() => setConfirmDelete(null)}
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => setConfirmDelete(null)}>
              Hủy
            </Button>
            <Button variant="danger" onClick={doDelete} loading={submitting}>
              Xóa
            </Button>
          </>
        }
      >
        <p className="text-sm text-[#374151]">
          Bạn có chắc muốn xóa đơn <b>{confirmDelete?.orderCode}</b>? Hành động không thể hoàn tác.
        </p>
      </Modal>
    </div>
  );
}
