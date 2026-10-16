import { apiRequest } from './http';
import type { CartItem } from '@/types';

function qs(params: Record<string, string | number>): string {
  return Object.entries(params)
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
    .join('&');
}

export async function getCart(sessionId: string): Promise<CartItem[]> {
  return apiRequest<CartItem[]>(`/cart?${qs({ sessionId })}`, { auth: true });
}

export async function addItem(
  sessionId: string,
  carItemId: number,
  quantity = 1,
): Promise<CartItem[]> {
  return apiRequest<CartItem[]>(`/cart/items?${qs({ sessionId, carItemId, quantity })}`, {
    method: 'POST',
    auth: true,
  });
}

export async function removeItem(sessionId: string, carItemId: number): Promise<CartItem[]> {
  return apiRequest<CartItem[]>(`/cart/items?${qs({ sessionId, carItemId })}`, {
    method: 'DELETE',
    auth: true,
  });
}

export async function clearCart(sessionId: string): Promise<void> {
  return apiRequest<void>(`/cart?${qs({ sessionId })}`, { method: 'DELETE', auth: true });
}
