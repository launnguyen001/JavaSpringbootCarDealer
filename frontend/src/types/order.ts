import type { CarCondition } from './car';

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface OrderItem {
  id: number;
  carItemId: number;
  carId: number;
  carName: string;
  vin: string | null;
  condition: CarCondition | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface Order {
  id: number;
  orderCode: string;
  orderDate: string;
  status: OrderStatus;
  totalAmount: number;
  customerId: number;
  customerName: string;
  employeeId: number | null;
  employeeName: string | null;
  items: OrderItem[];
}

/** Mỗi dòng đơn là MỘT chiếc xe vật lý; quantity bỏ trống hoặc bằng 1. */
export interface OrderItemRequest {
  carItemId: number;
  quantity?: number;
}

export interface OrderRequest {
  customerId: number;
  employeeId?: number | null;
  items: OrderItemRequest[];
}

/** Loại chứng từ cần chọn đơn hàng: hóa đơn, hợp đồng hoặc thanh toán. */
export type OrderOptionTarget = 'invoice' | 'contract' | 'payment';

/** Một đơn hàng đủ điều kiện gắn chứng từ — dùng cho dropdown ở form admin. */
export interface OrderOption {
  orderId: number;
  orderCode: string;
  orderDate: string;
  status: OrderStatus;
  customerName: string | null;
  totalAmount: number;
  paidAmount: number;
  remainingAmount: number;
  hasContract: boolean;
  hasInvoice: boolean;
}
