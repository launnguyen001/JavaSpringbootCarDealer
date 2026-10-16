export type PaymentMethod = 'CASH' | 'BANK_TRANSFER' | 'CARD' | 'INSTALLMENT';
export type PaymentStatus = 'UNPAID' | 'PARTIAL' | 'PAID' | 'REFUNDED';

export interface Payment {
  id: number;
  orderId: number;
  orderCode: string;
  amount: number;
  method: PaymentMethod;
  status: PaymentStatus;
  paidAt: string | null;
}

export interface PaymentRequest {
  orderId: number;
  amount: number;
  method: PaymentMethod;
  status: PaymentStatus;
}
