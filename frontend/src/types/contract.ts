export interface Contract {
  id: number;
  orderId: number;
  orderCode: string;
  contractNumber: string;
  signedAt: string | null;
  content: string | null;
}

export interface ContractRequest {
  orderId: number;
  contractNumber: string;
  signedAt?: string | null;
  content?: string | null;
}
