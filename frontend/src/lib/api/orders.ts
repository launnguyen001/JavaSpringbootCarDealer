import { apiRequest } from './http';
import type { Order, OrderItem, OrderItemRequest, OrderRequest, OrderStatus } from '@/types';

export async function listOrders(): Promise<Order[]> {
  return apiRequest<Order[]>('/orders', { auth: true });
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
