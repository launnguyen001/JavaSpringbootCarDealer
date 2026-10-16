export type { CarStatus } from './car';
export type { Car } from './car';
export type { Brand } from './brand';
export type { CarModel } from './carModel';
export type { CarSpecification } from './carSpec';
export type { CarMedia } from './carMedia';
export type { Reply } from './carReview';
export type { CarReview } from './carReview';
export type { CarReviewSummary } from './carReview';

export type { UserRole, AuthUser, AuthResponse, RegisterRequest } from './user';
export type { OrderStatus, OrderItem, Order, OrderItemRequest, OrderRequest } from './order';
export type { Customer, CustomerRequest } from './customer';
export type { Employee, EmployeeRequest } from './employee';
export type { AppointmentStatus, TestDrive, TestDriveRequest } from './testDrive';
export type { CartItem } from './cart';
export type { PaymentMethod, PaymentStatus, Payment, PaymentRequest } from './payment';
export type { Contract, ContractRequest } from './contract';
export type { Invoice, InvoiceRequest } from './invoice';
export type { ActivityLog } from './activityLog';
export type {
  DashboardSummary,
  RevenueSummary,
  OrderStatusCount,
  TopSellingCar,
  InventoryByBrand,
  InventoryByModel,
  CarInventoryItem,
  OrderSummary,
  CarSalesOrderLine,
  RevenueByMonth,
} from './report';

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
