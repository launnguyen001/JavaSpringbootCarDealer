export interface Customer {
  id: number;
  fullName: string;
  email: string | null;
  phone: string | null;
  address: string | null;
  nationalId: string | null;
}

export interface CustomerRequest {
  fullName: string;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  nationalId?: string | null;
}
