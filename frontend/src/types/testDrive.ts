export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface TestDrive {
  id: number;
  scheduledAt: string;
  status: AppointmentStatus;
  note: string | null;
  customerId: number;
  customerName: string;
  carItemId: number;
  carId: number;
  carName: string;
  vin: string | null;
  employeeId: number | null;
  employeeName: string | null;
}

export interface TestDriveRequest {
  scheduledAt: string;
  status?: AppointmentStatus;
  note?: string | null;
  customerId: number;
  /** Chiếc xe cụ thể khách muốn lái thử. */
  carItemId: number;
  employeeId?: number | null;
}
