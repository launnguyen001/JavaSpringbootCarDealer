export type CarStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'MAINTENANCE';

export interface Car {
  id: number;
  name: string;
  description: string | null;
  price: number;
  quantity: number;
  vin: string | null;
  year: number | null;
  color: string | null;
  status: CarStatus;
  modelId: number | null;
  modelName: string | null;
  brandName: string | null;
}
