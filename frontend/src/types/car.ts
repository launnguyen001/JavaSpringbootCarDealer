export type CarStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'MAINTENANCE';

/** Tình trạng xe: mới hoặc đã qua sử dụng. */
export type CarCondition = 'NEW' | 'USED';

/**
 * Một chiếc xe vật lý (bảng `car_items`).
 * Mỗi chiếc có VIN, màu, năm, tình trạng (mới/cũ), giá và trạng thái riêng.
 */
export interface CarItem {
  id: number;
  carId: number;
  carName: string;
  vin: string | null;
  color: string | null;
  year: number | null;
  condition: CarCondition;
  price: number;
  status: CarStatus;
  mileageKm: number | null;
  note: string | null;
  modelId: number | null;
  modelName: string | null;
  brandName: string | null;
}

/**
 * Dòng xe (bảng `cars`) — thông tin sản phẩm kèm số liệu tổng hợp tính từ các chiếc.
 * `items` chỉ được điền ở API chi tiết (`GET /api/cars/{id}`); ở danh sách là mảng rỗng.
 */
export interface Car {
  id: number;
  name: string;
  description: string | null;
  basePrice: number;
  priceFrom: number | null;
  priceTo: number | null;
  totalItems: number;
  availableItems: number;
  reservedItems: number;
  soldItems: number;
  maintenanceItems: number;
  conditions: CarCondition[];
  status: CarStatus;
  modelId: number | null;
  modelName: string | null;
  brandName: string | null;
  items: CarItem[];
}
