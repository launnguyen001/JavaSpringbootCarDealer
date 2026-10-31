'use client';

import { Suspense, useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import { Field, Select } from '@/components/ui/Field';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
import OrderDetailModal from '@/components/admin/OrderDetailModal';
import ResourceFormModal from '@/components/admin/ResourceFormModal';
import { contractFields } from '@/components/admin/contractFields';
import { invoiceFields } from '@/components/admin/invoiceFields';
import { paymentFields } from '@/components/admin/paymentFields';
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
  listCustomers,
  listEmployees,
  listAvailableCarItems,
  getOrderStateMachine,
  listContracts,
  listInvoices,
  createContract,
  createInvoice,
  createPayment,
} from '@/lib/api';
import type { Car, CarItem, Customer, Employee, Invoice, Order, OrderStatus } from '@/types';

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Cũ' };

type DocType = 'contract' | 'invoice' | 'payment';

// Mỗi nút (Hợp đồng / Hóa đơn / Thu tiền) mở cùng một popup, chỉ khác field và hàm create
const DOC_FORMS = {
  contract: { title: 'Hợp đồng', fields: contractFields, create: createContract },
  invoice: { title: 'Hóa đơn', fields: invoiceFields, create: createInvoice },
  payment: { title: 'Thanh toán', fields: paymentFields, create: createPayment },
} as const;

function parseOrderId(value: string | null): number | null {
  const id = Number(value);
  return Number.isInteger(id) && id > 0 ? id : null;
}

interface ItemRow {
  uid: number; // id ổn định cho mỗi dòng, dùng làm React key
  carId: string;
  carItemId: string;
}

// useSearchParams cần nằm trong Suspense khi build
export default function AdminOrdersPage() {
  return (
    <Suspense fallback={<div className="py-16 text-center text-[#9ca3af] text-sm">Đang tải…</div>}>
      <AdminOrdersContent />
    </Suspense>
  );
}

function AdminOrdersContent() {
  const toast = useToast();
  const searchParams = useSearchParams();

  // Giữ toast trong ref để `load` không bị tạo lại khi useToast trả về object mới
  const toastRef = useRef(toast);
  useEffect(() => {
    toastRef.current = toast;
  }, [toast]);

  const uidCounter = useRef(1);
  const newRow = useCallback(
    (): ItemRow => ({ uid: uidCounter.current++, carId: '', carItemId: '' }),
    [],
  );

  const [orders, setOrders] = useState<Order[]>([]);
  const [cars, setCars] = useState<Car[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [transitions, setTransitions] = useState<Record<OrderStatus, OrderStatus[]> | null>(null);
  const [contractOrderIds, setContractOrderIds] = useState<Set<number>>(new Set());
  const [invoicesByOrder, setInvoicesByOrder] = useState<Map<number, Invoice>>(new Map());
  const [documentsLoaded, setDocumentsLoaded] = useState(false);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<Order | null>(null);
  // Đơn đang xem chi tiết trong popup (gồm hợp đồng / thanh toán / hóa đơn)
  const [detailId, setDetailId] = useState<number | null>(() =>
    parseOrderId(searchParams.get('orderId')),
  );
  const [detailAction, setDetailAction] = useState<'invoice' | 'payment' | null>(null);
  // Popup thêm hợp đồng / hóa đơn / thanh toán, mở ngay tại trang này
  const [formFor, setFormFor] = useState<{
    type: DocType;
    orderId: number;
    remaining?: number;
  } | null>(null);

  const [customerId, setCustomerId] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [items, setItems] = useState<ItemRow[]>([{ uid: 0, carId: '', carItemId: '' }]);

  // Chỉ gọi API, không đụng tới state: dùng được cả trong effect lẫn event handler
  const fetchData = useCallback(async () => {
    const [ordersData, machine, contractsResult, invoicesResult] = await Promise.all([
      listOrders(),
      getOrderStateMachine().catch(() => null),
      listContracts()
        .then((data) => ({ ok: true as const, data }))
        .catch(() => ({ ok: false as const, data: [] })),
      listInvoices()
        .then((data) => ({ ok: true as const, data }))
        .catch(() => ({ ok: false as const, data: [] })),
    ]);
    return { ordersData, machine, contractsResult, invoicesResult };
  }, []);

  const applyData = useCallback((result: Awaited<ReturnType<typeof fetchData>>) => {
    const { ordersData, machine, contractsResult, invoicesResult } = result;
    setOrders(ordersData);
    setTransitions(machine);
    if (contractsResult.ok && invoicesResult.ok) {
      setContractOrderIds(new Set(contractsResult.data.map((contract) => contract.orderId)));
      setInvoicesByOrder(new Map(invoicesResult.data.map((invoice) => [invoice.orderId, invoice])));
      setDocumentsLoaded(true);
    } else {
      // Không hiển thị nút tạo/thu nếu chưa xác định được chứng từ hiện có,
      // tránh tạo trùng hoặc thu tiền khi dữ liệu chứng từ chưa tải được.
      setDocumentsLoaded(false);
    }
  }, []);

  // Tải lại từ event handler (nút ↻, sau khi tạo / sửa / xóa)
  const load = useCallback(async () => {
    setLoading(true);
    try {
      applyData(await fetchData());
    } catch (err) {
      toastRef.current.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [fetchData, applyData]);

  // Tải lần đầu: setState chỉ nằm trong callback của promise
  useEffect(() => {
    let active = true;
    fetchData()
      .then((result) => {
        if (active) applyData(result);
      })
      .catch((err) => {
        if (active) toastRef.current.error(getErrorMessage(err));
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    listCars().then(setCars).catch(() => setCars([]));
    listCustomers().then(setCustomers).catch(() => setCustomers([]));
    listEmployees().then(setEmployees).catch(() => setEmployees([]));
    return () => {
      active = false;
    };
  }, [fetchData, applyData]);

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
    setItems([newRow()]);
    setOpen(true);
  };

  const submitCreate = async () => {
    if (!customerId) {
      toast.error('Vui lòng chọn khách hàng');
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
      render: (r) => {
        const invoice = invoicesByOrder.get(r.id);
        const hasContract = contractOrderIds.has(r.id);
        const canCollect =
          hasContract &&
          !!invoice &&
          invoice.paymentStatus !== 'PAID' &&
          invoice.paymentStatus !== 'CANCELLED' &&
          invoice.remainingAmount > 0.0001;
        const nextStatuses = (transitions?.[r.status] ?? []).filter(
          (s) => s !== 'CANCELLED' && s !== 'COMPLETED',
        );
        const canCancel = !transitions
          ? r.status !== 'CANCELLED'
          : (transitions[r.status] ?? []).includes('CANCELLED');

        return (
          <div className="flex justify-end items-center gap-2">
            <Button size="sm" variant="gold" onClick={() => setDetailId(r.id)}>
              Chi tiết
            </Button>
            {r.status === 'CONFIRMED' && documentsLoaded && (
              <>
                {!hasContract && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setFormFor({ type: 'contract', orderId: r.id })}
                  >
                    Hợp đồng
                  </Button>
                )}
                {!invoice && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setFormFor({ type: 'invoice', orderId: r.id })}
                  >
                    Hóa đơn
                  </Button>
                )}
                {canCollect && (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() =>
                      setFormFor({
                        type: 'payment',
                        orderId: r.id,
                        remaining: invoice?.remainingAmount,
                      })
                    }
                  >
                    Thu tiền
                  </Button>
                )}
              </>
            )}
            <Select
              value={r.status}
              disabled={!transitions || nextStatuses.length === 0}
              onChange={(e) => changeStatus(r, e.target.value as OrderStatus)}
              className="!py-1 !px-2 !text-xs !w-32"
              title="Chỉ các trạng thái hợp lệ theo state machine mới chọn được"
            >
              <option value={r.status}>{r.status}</option>
              {nextStatuses.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </Select>
            {canCancel && (
              <Button size="sm" variant="outline" onClick={() => doCancel(r)}>
                Hủy
              </Button>
            )}
            <Button size="sm" variant="danger" onClick={() => setConfirmDelete(r)}>
              Xóa
            </Button>
          </div>
        );
      },
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
          <Field label="Khách hàng" required>
            <Select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
              <option value="">— Chọn khách hàng —</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.fullName}
                  {customer.phone ? ` · ${customer.phone}` : ''}
                  {customer.email ? ` · ${customer.email}` : ''}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="Nhân viên phụ trách">
            <Select value={employeeId} onChange={(e) => setEmployeeId(e.target.value)}>
              <option value="">— Chưa phân công —</option>
              {employees.map((employee) => (
                <option key={employee.id} value={employee.id}>
                  {employee.fullName}
                  {employee.position ? ` · ${employee.position}` : ''}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <div className="mt-5">
          <div className="flex items-center justify-between mb-2">
            <span className="text-[11px] font-semibold text-[#6b7280] uppercase tracking-wider">
              Xe trong đơn (mỗi dòng 1 chiếc)
            </span>
            <Button size="sm" variant="outline" onClick={() => setItems((prev) => [...prev, newRow()])}>
              + Thêm xe
            </Button>
          </div>
          <div className="space-y-3">
            {items.map((item) => {
              // Các xe đã được chọn ở những dòng khác -> ẩn khỏi danh sách dòng này
              const takenIds = new Set(
                items.filter((it) => it.uid !== item.uid && it.carItemId).map((it) => it.carItemId),
              );
              return (
                <OrderItemRow
                  key={item.uid}
                  cars={cars}
                  value={item}
                  takenIds={takenIds}
                  canRemove={items.length > 1}
                  onChange={(v) => setItems((prev) => prev.map((it) => (it.uid === item.uid ? v : it)))}
                  onRemove={() => setItems((prev) => prev.filter((it) => it.uid !== item.uid))}
                />
              );
            })}
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

      {/* Popup thêm hợp đồng / hóa đơn / thanh toán: orderId điền sẵn và ẩn */}
      {formFor && (
        <ResourceFormModal
          open
          title={DOC_FORMS[formFor.type].title}
          fields={DOC_FORMS[formFor.type].fields}
          hiddenFields={['orderId']}
          initialValues={{
            orderId: String(formFor.orderId),
            // Thu tiền: gợi ý số tiền = số còn phải thu của hóa đơn
            ...(formFor.type === 'payment' && formFor.remaining && formFor.remaining > 0
              ? { amount: formFor.remaining }
              : {}),
          }}
          create={DOC_FORMS[formFor.type].create}
          onClose={() => setFormFor(null)}
          onSuccess={() => {
            void load();
          }}
        />
      )}

      {/* Chi tiết đơn: gom hợp đồng + thanh toán + hóa đơn vào 1 popup, expand từng mục */}
      <OrderDetailModal
        orderId={detailId}
        initialAction={detailAction}
        onClose={() => {
          setDetailId(null);
          setDetailAction(null);
        }}
        onActionSuccess={() => {
          void load();
        }}
      />
    </div>
  );
}

function OrderItemRow({
  cars,
  value,
  takenIds,
  onChange,
  onRemove,
  canRemove,
}: {
  cars: Car[];
  value: ItemRow;
  takenIds: Set<string>;
  onChange: (v: ItemRow) => void;
  onRemove: () => void;
  canRemove: boolean;
}) {
  // Lưu kèm carId để biết dữ liệu đã tải thuộc dòng xe nào -> suy ra trạng thái loading,
  // không cần setState đồng bộ trong effect.
  const [loaded, setLoaded] = useState<{ carId: string; items: CarItem[] } | null>(null);

  useEffect(() => {
    if (!value.carId) return;
    let active = true;
    const carId = value.carId;
    listAvailableCarItems(Number(carId))
      .then((data) => {
        if (active) setLoaded({ carId, items: data });
      })
      .catch(() => {
        if (active) setLoaded({ carId, items: [] });
      });
    return () => {
      active = false;
    };
  }, [value.carId]);

  const ready = !!value.carId && loaded?.carId === value.carId;
  const loadingItems = !!value.carId && !ready;
  const carItems = ready && loaded ? loaded.items : [];

  const selectableItems = carItems.filter((it) => !takenIds.has(String(it.id)));

  return (
    <div className="flex items-center gap-3">
      <Select
        value={value.carId}
        onChange={(e) => onChange({ ...value, carId: e.target.value, carItemId: '' })}
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
        {selectableItems.map((it) => (
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