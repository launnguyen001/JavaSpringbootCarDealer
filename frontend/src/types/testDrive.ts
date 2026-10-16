export type AppointmentStatus = 'SCHEDULED' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED';

export interface TestDrive {
  id: number;
  scheduledAt: string;
  status: AppointmentStatus;
  note: string | null;
  customerId: number;
  customerName: string;
  carId: number;
  carName: string;
  employeeId: number | null;
  employeeName: string | null;
}

export interface TestDriveRequest {
  scheduledAt: string;
  status?: AppointmentStatus;
  note?: string | null;
  customerId: number;
  carId: number;
  employeeId?: number | null;
}
