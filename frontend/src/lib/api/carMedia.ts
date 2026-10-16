import { fetchOptional, apiRequest } from './http';
import type { CarMedia } from '@/types';

export type CarMediaRequest = {
  carId: number;
  thumbnail?: string | null;
  images?: string[];
  videos?: string[];
};

export async function getCarMedia(carId: number | string): Promise<CarMedia | null> {
  return fetchOptional<CarMedia>(`/car-media/${carId}`);
}

export async function listCarMedia(): Promise<CarMedia[]> {
  return apiRequest<CarMedia[]>('/car-media', { auth: true });
}

export async function upsertCarMedia(payload: CarMediaRequest): Promise<CarMedia> {
  return apiRequest<CarMedia>('/car-media', { method: 'PUT', body: payload, auth: true });
}

export async function deleteCarMedia(carId: number): Promise<void> {
  return apiRequest<void>(`/car-media/${carId}`, { method: 'DELETE', auth: true });
}
