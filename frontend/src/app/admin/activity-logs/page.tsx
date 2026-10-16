'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import { listActivityLogs } from '@/lib/api';
import { formatDateTime } from '@/lib/utils/format';
import type { ActivityLog } from '@/types';

export default function AdminActivityLogsPage() {
  return (
    <ResourceManager<ActivityLog>
      title="Nhật ký hoạt động"
      description="Lịch sử thao tác trên hệ thống (chỉ đọc)."
      list={listActivityLogs}
      getId={(row) => row.id}
      canCreate={false}
      canEdit={false}
      canDelete={false}
      fields={[]}
      columns={[
        { key: 'action', label: 'Hành động' },
        { key: 'entityType', label: 'Đối tượng' },
        { key: 'entityId', label: 'ID', className: 'text-center' },
        { key: 'performedBy', label: 'Người thực hiện', render: (r) => r.performedBy ?? '—' },
        { key: 'description', label: 'Mô tả', render: (r) => r.description ?? '—' },
        {
          key: 'timestamp',
          label: 'Thời gian',
          render: (r) => (r.timestamp ? formatDateTime(r.timestamp) : '—'),
        },
      ]}
    />
  );
}
