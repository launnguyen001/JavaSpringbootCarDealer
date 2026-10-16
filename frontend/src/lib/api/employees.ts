import { apiRequest } from './http';
import type { Employee, EmployeeRequest } from '@/types';

export async function listEmployees(): Promise<Employee[]> {
  return apiRequest<Employee[]>('/employees', { auth: true });
}

export async function getEmployee(id: number): Promise<Employee> {
  return apiRequest<Employee>(`/employees/${id}`, { auth: true });
}

export async function createEmployee(payload: EmployeeRequest): Promise<Employee> {
  return apiRequest<Employee>('/employees', { method: 'POST', body: payload, auth: true });
}

export async function updateEmployee(id: number, payload: EmployeeRequest): Promise<Employee> {
  return apiRequest<Employee>(`/employees/${id}`, { method: 'PUT', body: payload, auth: true });
}

export async function deleteEmployee(id: number): Promise<void> {
  return apiRequest<void>(`/employees/${id}`, { method: 'DELETE', auth: true });
}

export async function searchEmployeesByName(name: string): Promise<Employee[]> {
  return apiRequest<Employee[]>(`/employees/search/name?name=${encodeURIComponent(name)}`, {
    auth: true,
  });
}
