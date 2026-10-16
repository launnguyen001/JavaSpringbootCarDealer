export interface ActivityLog {
  id: string;
  action: string;
  entityType: string;
  entityId: number;
  description: string | null;
  performedBy: string | null;
  timestamp: string | null;
}
