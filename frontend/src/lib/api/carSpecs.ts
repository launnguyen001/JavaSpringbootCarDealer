import { fetchOptional, apiRequest } from './http';
import type { CarSpecification } from '@/types';

export type CarSpecificationRequest = {
  carId: number;
  engine?: string | null;
  transmission?: string | null;
  fuelType?: string | null;
  seats?: number | null;
  dimensions?: string | null;
  weightKg?: number | null;
  safetyFeatures?: string[];
  extra?: Record<string, unknown>;
};

export async function getCarSpecs(carId: number | string): Promise<CarSpecification | null> {
  return fetchOptional<CarSpecification>(`/car-specs/${carId}`);
}

export async function listCarSpecs(): Promise<CarSpecification[]> {
  return apiRequest<CarSpecification[]>('/car-specs', { auth: true });
}

export async function upsertCarSpecs(payload: CarSpecificationRequest): Promise<CarSpecification> {
  return apiRequest<CarSpecification>('/car-specs', { method: 'PUT', body: payload, auth: true });
}

export async function deleteCarSpecs(carId: number): Promise<void> {
  return apiRequest<void>(`/car-specs/${carId}`, { method: 'DELETE', auth: true });
}
