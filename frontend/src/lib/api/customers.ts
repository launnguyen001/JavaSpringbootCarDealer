import { apiRequest } from './http';
import type { Customer, CustomerRequest } from '@/types';

export async function listCustomers(): Promise<Customer[]> {
  return apiRequest<Customer[]>('/customers', { auth: true });
}

export async function getCustomer(id: number): Promise<Customer> {
  return apiRequest<Customer>(`/customers/${id}`, { auth: true });
}

export async function createCustomer(payload: CustomerRequest): Promise<Customer> {
  return apiRequest<Customer>('/customers', { method: 'POST', body: payload, auth: true });
}

export async function updateCustomer(id: number, payload: CustomerRequest): Promise<Customer> {
  return apiRequest<Customer>(`/customers/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteCustomer(id: number): Promise<void> {
  return apiRequest<void>(`/customers/${id}`, { method: 'DELETE', auth: true });
}

export async function searchCustomersByName(name: string): Promise<Customer[]> {
  return apiRequest<Customer[]>(`/customers/search/name?name=${encodeURIComponent(name)}`, {
    auth: true,
  });
}

export async function searchCustomersByPhone(phone: string): Promise<Customer[]> {
  return apiRequest<Customer[]>(`/customers/search/phone?phone=${encodeURIComponent(phone)}`, {
    auth: true,
  });
}
