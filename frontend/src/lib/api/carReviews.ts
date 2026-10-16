import { fetchJson, fetchOptional, apiRequest } from './http';
import type { CarReview, CarReviewSummary } from '@/types';

export type CarReviewRequest = {
  carId: number;
  customerId: number;
  customerName: string;
  rating: number;
  title?: string | null;
  content: string;
};

export async function getCarReviews(carId: number | string): Promise<CarReview[]> {
  return fetchJson<CarReview[]>(`/car-reviews/car/${carId}`);
}

export async function getCarReviewSummary(carId: number | string): Promise<CarReviewSummary | null> {
  return fetchOptional<CarReviewSummary>(`/car-reviews/car/${carId}/summary`);
}

export async function listCarReviews(): Promise<CarReview[]> {
  return apiRequest<CarReview[]>('/car-reviews', { auth: true });
}

export async function createCarReview(payload: CarReviewRequest): Promise<CarReview> {
  return apiRequest<CarReview>('/car-reviews', { method: 'POST', body: payload, auth: true });
}

export async function updateCarReview(id: string, payload: CarReviewRequest): Promise<CarReview> {
  return apiRequest<CarReview>(`/car-reviews/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteCarReview(id: string): Promise<void> {
  return apiRequest<void>(`/car-reviews/${id}`, { method: 'DELETE', auth: true });
}

export async function replyToReview(
  reviewId: string,
  author: string,
  content: string,
): Promise<CarReview> {
  return apiRequest<CarReview>(
    `/car-reviews/${reviewId}/replies?author=${encodeURIComponent(author)}&content=${encodeURIComponent(content)}`,
    { method: 'POST', auth: true },
  );
}
