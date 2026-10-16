import { apiRequest } from './http';
import type { Invoice, InvoiceRequest } from '@/types';

export async function listInvoices(): Promise<Invoice[]> {
  return apiRequest<Invoice[]>('/invoices', { auth: true });
}

export async function getInvoice(id: number): Promise<Invoice> {
  return apiRequest<Invoice>(`/invoices/${id}`, { auth: true });
}

export async function getInvoiceByOrder(orderId: number): Promise<Invoice> {
  return apiRequest<Invoice>(`/invoices/order/${orderId}`, { auth: true });
}

export async function createInvoice(payload: InvoiceRequest): Promise<Invoice> {
  return apiRequest<Invoice>('/invoices', { method: 'POST', body: payload, auth: true });
}

export async function updateInvoice(id: number, payload: InvoiceRequest): Promise<Invoice> {
  return apiRequest<Invoice>(`/invoices/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteInvoice(id: number): Promise<void> {
  return apiRequest<void>(`/invoices/${id}`, { method: 'DELETE', auth: true });
}
