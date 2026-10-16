export interface Invoice {
  id: number;
  orderId: number;
  orderCode: string;
  invoiceNumber: string;
  issuedAt: string | null;
  total: number;
}

export interface InvoiceRequest {
  orderId: number;
  invoiceNumber: string;
  total: number;
  issuedAt?: string | null;
}
