// 'use client';

// import ResourceManager from '@/components/admin/ResourceManager';
// import Button from '@/components/ui/Button';
// import { useToast } from '@/components/ui/Toast';
// import { getErrorMessage } from '@/lib/utils/error';
// import {
//   listContracts,
//   createContract,
//   updateContract,
//   getOrderOptions,
//   downloadFile,
// } from '@/lib/api';
// import { formatVnd, formatDateTime } from '@/lib/utils/format';
// import type { Contract } from '@/types';
// import contractFields from '@/components/admin/contractFields';

// export default function AdminContractsPage() {
//   const toast = useToast();

//   const downloadPdf = async (contract: Contract) => {
//     try {
//       await downloadFile(`/contracts/${contract.id}/pdf`, `hop-dong-${contract.id}.pdf`);
//     } catch (err) {
//       toast.error(getErrorMessage(err));
//     }
//   };

//   return (
//     <ResourceManager<Contract>
//       title="Hợp đồng"
//       description="Quản lý hợp đồng mua bán và tải bản PDF."
//       list={listContracts}
//       create={createContract}
//       update={updateContract}
//       canDelete={false}
//       rowActions={(row) => (
//         <Button size="sm" variant="ghost" onClick={() => downloadPdf(row)}>
//           PDF
//         </Button>
//       )}
//       getId={(row) => row.id}
//       toForm={(row) => ({
//         orderId: row.orderId,
//         contractNumber: row.contractNumber,
//         signedAt: row.signedAt ?? '',
//         content: row.content ?? '',
//       })}
//       fields={[
//         {
//           name: 'orderId',
//           label: 'Đơn hàng',
//           type: 'select',
//           required: true,
//           hideOnEdit: true,
//           placeholder: '— Chọn đơn chưa có hợp đồng —',
//           optionsLoader: () =>
//             getOrderOptions('contract').then((orders) =>
//               orders.map((o) => ({
//                 value: o.orderId,
//                 label: `${o.orderCode} — ${o.customerName ?? 'Khách lẻ'} — ${formatVnd(o.totalAmount)}`,
//               })),
//             ),
//         },
//         { name: 'contractNumber', label: 'Số hợp đồng', required: true },
//         { name: 'signedAt', label: 'Ngày ký', type: 'datetime' },
//         { name: 'content', label: 'Nội dung', type: 'textarea' },
//       ]}
//       columns={[
//         { key: 'id', label: 'ID' },
//         { key: 'contractNumber', label: 'Số hợp đồng' },
//         { key: 'orderCode', label: 'Mã đơn', render: (r) => r.orderCode ?? '—' },
//         {
//           key: 'signedAt',
//           label: 'Ngày ký',
//           render: (r) => (r.signedAt ? formatDateTime(r.signedAt) : '—'),
//         },
//       ]}
//     />
//   );
// }
'use client';

import ResourceManager from '@/components/admin/ResourceManager';
import Button from '@/components/ui/Button';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import {
  listContracts,
  createContract,
  updateContract,
  downloadFile,
} from '@/lib/api';
import { formatDateTime } from '@/lib/utils/format';
import type { Contract } from '@/types';
import { contractFields } from '@/components/admin/contractFields';

export default function AdminContractsPage() {
  const toast = useToast();

  const downloadPdf = async (contract: Contract) => {
    try {
      await downloadFile(`/contracts/${contract.id}/pdf`, `hop-dong-${contract.id}.pdf`);
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  return (
    <ResourceManager<Contract>
      title="Hợp đồng"
      description="Quản lý hợp đồng mua bán và tải bản PDF."
      list={listContracts}
      create={createContract}
      update={updateContract}
      canDelete={false}
      rowActions={(row) => (
        <Button size="sm" variant="ghost" onClick={() => downloadPdf(row)}>
          PDF
        </Button>
      )}
      getId={(row) => row.id}
      toForm={(row) => ({
        orderId: row.orderId,
        contractNumber: row.contractNumber,
        signedAt: row.signedAt ?? '',
        content: row.content ?? '',
      })}
      fields={contractFields}
      columns={[
        { key: 'id', label: 'ID' },
        { key: 'contractNumber', label: 'Số hợp đồng' },
        { key: 'orderCode', label: 'Mã đơn', render: (r) => r.orderCode ?? '—' },
        {
          key: 'signedAt',
          label: 'Ngày ký',
          render: (r) => (r.signedAt ? formatDateTime(r.signedAt) : '—'),
        },
      ]}
    />
  );
}