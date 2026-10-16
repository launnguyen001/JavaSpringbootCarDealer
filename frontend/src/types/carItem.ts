import type { CarCondition, CarStatus } from './car';

/** Form tạo/sửa MỘT chiếc xe vật lý (car_items). */
export interface CarItemRequest {
  vin?: string | null;
  color?: string | null;
  year?: number | null;
  condition?: CarCondition;
  price?: number | null;
  status?: CarStatus;
  mileageKm?: number | null;
  note?: string | null;
}
