import { fetchJson, apiRequest } from './http';
import type { Car, CarItem } from '@/types';

/** Form tạo/sửa DÒNG XE (cars): chỉ còn thông tin sản phẩm. */
export type CarRequest = {
  name: string;
  description?: string | null;
  basePrice: number;
  modelId: number;
};

export async function listCars(): Promise<Car[]> {
  return fetchJson<Car[]>('/cars');
}

export async function getCar(id: number | string): Promise<Car> {
  return fetchJson<Car>(`/cars/${id}`);
}

/** Danh sách từng chiếc xe vật lý của một dòng xe. */
export async function getCarItems(id: number | string): Promise<CarItem[]> {
  return fetchJson<CarItem[]>(`/cars/${id}/items`);
}

export async function searchCarsByName(name: string): Promise<Car[]> {
  return fetchJson<Car[]>(`/cars/search/name?name=${encodeURIComponent(name)}`);
}

export async function searchCarsByPrice(minPrice: number, maxPrice: number): Promise<Car[]> {
  return fetchJson<Car[]>(
    `/cars/search/price?minPrice=${minPrice}&maxPrice=${maxPrice}`,
  );
}

export async function searchCarsByStock(minQuantity: number): Promise<Car[]> {
  return fetchJson<Car[]>(`/cars/search/stock?minQuantity=${minQuantity}`);
}

export async function searchCarsAdvanced({
  modelName,
  minPrice = 0,
  maxPrice = 1_000_000_000,
  minQuantity = 0,
}: {
  modelName: string;
  minPrice?: number;
  maxPrice?: number;
  minQuantity?: number;
}): Promise<Car[]> {
  return fetchJson<Car[]>(
    `/cars/search/advanced?modelName=${encodeURIComponent(modelName)}&minPrice=${minPrice}&maxPrice=${maxPrice}&minQuantity=${minQuantity}`,
  );
}

export async function searchCarsAdvancedByBrand({
  brandName,
  minPrice = 0,
  maxPrice = 1_000_000_000,
  minQuantity = 0,
}: {
  brandName: string;
  minPrice?: number;
  maxPrice?: number;
  minQuantity?: number;
}): Promise<Car[]> {
  return fetchJson<Car[]>(
    `/cars/search/advanced/brand?brandName=${encodeURIComponent(brandName)}&minPrice=${minPrice}&maxPrice=${maxPrice}&minQuantity=${minQuantity}`,
  );
}

export async function getCarsByBrand(brandId: number | string): Promise<Car[]> {
  return fetchJson<Car[]>(`/cars/brand/${brandId}`);
}

export async function getCarsByModel(modelId: number | string): Promise<Car[]> {
  return fetchJson<Car[]>(`/cars/model/${modelId}`);
}

// ---- CRUD (ADMIN) ----
export async function createCar(payload: CarRequest): Promise<Car> {
  return apiRequest<Car>('/cars', { method: 'POST', body: payload, auth: true });
}

export async function updateCar(id: number, payload: CarRequest): Promise<Car> {
  return apiRequest<Car>(`/cars/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteCar(id: number): Promise<void> {
  return apiRequest<void>(`/cars/${id}`, { method: 'DELETE', auth: true });
}
