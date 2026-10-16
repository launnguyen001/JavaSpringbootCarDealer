export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface OrderItem {
  id: number;
  carId: number;
  carName: string;
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

export interface OrderItemRequest {
  carId: number;
  quantity: number;
}

export interface OrderRequest {
  customerId: number;
  employeeId?: number | null;
  items: OrderItemRequest[];
}
