'use client';

import { useEffect, useState, type ReactNode } from 'react';
import Modal from '@/components/ui/Modal';
import Button from '@/components/ui/Button';
import StatusPill from '@/components/ui/StatusPill';
import { getOrder, getInvoiceByOrder, getContractByOrder, getPaymentsByOrder } from '@/lib/api';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import type { Contract, Invoice, Order, Payment } from '@/types';

const METHOD_LABEL: Record<string, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  CARD: 'Thẻ',
  INSTALLMENT: 'Trả góp',
};

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Cũ' };

type SectionKey = 'items' | 'invoice' | 'contract' | 'payments';

const DEFAULT_OPEN: Record<SectionKey, boolean> = {
  items: true,
  invoice: true,
  contract: false,
  payments: false,
};

interface Props {
  /** null = đóng popup. Khi có giá trị sẽ tải đơn + hợp đồng + thanh toán + hóa đơn của đơn đó. */
  orderId: number | null;
  onClose: () => void;
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
  onOrderChange,
}: {
  orderId: number;
  onOrderChange: (order: Order) => void;
}) {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [order, setOrder] = useState<Order | null>(null);
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [contract, setContract] = useState<Contract | null>(null);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [open, setOpen] = useState<Record<SectionKey, boolean>>(DEFAULT_OPEN);

  useEffect(() => {
    let active = true;
    (async () => {
      const [o, inv, con, pay] = await Promise.allSettled([
        getOrder(orderId),
        getInvoiceByOrder(orderId),
        getContractByOrder(orderId),
        getPaymentsByOrder(orderId),
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
      setLoading(false);
    })();
    return () => {
      active = false;
    };
  }, [orderId, onOrderChange]);

  const toggle = (key: SectionKey) => setOpen((prev) => ({ ...prev, [key]: !prev[key] }));

  const paidTotal = payments.reduce((sum, p) => sum + (p.amount ?? 0), 0);
  const remaining = order ? order.totalAmount - paidTotal : 0;

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
          <dl className="grid grid-cols-2 sm:grid-cols-3 gap-x-4 gap-y-4">
            <Info label="Số hóa đơn">{invoice.invoiceNumber}</Info>
            <Info label="Mã đơn">{invoice.orderCode ?? order.orderCode}</Info>
            <Info label="Ngày phát hành">
              {invoice.issuedAt ? formatDateTime(invoice.issuedAt) : '—'}
            </Info>
            <Info label="Tổng tiền">
              <span className="text-[#c58e00] font-bold">{formatVnd(invoice.total)}</span>
            </Info>
          </dl>
        ) : (
          <p className="text-sm text-[#9ca3af]">Đơn hàng này chưa có hóa đơn.</p>
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
          </div>
        ) : (
          <p className="text-sm text-[#9ca3af]">Đơn hàng này chưa có hợp đồng.</p>
        )}
      </Section>

      {/* Thanh toán */}
      <Section title="Thanh toán" count={payments.length} open={open.payments} onToggle={() => toggle('payments')}>
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
                    <th className="px-3 py-2 text-left text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">Trạng thái</th>
                    <th className="px-3 py-2 text-left text-[10px] font-semibold text-[#6b7280] uppercase tracking-wider">Thời điểm</th>
                  </tr>
                </thead>
                <tbody>
                  {payments.map((p) => (
                    <tr key={p.id} className="border-b border-[#f3f4f6] last:border-0">
                      <td className="px-3 py-2 font-medium text-[#0a0a0a]">{formatVnd(p.amount)}</td>
                      <td className="px-3 py-2 text-[#374151]">{METHOD_LABEL[p.method] ?? p.method}</td>
                      <td className="px-3 py-2">
                        <StatusPill status={p.status} />
                      </td>
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
      </Section>
    </div>
  );
}

export default function OrderDetailModal({ orderId, onClose }: Props) {
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
        <OrderDetailContent key={orderId} orderId={orderId} onOrderChange={setOrder} />
      )}
    </Modal>
  );
}
