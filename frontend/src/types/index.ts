export type { CarStatus, CarCondition, Car, CarItem } from './car';
export type { Brand } from './brand';
export type { CarModel } from './carModel';
export type { CarItemRequest, CarItemDetail } from './carItem';
export type { CarSpecification } from './carSpec';
export type { CarMedia } from './carMedia';
export type { Reply } from './carReview';
export type { CarReview } from './carReview';
export type { CarReviewSummary } from './carReview';

export type {
  UserRole,
  AuthUser,
  AuthResponse,
  RegisterRequest,
  UpdateMeRequest,
  UserStatusRequest,
  UserRoleRequest,
} from './user';
export type { OrderStatus, OrderItem, Order, OrderItemRequest, OrderRequest, OrderOption, OrderOptionTarget, OrderStatusHistory } from './order';
export type { Customer, CustomerRequest } from './customer';
export type { Employee, EmployeeRequest } from './employee';
export type { AppointmentStatus, TestDrive, TestDriveRequest } from './testDrive';
export type { CartItem } from './cart';
export type { PaymentMethod, PaymentStatus, Payment, PaymentRequest } from './payment';
export type { Contract, ContractRequest } from './contract';
export type { Invoice, InvoiceRequest, InvoicePaymentStatus } from './invoice';
export type { ActivityLog } from './activityLog';
export type { NotificationType, AppNotification } from './notification';
export type {
  ChatRoomType,
  ChatMessageType,
  ChatRoom,
  ChatMessage,
  ChatMessageRequest,
  ChatRoomRequest,
  ChatMessagePage,
  ChatUser,
  ChatRealtimeEvent,
  ChatReadState,
  ChatPresenceEvent,
} from './chat';
export type {
  DashboardSummary,
  DashboardData,
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
