import type { CarCondition, CarStatus } from './car';
import type { CarSpecification } from './carSpec';
import type { Invoice } from './invoice';
import type { Contract } from './contract';
import type { Payment } from './payment';
import type { OrderSummary } from './report';

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


/** Chi tiết xe vật lý kèm thông số MongoDB và chứng từ của đơn liên quan. */
export interface CarItemDetail {
  vehicle: import('./car').CarItem;
  specifications: CarSpecification | null;
  relatedOrder: OrderSummary | null;
  invoice: Invoice | null;
  contract: Contract | null;
  payments: Payment[];
}
