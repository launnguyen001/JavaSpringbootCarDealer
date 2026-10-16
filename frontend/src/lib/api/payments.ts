import { apiRequest } from './http';
import type { Payment, PaymentRequest } from '@/types';

export async function listPayments(): Promise<Payment[]> {
  return apiRequest<Payment[]>('/payments', { auth: true });
}

export async function getPayment(id: number): Promise<Payment> {
  return apiRequest<Payment>(`/payments/${id}`, { auth: true });
}

export async function getPaymentsByOrder(orderId: number): Promise<Payment[]> {
  return apiRequest<Payment[]>(`/payments/order/${orderId}`, { auth: true });
}

export async function createPayment(payload: PaymentRequest): Promise<Payment> {
  return apiRequest<Payment>('/payments', { method: 'POST', body: payload, auth: true });
}

export async function updatePayment(id: number, payload: PaymentRequest): Promise<Payment> {
  return apiRequest<Payment>(`/payments/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deletePayment(id: number): Promise<void> {
  return apiRequest<void>(`/payments/${id}`, { method: 'DELETE', auth: true });
}
