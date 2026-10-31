'use client';

import { useEffect, useState } from 'react';
import Modal from '@/components/ui/Modal';
import Button from '@/components/ui/Button';
import StatusPill from '@/components/ui/StatusPill';
import { useToast } from '@/components/ui/Toast';
import { ApiError, downloadFile, getInvoiceByVin } from '@/lib/api';
import { getErrorMessage } from '@/lib/utils/error';
import { formatDateTime, formatVnd } from '@/lib/utils/format';
import type { Invoice } from '@/types';

interface Props {
  open: boolean;
  /** Xe được chọn từ drawer — hóa đơn tra theo VIN. */
  vin: string | null;
  carName?: string | null;
  onClose: () => void;
}

function Row({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 py-2.5">
      <dt className="shrink-0 text-xs text-[#6b7280]">{label}</dt>
      <dd className="min-w-0 text-right text-sm font-medium text-[#111827]">{children}</dd>
    </div>
  );
}

/** Popup xem nhanh hóa đơn của một chiếc xe, dùng ngay trong drawer Dashboard. */
export default function InvoiceQuickViewModal({ open, vin, carName, onClose }: Props) {
  const toast = useToast();
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [downloading, setDownloading] = useState(false);

  useEffect(() => {
    if (!open || !vin) {
      setInvoice(null);
      setError('');
      setLoading(false);
      return;
    }

    let active = true;
    setLoading(true);
    setError('');
    setInvoice(null);
    getInvoiceByVin(vin)
      .then((value) => {
        if (active) setInvoice(value);
      })
      .catch((err) => {
        if (!active) return;
        setError(
          err instanceof ApiError && err.status === 404
            ? 'Xe này chưa có hóa đơn được phát hành.'
            : getErrorMessage(err),
        );
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [open, vin]);

  const downloadPdf = async () => {
    if (!invoice) return;
    setDownloading(true);
    try {
      await downloadFile(`/invoices/${invoice.id}/pdf`, `hoa-don-${invoice.id}.pdf`);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDownloading(false);
    }
  };

  const paidRatio = invoice && invoice.total > 0
    ? Math.min(100, Math.round((invoice.paidAmount / invoice.total) * 100))
    : 0;

  return (
    <Modal
      open={open}
      title="Xem nhanh hóa đơn"
      onClose={onClose}
      size="md"
      zIndex={140}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Đóng
          </Button>
          {invoice && (
            <Button variant="gold" loading={downloading} onClick={downloadPdf}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              Tải PDF
            </Button>
          )}
        </>
      }
    >
      {loading ? (
        <div className="space-y-3 py-4">
          <div className="h-24 animate-pulse rounded-2xl bg-[#faf7ee]" />
          <div className="h-4 w-2/3 animate-pulse rounded bg-[#f3f4f6]" />
          <div className="h-4 w-1/2 animate-pulse rounded bg-[#f3f4f6]" />
        </div>
      ) : error ? (
        <div className="py-8 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[#fef2f2] text-[#ef4444]">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" aria-hidden="true">
              <path d="M6 3h12v18l-3-2-3 2-3-2-3 2z" />
              <path d="M9 9h6M9 13h4" />
            </svg>
          </div>
          <p className="mt-3 text-sm font-medium text-[#374151]">{error}</p>
          {vin && <p className="mt-1 text-xs text-[#9ca3af]">VIN: {vin}</p>}
        </div>
      ) : invoice ? (
        <div className="space-y-4">
          <div className="relative overflow-hidden rounded-2xl border border-[#ead9a8] bg-gradient-to-br from-[#fffdf7] via-white to-[#faf3e1] p-4">
            <span className="absolute inset-y-0 left-0 w-1 bg-gradient-to-b from-[#c58e00] to-[#e7c86a]" />
            <div className="flex items-start justify-between gap-3 pl-2">
              <div className="min-w-0">
                <p className="text-[10px] font-semibold uppercase tracking-[0.18em] text-[#a87900]">Hóa đơn bán hàng</p>
                <p className="mt-1 truncate font-serif text-2xl font-bold text-[#111827]">{invoice.invoiceNumber}</p>
                <p className="mt-1 truncate text-xs text-[#6b7280]">
                  {carName ?? 'Xe'} {vin ? `· VIN ${vin}` : ''}
                </p>
              </div>
              <StatusPill status={invoice.paymentStatus} />
            </div>
          </div>

          <dl className="divide-y divide-[#f0efe9] rounded-2xl border border-[#e5e7eb] px-4">
            <Row label="Mã đơn">{invoice.orderCode}</Row>
            <Row label="Khách hàng">{invoice.customerName ?? '—'}</Row>
            <Row label="Ngày phát hành">{invoice.issuedAt ? formatDateTime(invoice.issuedAt) : '—'}</Row>
            <Row label="Hạn thanh toán">{invoice.dueDate ?? '—'}</Row>
            <Row label="Tổng tiền">
              <span className="font-serif text-lg font-bold text-[#c58e00]">{formatVnd(invoice.total)}</span>
            </Row>
            <Row label="Đã thu">{formatVnd(invoice.paidAmount)}</Row>
            <Row label="Còn phải thu">
              <span className={invoice.remainingAmount > 0 ? 'text-[#ef4444]' : 'text-[#10b981]'}>
                {formatVnd(invoice.remainingAmount)}
              </span>
            </Row>
          </dl>

          <div>
            <div className="mb-1.5 flex items-center justify-between text-[11px] text-[#6b7280]">
              <span>Tiến độ thanh toán</span>
              <span className="font-semibold text-[#374151]">{paidRatio}%</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-[#f3f4f6]">
              <div
                className="h-full rounded-full bg-gradient-to-r from-[#c58e00] to-[#e7c86a] transition-all"
                style={{ width: `${paidRatio}%` }}
              />
            </div>
          </div>
        </div>
      ) : null}
    </Modal>
  );
}
