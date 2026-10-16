import { apiRequest } from './http';
import type { Contract, ContractRequest } from '@/types';

export async function listContracts(): Promise<Contract[]> {
  return apiRequest<Contract[]>('/contracts', { auth: true });
}

export async function getContract(id: number): Promise<Contract> {
  return apiRequest<Contract>(`/contracts/${id}`, { auth: true });
}

export async function getContractByOrder(orderId: number): Promise<Contract> {
  return apiRequest<Contract>(`/contracts/order/${orderId}`, { auth: true });
}

export async function createContract(payload: ContractRequest): Promise<Contract> {
  return apiRequest<Contract>('/contracts', { method: 'POST', body: payload, auth: true });
}

export async function updateContract(id: number, payload: ContractRequest): Promise<Contract> {
  return apiRequest<Contract>(`/contracts/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteContract(id: number): Promise<void> {
  return apiRequest<void>(`/contracts/${id}`, { method: 'DELETE', auth: true });
}
