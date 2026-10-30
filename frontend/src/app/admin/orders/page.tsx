'use client';

import { useCallback, useEffect, useState } from 'react';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import { Field, Input, Select } from '@/components/ui/Field';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
import OrderDetailModal from '@/components/admin/OrderDetailModal';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import {
  listOrders,
  createOrder,
  updateOrderStatus,
  cancelOrder,
  deleteOrder,
  listCars,
  listAvailableCarItems,
  getOrderStateMachine,
} from '@/lib/api';
import type { Car, CarItem, Order, OrderStatus } from '@/types';

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Cũ' };

interface ItemRow {
  carId: string;
  carItemId: string;
}

export default function AdminOrdersPage() {
  const toast = useToast();
  const [orders, setOrders] = useState<Order[]>([]);
  const [cars, setCars] = useState<Car[]>([]);
  const [transitions, setTransitions] = useState<Record<OrderStatus, OrderStatus[]> | null>(null);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Order | null>(null);
  // Đơn đang xem chi tiết trong popup (gồm hợp đồng / thanh toán / hóa đơn)
  const [detailId, setDetailId] = useState<number | null>(null);

  const [customerId, setCustomerId] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [items, setItems] = useState<ItemRow[]>([{ carId: '', carItemId: '' }]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [ordersData, machine] = await Promise.all([
        listOrders(),
        getOrderStateMachine().catch(() => null),
      ]);
      setOrders(ordersData);
      setTransitions(machine);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
    listCars().then(setCars).catch(() => setCars([]));
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
    setItems([{ carId: '', carItemId: '' }]);
    setOpen(true);
  };

  const submitCreate = async () => {
    if (!customerId) {
      toast.error('Vui lòng nhập ID khách hàng');
      return;
    }
    // Mỗi dòng đơn là một chiếc xe cụ thể (car_item), số lượng luôn = 1.
    const parsed = items.filter((i) => i.carItemId).map((i) => ({ carItemId: Number(i.carItemId) }));
    if (parsed.length === 0) {
      toast.error('Cần chọn ít nhất một chiếc xe');
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
          <Button size="sm" variant="gold" onClick={() => setDetailId(r.id)}>
            Chi tiết
          </Button>
          <Select
            value={r.status}
            disabled={!transitions || (transitions[r.status] ?? []).filter((s) => s !== 'CANCELLED').length === 0}
            onChange={(e) => changeStatus(r, e.target.value as OrderStatus)}
            className="!py-1 !px-2 !text-xs !w-32"
            title="Chỉ các trạng thái hợp lệ theo state machine mới chọn được"
          >
            <option value={r.status}>{r.status}</option>
            {(transitions?.[r.status] ?? [])
              .filter((s) => s !== 'CANCELLED')
              .map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
          </Select>
          {(!transitions
            ? r.status !== 'CANCELLED'
            : (transitions[r.status] ?? []).includes('CANCELLED')) && (
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
          <p className="text-sm text-[#6b7280] mt-1">
            Quản lý đơn hàng, đổi trạng thái, hủy đơn — bấm “Chi tiết” để xem hợp đồng, thanh toán và hóa đơn của đơn.
          </p>
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
              Xe trong đơn (mỗi dòng 1 chiếc)
            </span>
            <Button
              size="sm"
              variant="outline"
              onClick={() => setItems((prev) => [...prev, { carId: '', carItemId: '' }])}
            >
              + Thêm xe
            </Button>
          </div>
          <div className="space-y-3">
            {items.map((item, index) => (
              <OrderItemRow
                key={index}
                cars={cars}
                value={item}
                canRemove={items.length > 1}
                onChange={(v) =>
                  setItems((prev) => prev.map((it, i) => (i === index ? v : it)))
                }
                onRemove={() => setItems((prev) => prev.filter((_, i) => i !== index))}
              />
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
          Bạn có chắc muốn xóa đơn <b>{confirmDelete?.orderCode}</b>? Hành động này không thể hoàn tác.
        </p>
      </Modal>

      {/* Chi tiết đơn: gom hợp đồng + thanh toán + hóa đơn vào 1 popup, expand từng mục */}
      <OrderDetailModal orderId={detailId} onClose={() => setDetailId(null)} />
    </div>
  );
}

function OrderItemRow({
  cars,
  value,
  onChange,
  onRemove,
  canRemove,
}: {
  cars: Car[];
  value: ItemRow;
  onChange: (v: ItemRow) => void;
  onRemove: () => void;
  canRemove: boolean;
}) {
  const [carItems, setCarItems] = useState<CarItem[]>([]);
  const [loadingItems, setLoadingItems] = useState(false);

  useEffect(() => {
    if (!value.carId) {
      setCarItems([]);
      return;
    }
    let active = true;
    setLoadingItems(true);
    listAvailableCarItems(Number(value.carId))
      .then((data) => {
        if (active) setCarItems(data);
      })
      .catch(() => {
        if (active) setCarItems([]);
      })
      .finally(() => {
        if (active) setLoadingItems(false);
      });
    return () => {
      active = false;
    };
  }, [value.carId]);

  return (
    <div className="flex items-center gap-3">
      <Select
        value={value.carId}
        onChange={(e) => onChange({ carId: e.target.value, carItemId: '' })}
      >
        <option value="">Chọn dòng xe…</option>
        {cars.map((c) => (
          <option key={c.id} value={c.id}>
            {c.name}
          </option>
        ))}
      </Select>
      <Select
        value={value.carItemId}
        disabled={!value.carId || loadingItems}
        onChange={(e) => onChange({ ...value, carItemId: e.target.value })}
      >
        <option value="">{loadingItems ? 'Đang tải…' : 'Chọn xe cụ thể…'}</option>
        {carItems.map((it) => (
          <option key={it.id} value={it.id}>
            {(it.vin ?? `#${it.id}`) +
              ' • ' +
              (CONDITION_LABEL[it.condition] ?? it.condition) +
              ' • ' +
              formatVnd(it.price)}
          </option>
        ))}
      </Select>
      <Button size="sm" variant="ghost" disabled={!canRemove} onClick={onRemove}>
        ✕
      </Button>
    </div>
  );
}
