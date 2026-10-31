export type InvoicePaymentStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE' | 'CANCELLED';

export interface Invoice {
  id: number;
  orderId: number;
  orderCode: string;
  invoiceNumber: string;
  issuedAt: string | null;
  total: number;
  paidAmount: number;
  remainingAmount: number;
  paymentStatus: InvoicePaymentStatus;
  dueDate: string | null;
  customerName: string | null;
}

export interface InvoiceRequest {
  orderId: number;
  invoiceNumber: string;
  total: number;
  issuedAt?: string | null;
  dueDate?: string | null;
}
