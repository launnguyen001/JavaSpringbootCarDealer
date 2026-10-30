import { apiRequest } from './http';
import type {
  Order,
  OrderItem,
  OrderItemRequest,
  OrderOption,
  OrderOptionTarget,
  OrderRequest,
  OrderStatus,
  OrderStatusHistory,
} from '@/types';

export async function listOrders(): Promise<Order[]> {
  return apiRequest<Order[]>('/orders', { auth: true });
}

/**
 * Danh sách đơn hàng đủ điều kiện gắn chứng từ, dùng cho dropdown ở form admin.
 * target = 'invoice' | 'contract' | 'payment'.
 */
export async function getOrderOptions(target: OrderOptionTarget): Promise<OrderOption[]> {
  return apiRequest<OrderOption[]>(`/orders/options?target=${encodeURIComponent(target)}`, {
    auth: true,
  });
}

export async function getOrder(id: number): Promise<Order> {
  return apiRequest<Order>(`/orders/${id}`, { auth: true });
}

export async function getOrdersByCustomer(customerId: number): Promise<Order[]> {
  return apiRequest<Order[]>(`/orders/customer/${customerId}`, { auth: true });
}

export async function createOrder(payload: OrderRequest): Promise<Order> {
  return apiRequest<Order>('/orders', { method: 'POST', body: payload, auth: true });
}

export async function updateOrder(id: number, payload: OrderRequest): Promise<Order> {
  return apiRequest<Order>(`/orders/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteOrder(id: number): Promise<void> {
  return apiRequest<void>(`/orders/${id}`, { method: 'DELETE', auth: true });
}

export async function updateOrderStatus(id: number, status: OrderStatus): Promise<Order> {
  return apiRequest<Order>(`/orders/${id}/status?status=${encodeURIComponent(status)}`, {
    method: 'PATCH',
    auth: true,
  });
}

export async function cancelOrder(id: number): Promise<Order> {
  return apiRequest<Order>(`/orders/${id}/cancel`, { method: 'POST', auth: true });
}

/** Lịch sử chuyển trạng thái của đơn (chủ đơn hoặc staff). */
export async function getOrderStatusHistory(orderId: number): Promise<OrderStatusHistory[]> {
  return apiRequest<OrderStatusHistory[]>(`/orders/${orderId}/history`, { auth: true });
}

/** Các trạng thái được phép chuyển tới từ trạng thái hiện tại của đơn. */
export async function getAllowedOrderTransitions(orderId: number): Promise<OrderStatus[]> {
  return apiRequest<OrderStatus[]>(`/orders/${orderId}/allowed-transitions`, { auth: true });
}

/** Toàn bộ cấu hình state machine (chỉ admin) — dùng để render dropdown trạng thái. */
export async function getOrderStateMachine(): Promise<Record<OrderStatus, OrderStatus[]>> {
  return apiRequest<Record<OrderStatus, OrderStatus[]>>('/orders/state-machine', { auth: true });
}

// ---- Order items ----
export async function listOrderItems(orderId: number): Promise<OrderItem[]> {
  return apiRequest<OrderItem[]>(`/orders/${orderId}/items`, { auth: true });
}

export async function addOrderItem(orderId: number, payload: OrderItemRequest): Promise<OrderItem> {
  return apiRequest<OrderItem>(`/orders/${orderId}/items`, {
    method: 'POST',
    body: payload,
    auth: true,
  });
}

export async function updateOrderItem(
  orderId: number,
  itemId: number,
  payload: OrderItemRequest,
): Promise<OrderItem> {
  return apiRequest<OrderItem>(`/orders/${orderId}/items/${itemId}`, {
    method: 'PUT',
    body: payload,
    auth: true,
  });
}

export async function deleteOrderItem(orderId: number, itemId: number): Promise<void> {
  return apiRequest<void>(`/orders/${orderId}/items/${itemId}`, {
    method: 'DELETE',
    auth: true,
  });
}
