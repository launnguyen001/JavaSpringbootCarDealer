'use client';

import { useEffect, useState, type ReactNode } from 'react';
import Modal from '@/components/ui/Modal';
import Button from '@/components/ui/Button';
import { Field, Input, Select } from '@/components/ui/Field';
import StatusPill from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import {
  getOrder,
  getInvoiceByOrder,
  getContractByOrder,
  getPaymentsByOrder,
  getOrderStatusHistory,
  downloadFile,
  createInvoice,
  createPayment,
} from '@/lib/api';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import type { Contract, Invoice, Order, OrderStatusHistory, Payment, PaymentMethod } from '@/types';

const METHOD_LABEL: Record<string, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  CARD: 'Thẻ',
  INSTALLMENT: 'Trả góp',
};

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Cũ' };

type SectionKey = 'items' | 'invoice' | 'contract' | 'payments' | 'history';

const DEFAULT_OPEN: Record<SectionKey, boolean> = {
  items: true,
  invoice: true,
  contract: false,
  payments: false,
  history: true,
};

type FinancialAction = 'invoice' | 'payment';

interface Props {
  /** null = đóng popup. Khi có giá trị sẽ tải đơn + hợp đồng + thanh toán + hóa đơn của đơn đó. */
  orderId: number | null;
  initialAction?: FinancialAction | null;
  onClose: () => void;
  onActionSuccess?: () => void;
}

/** Section có nút expand (mũi tên) — gom mọi thông tin liên quan của 1 đơn hàng vào popup. */
function Section({
  title,
  count,
  open,
  onToggle,
  children,
}: {
  title: string;
  count?: number;
  open: boolean;
  onToggle: () => void;
  children: ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-[#e5e7eb] overflow-hidden">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={open}
        className="w-full flex items-center justify-between gap-3 px-4 py-3 bg-[#fafafa] text-left transition-colors hover:bg-[#f3f4f6]"
      >
        <span className="text-[11px] font-semibold text-[#6b7280] uppercase tracking-wider">
          {title}
          {count != null && ` (${count})`}
        </span>
        <svg
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="#9ca3af"
          strokeWidth="2"
          className={`transition-transform duration-200 ${open ? 'rotate-180' : ''}`}
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>
      {open && <div className="px-4 py-4">{children}</div>}
    </div>
  );
}

function Info({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-[10px] font-semibold text-[#9ca3af] uppercase tracking-wider">{label}</dt>
      <dd className="mt-0.5 text-sm text-[#0a0a0a] font-medium">{children}</dd>
    </div>
  );
}

/**
 * Nội dung chi tiết 1 đơn hàng — mount lại từ đầu mỗi khi đổi orderId (nhờ key),
 * nên trạng thái loading/expand được reset mà không cần setState trong effect.
 */
function OrderDetailContent({
  orderId,
  initialAction,
  onOrderChange,
  onActionSuccess,
}: {
  orderId: number;
  initialAction?: FinancialAction | null;
  onOrderChange: (order: Order) => void;
  onActionSuccess?: () => void;
}) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<Order | null>(null);
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [contract, setContract] = useState<Contract | null>(null);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [history, setHistory] = useState<OrderStatusHistory[]>([]);
  const [open, setOpen] = useState<Record<SectionKey, boolean>>(DEFAULT_OPEN);
  const [action, setAction] = useState<FinancialAction | null>(initialAction ?? null);
  const [invoiceNumber, setInvoiceNumber] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [paymentAmount, setPaymentAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('BANK_TRANSFER');
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  useEffect(() => {
    let active = true;
    (async () => {
      const [o, inv, con, pay, hist] = await Promise.allSettled([
        getOrder(orderId),
        getInvoiceByOrder(orderId),
        getContractByOrder(orderId),
        getPaymentsByOrder(orderId),
        getOrderStatusHistory(orderId),
      ]);
      if (!active) return;
      if (o.status === 'fulfilled') {
        setOrder(o.value);
        onOrderChange(o.value);
      } else {
        setError('Không tải được thông tin đơn hàng. Vui lòng thử lại.');
      }
      // Các mục liên quan có thể chưa tồn tại (404) — bỏ qua im lặng.
      if (inv.status === 'fulfilled') setInvoice(inv.value);
      if (con.status === 'fulfilled') setContract(con.value);
      if (pay.status === 'fulfilled') setPayments(pay.value);
      if (hist.status === 'fulfilled') setHistory(hist.value);
      setLoading(false);
    })();
    return () => {
      active = false;
    };
  }, [orderId, onOrderChange]);

  const downloadPdf = async (path: string, filename: string) => {
    try {
      await downloadFile(path, filename);
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const toggle = (key: SectionKey) => setOpen((prev) => ({ ...prev, [key]: !prev[key] }));

  const submitInvoice = async () => {
    if (!order) return;
    const number = invoiceNumber.trim() || `HD-${order.orderCode}`;
    setSubmitting(true);
    try {
      const created = await createInvoice({
        orderId: order.id,
        invoiceNumber: number,
        total: order.totalAmount,
        dueDate: dueDate || null,
      });
      setInvoice(created);
      setAction(null);
      toast.success('Đã tạo hóa đơn cho đơn hàng.');
      onActionSuccess?.();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const submitPayment = async () => {
    if (!order || !invoice) return;
    const amount = Number(paymentAmount || invoice.remainingAmount);
    if (!Number.isFinite(amount) || amount <= 0) {
      toast.error('Số tiền thu không hợp lệ.');
      return;
    }
    if (amount - invoice.remainingAmount > 0.0001) {
      toast.error('Số tiền vượt quá công nợ còn lại.');
      return;
    }
    setSubmitting(true);
    try {
      const created = await createPayment({
        orderId: order.id,
        amount,
        method: paymentMethod,
        status: 'PAID',
      });
      setPayments((prev) => [created, ...prev]);
      const refreshedInvoice = await getInvoiceByOrder(order.id);
      setInvoice(refreshedInvoice);
      setPaymentAmount('');
      setAction(null);
      toast.success('Đã ghi nhận khoản tiền thực thu.');
      onActionSuccess?.();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const paidTotal = payments.reduce((sum, p) => sum + (p.status === 'PAID' || p.status === 'PARTIAL' ? (p.amount ?? 0) : 0), 0);
  const remaining = invoice ? invoice.remainingAmount : order ? order.totalAmount - paidTotal : 0;

  if (loading) {
    return <div className="py-12 text-center text-[#9ca3af] text-sm">Đang tải chi tiết đơn hàng…</div>;
  }
  if (error || !order) {
    return (
      <div className="py-12 text-center text-[#ef4444] text-sm">
        {error ?? 'Không tìm thấy đơn hàng.'}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Tổng quan đơn hàng */}
      <dl className="grid grid-cols-2 sm:grid-cols-3 gap-x-4 gap-y-4 rounded-2xl bg-[#fafafa] border border-[#e5e7eb] p-4">
        <Info label="Mã đơn">{order.orderCode}</Info>
        <Info label="Khách hàng">{order.customerName ?? '—'}</Info>
        <Info label="Nhân viên">{order.employeeName ?? '—'}</Info>
        <Info label="Ngày đặt">{formatDateTime(order.orderDate)}</Info>
        <Info label="Trạng thái">
          <StatusPill status={order.status} />
        </Info>
        <Info label="Tổng tiền">
          <span className="text-[#c58e00] font-bold">{formatVnd(order.totalAmount)}</span>
        </Info>
      </dl>

      {/* Mặt hàng */}
      <Section title="Mặt hàng" count={order.items?.length ?? 0} open={open.items} onToggle={() => toggle('items')}>
        {(order.items?.length ?? 0) === 0 ? (
          <p className="text-sm text-[#9ca3af]">Đơn hàng chưa có mặt hàng nào.</p>
        ) : (
          <div className="overflow-x-auto rounded-xl border border-[#e5e7eb]">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-[#fafafa] border-b border-[#e5e7eb]">
                  <th className="px-3 py-2 text-left text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">Xe</th>
                  <th className="px-3 py-2 text-left text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">VIN</th>
                  <th className="px-3 py-2 text-left text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">Tình trạng</th>
                  <th className="px-3 py-2 text-right text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">Đơn giá</th>
                  <th className="px-3 py-2 text-right text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                {order.items.map((it) => (
                  <tr key={it.id} className="border-b border-[#f3f4f6] last:border-0">
                    <td className="px-3 py-2 text-[#374151]">{it.carName}</td>
                    <td className="px-3 py-2 text-[#374151]">{it.vin ?? '—'}</td>
                    <td className="px-3 py-2 text-[#374151]">
                      {it.condition ? (CONDITION_LABEL[it.condition] ?? it.condition) : '—'}
                    </td>
                    <td className="px-3 py-2 text-right text-[#374151]">{formatVnd(it.unitPrice)}</td>
                    <td className="px-3 py-2 text-right font-medium text-[#0a0a0a]">{formatVnd(it.lineTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Section>

      {/* Hóa đơn */}
      <Section title="Hóa đơn" open={open.invoice} onToggle={() => toggle('invoice')}>
        {invoice ? (
          <>
            <dl className="grid grid-cols-2 sm:grid-cols-3 gap-x-4 gap-y-4">
              <Info label="Số hóa đơn">{invoice.invoiceNumber}</Info>
              <Info label="Mã đơn">{invoice.orderCode ?? order.orderCode}</Info>
              <Info label="Ngày phát hành">
                {invoice.issuedAt ? formatDateTime(invoice.issuedAt) : '—'}
              </Info>
              <Info label="Tổng tiền">
                <span className="text-[#c58e00] font-bold">{formatVnd(invoice.total)}</span>
              </Info>
              <Info label="Trạng thái thu"><StatusPill status={invoice.paymentStatus} /></Info>
              <Info label="Đã thu">{formatVnd(invoice.paidAmount)}</Info>
              <Info label="Còn phải thu">{formatVnd(invoice.remainingAmount)}</Info>
              <Info label="Hạn thanh toán">{invoice.dueDate ?? '—'}</Info>
            </dl>
            <div className="mt-4">
              <Button
                size="sm"
                variant="gold"
                onClick={() =>
                  downloadPdf(`/invoices/${invoice.id}/pdf`, `hoa-don-${invoice.id}.pdf`)
                }
              >
                Tải hóa đơn PDF
              </Button>
            </div>
          </>
        ) : (
          <div className="space-y-3">
            <p className="text-sm text-[#9ca3af]">Đơn hàng này chưa có hóa đơn.</p>
            {order.status === 'CONFIRMED' && (
              <Button size="sm" variant="gold" onClick={() => setAction('invoice')}>
                + Tạo hóa đơn tại đây
              </Button>
            )}
          </div>
        )}
        {action === 'invoice' && !invoice && (
          <div className="mt-4 rounded-xl border border-[#e5e7eb] bg-[#fafafa] p-4 space-y-3">
            <div className="font-semibold text-sm text-[#111827]">Lập hóa đơn cho {order.orderCode}</div>
            <Field label="Số hóa đơn" required>
              <Input value={invoiceNumber} onChange={(event) => setInvoiceNumber(event.target.value)} maxLength={40} placeholder={`HD-${order.orderCode}`} />
            </Field>
            <Field label="Tổng tiền hóa đơn">
              <Input value={formatVnd(order.totalAmount)} readOnly />
            </Field>
            <Field label="Hạn thanh toán">
              <Input type="date" value={dueDate} onChange={(event) => setDueDate(event.target.value)} />
            </Field>
            <div className="flex justify-end gap-2">
              <Button size="sm" variant="outline" disabled={submitting} onClick={() => setAction(null)}>Đóng</Button>
              <Button size="sm" variant="gold" loading={submitting} onClick={submitInvoice}>Tạo hóa đơn</Button>
            </div>
          </div>
        )}
      </Section>

      {/* Hợp đồng */}
      <Section title="Hợp đồng" open={open.contract} onToggle={() => toggle('contract')}>
        {contract ? (
          <div className="space-y-3">
            <dl className="grid grid-cols-2 sm:grid-cols-3 gap-x-4 gap-y-4">
              <Info label="Số hợp đồng">{contract.contractNumber}</Info>
              <Info label="Mã đơn">{contract.orderCode ?? order.orderCode}</Info>
              <Info label="Ngày ký">
                {contract.signedAt ? formatDateTime(contract.signedAt) : '—'}
              </Info>
            </dl>
            {contract.content && (
              <div>
                <p className="text-[10px] font-semibold text-[#9ca3af] uppercase tracking-wider mb-1">
                  Nội dung
                </p>
                <p className="text-sm text-[#4b5563] leading-relaxed whitespace-pre-wrap rounded-xl bg-[#fafafa] border border-[#e5e7eb] p-3">
                  {contract.content}
                </p>
              </div>
            )}
            <div>
              <Button
                size="sm"
                variant="gold"
                onClick={() =>
                  downloadPdf(`/contracts/${contract.id}/pdf`, `hop-dong-${contract.id}.pdf`)
                }
              >
                Tải hợp đồng PDF
              </Button>
            </div>
          </div>
        ) : (
          <p className="text-sm text-[#9ca3af]">Đơn hàng này chưa có hợp đồng.</p>
        )}
      </Section>

      {/* Thanh toán */}
      <Section title="Thanh toán" count={payments.length} open={open.payments} onToggle={() => toggle('payments')}>
        {invoice && contract && invoice.remainingAmount > 0.0001
          && invoice.paymentStatus !== 'CANCELLED' && order.status === 'CONFIRMED' && (
          <div className="mb-4">
            <Button size="sm" variant="gold" onClick={() => {
              setPaymentAmount(String(invoice.remainingAmount));
              setAction('payment');
            }}>
              + Ghi nhận thanh toán / Thu tiền
            </Button>
          </div>
        )}
        {payments.length === 0 ? (
          <p className="text-sm text-[#9ca3af]">Chưa có giao dịch thanh toán nào.</p>
        ) : (
          <>
            <div className="overflow-x-auto rounded-xl border border-[#e5e7eb]">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-[#fafafa] border-b border-[#e5e7eb]">
                    <th className="px-3 py-2 text-left text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">Số tiền</th>
                    <th className="px-3 py-2 text-left text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">Phương thức</th>
                    <th className="px-3 py-2 text-left text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">Thời điểm</th>
                  </tr>
                </thead>
                <tbody>
                  {payments.map((p) => (
                    <tr key={p.id} className="border-b border-[#f3f4f6] last:border-0">
                      <td className="px-3 py-2 font-medium text-[#0a0a0a]">{formatVnd(p.amount)}</td>
                      <td className="px-3 py-2 text-[#374151]">{METHOD_LABEL[p.method] ?? p.method}</td>
                      <td className="px-3 py-2 text-[#374151]">
                        {p.paidAt ? formatDateTime(p.paidAt) : '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mt-3 flex flex-wrap items-center justify-end gap-x-6 gap-y-1 text-sm">
              <span className="text-[#6b7280]">
                Đã thanh toán: <b className="text-[#0a0a0a]">{formatVnd(paidTotal)}</b>
              </span>
              <span className="text-[#6b7280]">
                Còn lại:{' '}
                <b className={remaining > 0 ? 'text-[#ef4444]' : 'text-[#10b981]'}>
                  {formatVnd(Math.max(remaining, 0))}
                </b>
              </span>
            </div>
          </>
        )}
        {action === 'payment' && invoice && (
          <div className="mt-4 rounded-xl border border-[#e5e7eb] bg-[#fafafa] p-4 space-y-3">
            <div className="font-semibold text-sm text-[#111827]">Ghi nhận tiền thu — {order.orderCode}</div>
            <dl className="grid grid-cols-2 gap-3 text-sm">
              <div><dt className="text-xs text-[#6b7280]">Còn phải thu</dt><dd className="font-semibold text-red-600">{formatVnd(invoice.remainingAmount)}</dd></div>
              <div><dt className="text-xs text-[#6b7280]">Đã thu</dt><dd className="font-semibold">{formatVnd(invoice.paidAmount)}</dd></div>
            </dl>
            <Field label="Số tiền thực nhận (VND)" required>
              <Input type="number" min="1" max={invoice.remainingAmount} step="1" value={paymentAmount || String(invoice.remainingAmount)} onChange={(event) => setPaymentAmount(event.target.value)} />
            </Field>
            <Field label="Phương thức thanh toán" required>
              <Select value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value as PaymentMethod)}>
                <option value="BANK_TRANSFER">Chuyển khoản</option>
                <option value="CASH">Tiền mặt</option>
                <option value="CARD">Thẻ</option>
                <option value="INSTALLMENT">Trả góp</option>
              </Select>
            </Field>
            <div className="flex justify-end gap-2">
              <Button size="sm" variant="outline" disabled={submitting} onClick={() => setAction(null)}>Đóng</Button>
              <Button size="sm" variant="gold" loading={submitting} onClick={submitPayment}>Xác nhận đã nhận tiền</Button>
            </div>
            <p className="text-xs text-[#9ca3af]">Chỉ xác nhận sau khi nhân viên đã thực tế nhận hoặc đối soát được khoản tiền này.</p>
          </div>
        )}
      </Section>

      {/* Lịch sử trạng thái (state machine) */}
      <Section
        title="Lịch sử trạng thái"
        count={history.length}
        open={open.history}
        onToggle={() => toggle('history')}
      >
        {history.length === 0 ? (
          <p className="text-sm text-[#9ca3af]">Chưa có lịch sử chuyển trạng thái.</p>
        ) : (
          <ol className="space-y-4">
            {history.map((entry, index) => (
              <li key={entry.id} className="relative pl-6">
                <span className="absolute left-0 top-1.5 h-2.5 w-2.5 rounded-full bg-[#c58e00]" />
                {index < history.length - 1 && (
                  <span className="absolute left-[4px] top-4 h-[calc(100%+0.5rem)] w-px bg-[#e5e7eb]" />
                )}
                <div className="flex flex-wrap items-center gap-2">
                  <StatusPill status={entry.toStatus} />
                  <span className="text-xs text-[#6b7280]">{formatDateTime(entry.changedAt)}</span>
                </div>
                <p className="mt-1 text-sm text-[#374151]">
                  {entry.fromStatus
                    ? `${entry.fromStatus} → ${entry.toStatus}`
                    : `Khởi tạo đơn ở trạng thái ${entry.toStatus}`}
                </p>
                <p className="text-xs text-[#9ca3af]">
                  {entry.changedBy ?? 'system'}
                  {entry.note ? ` • ${entry.note}` : ''}
                </p>
              </li>
            ))}
          </ol>
        )}
      </Section>
    </div>
  );
}

export default function OrderDetailModal({ orderId, initialAction, onClose, onActionSuccess }: Props) {
  // Dùng cho tiêu đề popup; chỉ nhận lại khi đúng đơn đang mở.
  const [order, setOrder] = useState<Order | null>(null);
  const currentOrder = order && order.id === orderId ? order : null;

  return (
    <Modal
      open={orderId != null}
      title={currentOrder ? `Chi tiết đơn ${currentOrder.orderCode}` : 'Chi tiết đơn hàng'}
      onClose={onClose}
      size="xl"
      footer={
        <Button variant="outline" onClick={onClose}>
          Đóng
        </Button>
      }
    >
      {orderId != null && (
        <OrderDetailContent
          key={`${orderId}-${initialAction ?? 'view'}`}
          orderId={orderId}
          initialAction={initialAction}
          onOrderChange={setOrder}
          onActionSuccess={onActionSuccess}
        />
      )}
    </Modal>
  );
}
