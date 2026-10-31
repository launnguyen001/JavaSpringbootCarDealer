'use client';
import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
import { getInventoryCars } from '@/lib/api';
import { formatVnd } from '@/lib/utils/format';
import type { CarInventoryItem } from '@/types';

type Props = { status: string | null; title: string; count: number; brandId?: number; modelId?: number; from: string; to: string; onClose: () => void };
const columns: Column<CarInventoryItem>[] = [
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
 return <div className="fixed inset-0 z-[100]">
  <button aria-label="Đóng bảng chi tiết" className="absolute inset-0 bg-black/40" onClick={onClose}/>
  <aside className="absolute right-0 top-0 flex h-full w-full max-w-[min(1100px,82vw)] flex-col bg-white shadow-2xl">
   <header className="flex items-center justify-between border-b border-[#e5e7eb] px-6 py-5">
    <div><p className="text-[11px] font-semibold uppercase tracking-wider text-[#9ca3af]">Chi tiết tồn kho</p><h2 className="mt-1 text-xl font-semibold text-[#111827]">{title}</h2><p className="mt-1 text-sm text-[#6b7280]">{count.toLocaleString('vi-VN')} xe trong chỉ số này</p></div>
    <button onClick={onClose} aria-label="Đóng" className="rounded-xl p-2 text-xl text-[#6b7280] hover:bg-[#f3f4f6]">×</button>
   </header>
   <div className="flex-1 overflow-auto p-6">{loading?<p className="py-16 text-center text-sm text-[#9ca3af]">Đang tải danh sách xe…</p>:error?<p className="py-16 text-center text-sm text-red-600">{error}</p>:<DataTable columns={columns} rows={cars} rowKey={r=>r.carId} empty="Không có xe phù hợp với bộ lọc hiện tại." onRowClick={r=>router.push(`/admin/car-items/${r.carId}`)}/>}</div>
   <footer className="border-t border-[#e5e7eb] px-6 py-3 text-xs text-[#9ca3af]">Danh sách kế thừa bộ lọc hãng, model và khoảng ngày từ Dashboard.</footer>
  </aside>
 </div>;
}
