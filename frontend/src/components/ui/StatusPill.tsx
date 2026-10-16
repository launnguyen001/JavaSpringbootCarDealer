'use client';

const statusMap: Record<string, { label: string; className: string }> = {
  // CarStatus
  AVAILABLE: { label: 'Sẵn sàng', className: 'bg-[#ecfdf5] text-[#10b981] border-[#10b981]/30' },
  RESERVED: { label: 'Đã đặt', className: 'bg-[#fffbeb] text-[#f59e0b] border-[#f59e0b]/30' },
  SOLD: { label: 'Đã bán', className: 'bg-[#f3f4f6] text-[#6b7280] border-[#6b7280]/30' },
  MAINTENANCE: { label: 'Bảo trì', className: 'bg-[#fef2f2] text-[#ef4444] border-[#ef4444]/30' },
  // OrderStatus
  PENDING: { label: 'Chờ xử lý', className: 'bg-[#fffbeb] text-[#f59e0b] border-[#f59e0b]/30' },
  CONFIRMED: { label: 'Đã xác nhận', className: 'bg-[#eff6ff] text-[#2563eb] border-[#2563eb]/30' },
  COMPLETED: { label: 'Hoàn tất', className: 'bg-[#ecfdf5] text-[#10b981] border-[#10b981]/30' },
  CANCELLED: { label: 'Đã hủy', className: 'bg-[#fef2f2] text-[#ef4444] border-[#ef4444]/30' },
  // AppointmentStatus
  SCHEDULED: { label: 'Đã lên lịch', className: 'bg-[#fffbeb] text-[#f59e0b] border-[#f59e0b]/30' },
  // PaymentStatus
  UNPAID: { label: 'Chưa thanh toán', className: 'bg-[#fef2f2] text-[#ef4444] border-[#ef4444]/30' },
  PARTIAL: { label: 'Thanh toán một phần', className: 'bg-[#fffbeb] text-[#f59e0b] border-[#f59e0b]/30' },
  PAID: { label: 'Đã thanh toán', className: 'bg-[#ecfdf5] text-[#10b981] border-[#10b981]/30' },
  REFUNDED: { label: 'Đã hoàn tiền', className: 'bg-[#f3f4f6] text-[#6b7280] border-[#6b7280]/30' },
};

interface StatusPillProps {
  status: string;
  className?: string;
}

export default function StatusPill({ status, className = '' }: StatusPillProps) {
  const cfg = statusMap[status] ?? {
    label: status,
    className: 'bg-[#f3f4f6] text-[#6b7280] border-[#6b7280]/30',
  };
  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-semibold uppercase tracking-wide border ${cfg.className} ${className}`}
    >
      {cfg.label}
    </span>
  );
}
