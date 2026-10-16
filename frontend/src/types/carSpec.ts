export interface CarSpecification {
  id: string;
  carId: number;
  engine: string | null;
  transmission: string | null;
  fuelType: string | null;
  seats: number | null;
  dimensions: string | null;
  weightKg: number | null;
  safetyFeatures: string[];
  extra: Record<string, unknown>;
  updatedAt: string;
}
