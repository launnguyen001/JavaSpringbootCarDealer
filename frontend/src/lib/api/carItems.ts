import { fetchJson, apiRequest } from './http';
import type { CarItem, CarItemRequest, CarStatus } from '@/types';

function qs(params: Record<string, string | number>): string {
  return Object.entries(params)
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
    .join('&');
}

/** Toàn bộ chiếc xe của một dòng xe (đọc công khai). */
export async function listCarItems(carId: number): Promise<CarItem[]> {
  return fetchJson<CarItem[]>(`/car-items?${qs({ carId })}`);
}

/** Chỉ những chiếc đang sẵn sàng (AVAILABLE) của một dòng xe. */
export async function listAvailableCarItems(carId: number): Promise<CarItem[]> {
  return fetchJson<CarItem[]>(`/car-items/available?${qs({ carId })}`);
}

export async function getCarItem(id: number | string): Promise<CarItem> {
  return fetchJson<CarItem>(`/car-items/${id}`);
}

// ---- CRUD (ADMIN) ----
export async function createCarItem(carId: number, payload: CarItemRequest): Promise<CarItem> {
  return apiRequest<CarItem>(`/car-items?${qs({ carId })}`, {
    method: 'POST',
    body: payload,
    auth: true,
  });
}

export async function updateCarItem(id: number, payload: CarItemRequest): Promise<CarItem> {
  return apiRequest<CarItem>(`/car-items/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function updateCarItemStatus(id: number, status: CarStatus): Promise<CarItem> {
  return apiRequest<CarItem>(`/car-items/${id}/status?${qs({ status })}`, {
    method: 'PATCH',
    auth: true,
  });
}

export async function deleteCarItem(id: number): Promise<void> {
  return apiRequest<void>(`/car-items/${id}`, { method: 'DELETE', auth: true });
}
