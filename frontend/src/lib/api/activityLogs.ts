import { apiRequest } from './http';
import type { ActivityLog } from '@/types';

export async function listActivityLogs(): Promise<ActivityLog[]> {
  return apiRequest<ActivityLog[]>('/activity-logs', { auth: true });
}

export async function getActivityLogsByEntity(
  entityType: string,
  entityId: number,
): Promise<ActivityLog[]> {
  return apiRequest<ActivityLog[]>(
    `/activity-logs/entity?entityType=${encodeURIComponent(entityType)}&entityId=${entityId}`,
    { auth: true },
  );
}
