import { apiRequest } from './http';
import type {
  CarInventoryItem,
  CarSalesOrderLine,
  DashboardSummary,
  DashboardData,
  InventoryByBrand,
  InventoryByModel,
  OrderStatusCount,
  OrderSummary,
  RevenueByMonth,
  RevenueCategory,
  RevenueOrderSummary,
  RevenueSummary,
  TopSellingCar,
} from '@/types';

// ---- Báo cáo cơ bản ----
export async function getTopSellingCars(limit = 5): Promise<TopSellingCar[]> {
  return apiRequest<TopSellingCar[]>(`/reports/top-selling-cars?limit=${limit}`, { auth: true });
}

export async function getInventoryByBrand(): Promise<InventoryByBrand[]> {
  return apiRequest<InventoryByBrand[]>('/reports/inventory-by-brand', { auth: true });
}

export async function getRevenueSummary(): Promise<RevenueSummary> {
  return apiRequest<RevenueSummary>('/reports/revenue-summary', { auth: true });
}

export async function getOrdersByStatusCount(): Promise<OrderStatusCount[]> {
  return apiRequest<OrderStatusCount[]>('/reports/orders-by-status', { auth: true });
}

// ---- Dashboard & drilldown ----
export async function getDashboardData(filters: { from: string; to: string; brandId?: number; modelId?: number }): Promise<DashboardData> {
  const params = new URLSearchParams({ from: filters.from, to: filters.to });
  if (filters.brandId) params.set("brandId", String(filters.brandId));
  if (filters.modelId) params.set("modelId", String(filters.modelId));
  return apiRequest<DashboardData>("/reports/dashboard?" + params.toString(), { auth: true });
}

export async function getInventoryCars(filters: { status?: string; from: string; to: string; brandId?: number; modelId?: number }): Promise<CarInventoryItem[]> {
  const params = new URLSearchParams();
  if (filters.status) params.set("status", filters.status);
  params.set("from", filters.from);
  params.set("to", filters.to);
  if (filters.brandId) params.set("brandId", String(filters.brandId));
  if (filters.modelId) params.set("modelId", String(filters.modelId));
  return apiRequest<CarInventoryItem[]>("/reports/inventory-cars?" + params.toString(), { auth: true });
}

export async function getDashboardSummary(): Promise<DashboardSummary> {
  return apiRequest<DashboardSummary>('/reports/dashboard-summary', { auth: true });
}

export async function getModelsByBrand(brandId: number): Promise<InventoryByModel[]> {
  return apiRequest<InventoryByModel[]>(`/reports/inventory-by-brand/${brandId}/models`, {
    auth: true,
  });
}

export async function getModelInventoryCars(modelId: number): Promise<CarInventoryItem[]> {
  return apiRequest<CarInventoryItem[]>(`/reports/inventory-by-model/${modelId}/cars`, {
    auth: true,
  });
}

export async function getOrdersByCar(carId: number): Promise<CarSalesOrderLine[]> {
  return apiRequest<CarSalesOrderLine[]>(`/reports/top-selling-cars/${carId}/orders`, {
    auth: true,
  });
}

export async function getOrdersByStatus(
  status: string,
  filters?: { from?: string; to?: string; brandId?: number; modelId?: number },
): Promise<OrderSummary[]> {
  const params = new URLSearchParams();
  if (filters?.from) params.set('from', filters.from);
  if (filters?.to) params.set('to', filters.to);
  if (filters?.brandId) params.set('brandId', String(filters.brandId));
  if (filters?.modelId) params.set('modelId', String(filters.modelId));
  const query = params.toString();
  return apiRequest<OrderSummary[]>(
    `/reports/orders-by-status/${encodeURIComponent(status)}${query ? `?${query}` : ''}`,
    { auth: true },
  );
}

export async function getRevenueByMonth(): Promise<RevenueByMonth[]> {
  return apiRequest<RevenueByMonth[]>('/reports/revenue-by-month', { auth: true });
}


export async function getOrdersByRevenue(
  period: string,
  category: RevenueCategory,
  filters?: { brandId?: number; modelId?: number },
): Promise<RevenueOrderSummary[]> {
  const params = new URLSearchParams({ category });
  if (filters?.brandId) params.set('brandId', String(filters.brandId));
  if (filters?.modelId) params.set('modelId', String(filters.modelId));
  return apiRequest<RevenueOrderSummary[]>(
    `/reports/revenue-by-month/${encodeURIComponent(period)}/orders?${params.toString()}`,
    { auth: true },
  );
}
