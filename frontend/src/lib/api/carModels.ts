import { fetchJson, apiRequest } from './http';
import type { CarModel } from '@/types';

export type CarModelRequest = {
  name: string;
  bodyType?: string | null;
  description?: string | null;
  brandId: number;
};

export async function listCarModels(): Promise<CarModel[]> {
  return fetchJson<CarModel[]>('/car-models');
}

export async function getCarModel(id: number | string): Promise<CarModel> {
  return fetchJson<CarModel>(`/car-models/${id}`);
}

export async function searchCarModelsByName(name: string): Promise<CarModel[]> {
  return fetchJson<CarModel[]>(`/car-models/search/name?name=${encodeURIComponent(name)}`);
}

export async function getCarModelsByBrand(brandId: number | string): Promise<CarModel[]> {
  return fetchJson<CarModel[]>(`/car-models/brand/${brandId}`);
}

export async function createCarModel(payload: CarModelRequest): Promise<CarModel> {
  return apiRequest<CarModel>('/car-models', { method: 'POST', body: payload, auth: true });
}

export async function updateCarModel(id: number, payload: CarModelRequest): Promise<CarModel> {
  return apiRequest<CarModel>(`/car-models/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteCarModel(id: number): Promise<void> {
  return apiRequest<void>(`/car-models/${id}`, { method: 'DELETE', auth: true });
}
