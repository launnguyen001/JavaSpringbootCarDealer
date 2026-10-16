// ---- Dashboard & báo cáo (khớp DTO backend) ----

export interface DashboardSummary {
  totalCars: number;
  availableCars: number;
  reservedCars: number;
  soldCars: number;
  maintenanceCars: number;
  totalCustomers: number;
  totalEmployees: number;
  totalOrders: number;
  scheduledTestDrives: number;
  totalRevenue: number;
}

export interface RevenueSummary {
  totalOrders: number;
  totalRevenue: number;
}

export interface OrderStatusCount {
  status: string;
  total: number;
}

export interface TopSellingCar {
  carId: number;
  carName: string;
  totalSold: number;
  revenue: number;
}

export interface InventoryByBrand {
  brandId: number;
  brandName: string;
  totalCars: number;
  totalQuantity: number;
}

export interface InventoryByModel {
  modelId: number;
  modelName: string;
  bodyType: string | null;
  totalCars: number;
  totalQuantity: number;
}

export interface CarInventoryItem {
  carId: number;
  carName: string;
  vin: string | null;
  color: string | null;
  year: number | null;
  status: string;
  quantity: number | null;
  price: number;
}

export interface OrderSummary {
  orderId: number;
  orderCode: string;
  orderDate: string;
  status: string;
  totalAmount: number;
  customerName: string | null;
  employeeName: string | null;
}

export interface CarSalesOrderLine {
  orderId: number;
  orderCode: string;
  orderDate: string;
  status: string;
  customerName: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
}

export interface RevenueByMonth {
  period: string;
  totalOrders: number;
  totalRevenue: number;
}
