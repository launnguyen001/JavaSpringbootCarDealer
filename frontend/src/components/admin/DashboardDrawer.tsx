'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
import InvoiceQuickViewModal from '@/components/admin/InvoiceQuickViewModal';
import { getInventoryCars } from '@/lib/api';
import { formatVnd } from '@/lib/utils/format';
import type { CarInventoryItem } from '@/types';

type Props = { status: string | null; title: string; count: number; brandId?: number; modelId?: number; from: string; to: string; onClose: () => void };
const baseColumns: Column<CarInventoryItem>[] = [
 { key:'carName', label:'Mẫu xe' },
 { key:'vin', label:'VIN', render:r=>r.vin ?? '—' },
 { key:'color', label:'Màu', render:r=>r.color ?? '—' },
 { key:'year', label:'Năm', render:r=>r.year ?? '—' },
 { key:'condition', label:'Tình trạng', render:r=>r.condition==='NEW'?'Mới':r.condition==='USED'?'Đã qua sử dụng':'—' },
 { key:'price', label:'Giá', className:'text-right', render:r=>formatVnd(r.price) },
 { key:'status', label:'Trạng thái', render:r=><StatusPill status={r.status}/> },
];
export default function DashboardDrawer({status,title,count,brandId,modelId,from,to,onClose}:Props) {
 const router=useRouter();
 const [cars,setCars]=useState<CarInventoryItem[]>([]);
 const [loading,setLoading]=useState(true);
 const [error,setError]=useState('');
 const [invoiceVin,setInvoiceVin]=useState<string|null>(null);
 const [invoiceCarName,setInvoiceCarName]=useState<string|null>(null);
 useEffect(()=>{
  let active=true; setLoading(true); setError('');
  getInventoryCars({status:status ?? undefined,brandId,modelId,from,to})
   .then(rows=>{if(active)setCars(rows)})
   .catch(()=>{if(active)setError('Không thể tải danh sách xe. Vui lòng thử lại.')})
   .finally(()=>{if(active)setLoading(false)});
  return ()=>{active=false};
 },[status,brandId,modelId,from,to]);
 useEffect(()=>{
  const key=(e:KeyboardEvent)=>{if(e.key==='Escape')onClose()};
  window.addEventListener('keydown',key); return ()=>window.removeEventListener('keydown',key);
 },[onClose]);
 const columns:Column<CarInventoryItem>[]=[...baseColumns,{
  key:'__quickView',
  label:'',
  className:'text-right whitespace-nowrap',
  render:r=><button type="button" disabled={!r.vin} onClick={event=>{event.stopPropagation();if(!r.vin)return;setInvoiceVin(r.vin);setInvoiceCarName(r.carName)}} title={r.vin?'Xem nhanh hóa đơn':'Xe chưa có VIN nên chưa tra được hóa đơn'} className="group/quick inline-flex items-center gap-1.5 rounded-full border border-[#e5e7eb] bg-white px-2.5 py-1 text-[11px] font-medium text-[#4b5563] transition-all duration-150 hover:-translate-y-px hover:border-[#c58e00]/60 hover:bg-[#fffbeb] hover:text-[#8a6d00] hover:shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#c58e00]/40 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:translate-y-0 disabled:hover:border-[#e5e7eb] disabled:hover:bg-white disabled:hover:text-[#4b5563]">
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" className="transition-transform group-hover/quick:scale-110"><path d="M6 3h12v18l-3-2-3 2-3-2-3 2z"/><path d="M9 8h6M9 12h6"/></svg>
    Xem nhanh
  </button>,
}];
 return <div className="fixed inset-0 z-[100]">
  <button aria-label="Đóng bảng chi tiết" className="absolute inset-0 bg-black/40 backdrop-blur-[1px]" onClick={onClose}/>
  <aside className="absolute right-0 top-0 flex h-full w-full max-w-[min(1100px,82vw)] flex-col bg-white shadow-2xl">
   <header className="flex items-center justify-between border-b border-[#e5e7eb] px-6 py-5">
    <div><p className="text-[11px] font-semibold uppercase tracking-wider text-[#9ca3af]">Chi tiết tồn kho</p><h2 className="mt-1 text-xl font-semibold text-[#111827]">{title}</h2><p className="mt-1 text-sm text-[#6b7280]">{count.toLocaleString('vi-VN')} xe trong chỉ số này</p></div>
    <button onClick={onClose} aria-label="Đóng" className="rounded-xl p-2 text-xl text-[#6b7280] transition-colors hover:bg-[#f3f4f6] hover:text-[#111827] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#c58e00]/40">×</button>
   </header>
   <div className="flex-1 overflow-auto p-6">{loading?<p className="py-16 text-center text-sm text-[#9ca3af]">Đang tải danh sách xe…</p>:error?<p className="py-16 text-center text-sm text-red-600">{error}</p>:<DataTable columns={columns} rows={cars} rowKey={r=>r.carId} empty="Không có xe phù hợp với bộ lọc hiện tại." onRowClick={r=>router.push(`/admin/car-items/${r.carId}`)}/>}</div>
   <footer className="border-t border-[#e5e7eb] px-6 py-3 text-xs text-[#9ca3af]">Bấm “Xem nhanh” để xem hóa đơn tại chỗ, hoặc bấm vào dòng để mở trang chi tiết xe.</footer>
  </aside>
  <InvoiceQuickViewModal open={invoiceVin!==null} vin={invoiceVin} carName={invoiceCarName} onClose={()=>{setInvoiceVin(null);setInvoiceCarName(null)}}/>
 </div>;
}

