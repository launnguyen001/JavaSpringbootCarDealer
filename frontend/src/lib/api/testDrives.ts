import { apiRequest } from './http';
import type { AppointmentStatus, TestDrive, TestDriveRequest } from '@/types';

export async function listTestDrives(): Promise<TestDrive[]> {
  return apiRequest<TestDrive[]>('/test-drives', { auth: true });
}

export async function getTestDrive(id: number): Promise<TestDrive> {
  return apiRequest<TestDrive>(`/test-drives/${id}`, { auth: true });
}

export async function getTestDrivesByCustomer(customerId: number): Promise<TestDrive[]> {
  return apiRequest<TestDrive[]>(`/test-drives/customer/${customerId}`, { auth: true });
}

export async function createTestDrive(payload: TestDriveRequest): Promise<TestDrive> {
  return apiRequest<TestDrive>('/test-drives', { method: 'POST', body: payload, auth: true });
}

export async function updateTestDrive(id: number, payload: TestDriveRequest): Promise<TestDrive> {
  return apiRequest<TestDrive>(`/test-drives/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteTestDrive(id: number): Promise<void> {
  return apiRequest<void>(`/test-drives/${id}`, { method: 'DELETE', auth: true });
}

export async function updateTestDriveStatus(
  id: number,
  status: AppointmentStatus,
): Promise<TestDrive> {
  return apiRequest<TestDrive>(`/test-drives/${id}/status?status=${encodeURIComponent(status)}`, {
    method: 'PATCH',
    auth: true,
  });
}

export async function cancelTestDrive(id: number): Promise<TestDrive> {
  return apiRequest<TestDrive>(`/test-drives/${id}/cancel`, { method: 'POST', auth: true });
}
