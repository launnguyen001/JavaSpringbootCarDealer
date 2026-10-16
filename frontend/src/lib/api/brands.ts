import { fetchJson, apiRequest } from './http';
import type { Brand } from '@/types';

export type BrandRequest = {
  name: string;
  country?: string | null;
  description?: string | null;
};

export async function listBrands(): Promise<Brand[]> {
  return fetchJson<Brand[]>('/brands');
}

export async function getBrand(id: number | string): Promise<Brand> {
  return fetchJson<Brand>(`/brands/${id}`);
}

export async function createBrand(payload: BrandRequest): Promise<Brand> {
  return apiRequest<Brand>('/brands', { method: 'POST', body: payload, auth: true });
}

export async function updateBrand(id: number, payload: BrandRequest): Promise<Brand> {
  return apiRequest<Brand>(`/brands/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteBrand(id: number): Promise<void> {
  return apiRequest<void>(`/brands/${id}`, { method: 'DELETE', auth: true });
}
