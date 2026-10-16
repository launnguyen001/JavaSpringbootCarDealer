export interface Employee {
  id: number;
  fullName: string;
  email: string | null;
  phone: string | null;
  position: string | null;
}

export interface EmployeeRequest {
  fullName: string;
  email?: string | null;
  phone?: string | null;
  position?: string | null;
}
