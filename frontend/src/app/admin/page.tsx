'use client';

import { useCallback, useEffect, useState } from 'react';
import KpiCard from '@/components/ui/KpiCard';
import BarChart from '@/components/ui/BarChart';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
import Modal from '@/components/ui/Modal';
import OrderDetailModal from '@/components/admin/OrderDetailModal';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import { formatVnd, formatDateTime } from '@/lib/utils/format';
import {
  getDashboardSummary,
  getInventoryByBrand,
  getOrdersByStatus,
  getOrdersByStatusCount,
  getRevenueByMonth,
  getTopSellingCars,
  getModelsByBrand,
  getModelInventoryCars,
  getOrdersByCar,
  listInvoices,
} from '@/lib/api';
import type {
  CarInventoryItem,
  CarSalesOrderLine,
  DashboardSummary,
  InventoryByBrand,
  InventoryByModel,
  Invoice,
  OrderStatusCount,
  OrderSummary,
  RevenueByMonth,
  TopSellingCar,
} from '@/types';

const statusColors: Record<string, string> = {
  PENDING: '#f59e0b',
  CONFIRMED: '#2563eb',
  COMPLETED: '#10b981',
  CANCELLED: '#ef4444',
};

const orderColumns: Column<OrderSummary>[] = [
  { key: 'orderCode', label: 'Mã đơn' },
  { key: 'customerName', label: 'Khách hàng', render: (r) => r.customerName ?? '—' },
  { key: 'orderDate', label: 'Ngày', render: (r) => formatDateTime(r.orderDate) },
  {
    key: 'totalAmount',
    label: 'Tổng tiền',
    className: 'text-right',
    render: (r) => formatVnd(r.totalAmount),
  },
  { key: 'status', label: 'Trạng thái', render: (r) => <StatusPill status={r.status} /> },
];

// Hóa đơn gần đây trên dashboard — nhấp để mở popup chi tiết đơn hàng
const invoiceColumns: Column<Invoice>[] = [
  { key: 'invoiceNumber', label: 'Số hóa đơn' },
  { key: 'orderCode', label: 'Mã đơn', render: (r) => r.orderCode ?? '—' },
  {
    key: 'total',
    label: 'Tổng tiền',
    className: 'text-right',
    render: (r) => formatVnd(r.total),
  },
  {
    key: 'issuedAt',
    label: 'Ngày phát hành',
    render: (r) => (r.issuedAt ? formatDateTime(r.issuedAt) : '—'),
  },
];

/** Drilldown đang mở (hiển thị dạng popup thay vì tại chỗ). */
type Drill =
  | { type: 'status'; status: string }
  | { type: 'brand'; brand: InventoryByBrand }
  | { type: 'car'; car: TopSellingCar };

const carLineColumns: Column<CarSalesOrderLine>[] = [
  { key: 'orderCode', label: 'Mã đơn' },
  { key: 'customerName', label: 'Khách hàng', render: (r) => r.customerName ?? '—' },
  { key: 'quantity', label: 'SL', className: 'text-center' },
  { key: 'orderDate', label: 'Ngày', render: (r) => formatDateTime(r.orderDate) },
  {
    key: 'lineTotal',
    label: 'Thành tiền',
    className: 'text-right',
    render: (r) => formatVnd(r.lineTotal),
  },
  { key: 'status', label: 'Trạng thái', render: (r) => <StatusPill status={r.status} /> },
];

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Cũ' };

const carInventoryColumns: Column<CarInventoryItem>[] = [
  { key: 'carName', label: 'Xe' },
  { key: 'vin', label: 'VIN', render: (r) => r.vin ?? '—' },
  { key: 'color', label: 'Màu', render: (r) => r.color ?? '—' },
  { key: 'year', label: 'Năm', render: (r) => r.year ?? '—' },
  {
    key: 'condition',
    label: 'Tình trạng',
    render: (r) =>
      r.condition ? (CONDITION_LABEL[r.condition] ?? r.condition) : '—',
  },
  { key: 'price', label: 'Giá', className: 'text-right', render: (r) => formatVnd(r.price) },
  { key: 'status', label: 'Trạng thái', render: (r) => <StatusPill status={r.status} /> },
];

export default function AdminDashboardPage() {
  const toast = useToast();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [inventory, setInventory] = useState<InventoryByBrand[]>([]);
  const [statusCounts, setStatusCounts] = useState<OrderStatusCount[]>([]);
  const [topCars, setTopCars] = useState<TopSellingCar[]>([]);
  const [revenueByMonth, setRevenueByMonth] = useState<RevenueByMonth[]>([]);
  const [loading, setLoading] = useState(true);

  // drilldown states — hiển thị dạng popup
  const [drill, setDrill] = useState<Drill | null>(null);
  const [drillLoading, setDrillLoading] = useState(false);
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [models, setModels] = useState<InventoryByModel[]>([]);
  const [selectedModel, setSelectedModel] = useState<InventoryByModel | null>(null);
  const [cars, setCars] = useState<CarInventoryItem[]>([]);
  const [carOrders, setCarOrders] = useState<CarSalesOrderLine[]>([]);

  // Hóa đơn gần đây (nhấp để mở popup chi tiết đơn hàng dùng chung)
  const [recentInvoices, setRecentInvoices] = useState<Invoice[]>([]);
  const [detailOrderId, setDetailOrderId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [s, inv, sc, tc, rbm, invs] = await Promise.all([
        getDashboardSummary(),
        getInventoryByBrand(),
        getOrdersByStatusCount(),
        getTopSellingCars(5),
        getRevenueByMonth(),
        listInvoices().catch(() => [] as Invoice[]),
      ]);
      setSummary(s);
      setInventory(inv);
      setStatusCounts(sc);
      setTopCars(tc);
      setRevenueByMonth(rbm);
      // Hóa đơn gần đây: sắp xếp theo ngày phát hành giảm dần, lấy 8 bản mới nhất
      setRecentInvoices(
        [...invs]
          .sort(
            (a, b) =>
              (b.issuedAt ? Date.parse(b.issuedAt) : 0) - (a.issuedAt ? Date.parse(a.issuedAt) : 0),
          )
          .slice(0, 8),
      );
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const openStatusDrill = async (status: string) => {
    setDrill({ type: 'status', status });
    setOrders([]);
    setDrillLoading(true);
    try {
      setOrders(await getOrdersByStatus(status));
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDrillLoading(false);
    }
  };

  const openBrandDrill = async (brand: InventoryByBrand) => {
    setDrill({ type: 'brand', brand });
    setSelectedModel(null);
    setCars([]);
    setModels([]);
    setDrillLoading(true);
    try {
      setModels(await getModelsByBrand(brand.brandId));
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDrillLoading(false);
    }
  };

  const drillModel = async (model: InventoryByModel) => {
    setSelectedModel(model);
    setCars([]);
    try {
      setCars(await getModelInventoryCars(model.modelId));
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const openCarDrill = async (car: TopSellingCar) => {
    setDrill({ type: 'car', car });
    setCarOrders([]);
    setDrillLoading(true);
    try {
      setCarOrders(await getOrdersByCar(car.carId));
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setDrillLoading(false);
    }
  };

  const closeDrill = () => {
    setDrill(null);
    setSelectedModel(null);
  };

  if (loading || !summary) {
    return <div className="py-20 text-center text-[#9ca3af] text-sm">Đang tải dashboard…</div>;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-serif font-bold text-[#0a0a0a]">Dashboard</h1>
        <p className="text-sm text-[#6b7280] mt-1">
          Tổng quan hoạt động kinh doanh — nhấp vào biểu đồ hoặc dòng dữ liệu để xem chi tiết trong popup.
        </p>
      </div>

      {/* KPI */}
      <div className="grid grid-cols-2 md:grid-cols-3 xl:grid-cols-5 gap-4">
        <KpiCard label="Tổng xe" value={summary.totalCars} hint={`${summary.availableCars} sẵn sàng`} />
        <KpiCard label="Đã bán" value={summary.soldCars} hint={`${summary.reservedCars} đã đặt`} />
        <KpiCard label="Khách hàng" value={summary.totalCustomers} />
        <KpiCard label="Nhân viên" value={summary.totalEmployees} />
        <KpiCard label="Đơn hàng" value={summary.totalOrders} hint={`${summary.scheduledTestDrives} lịch lái thử`} />
        <KpiCard label="Doanh thu" value={formatVnd(summary.totalRevenue)} accent />
        <KpiCard label="Sẵn sàng" value={summary.availableCars} />
        <KpiCard label="Đang bảo trì" value={summary.maintenanceCars} />
        <KpiCard label="Đã đặt trước" value={summary.reservedCars} />
        <KpiCard label="Lịch lái thử" value={summary.scheduledTestDrives} />
        <KpiCard label="Xe mới" value={summary.newItems} />
        <KpiCard label="Xe cũ" value={summary.usedItems} />
      </div>

      {/* Orders by status — nhấp cột để mở popup drilldown */}
      <div className="bg-white rounded-2xl border border-[#e5e7eb] p-6 shadow-sm">
        <h2 className="font-serif font-bold text-lg text-[#0a0a0a] mb-4">
          Đơn hàng theo trạng thái
          <span className="text-xs font-sans font-normal text-[#9ca3af] ml-2">(nhấp cột để xem danh sách)</span>
        </h2>
        <BarChart
          data={statusCounts.map((s) => ({
            label: s.status,
            value: s.total,
            color: statusColors[s.status] ?? '#c58e00',
          }))}
          onSelect={openStatusDrill}
        />
      </div>

      {/* Revenue by month */}
      <div className="bg-white rounded-2xl border border-[#e5e7eb] p-6 shadow-sm">
        <h2 className="font-serif font-bold text-lg text-[#0a0a0a] mb-4">Doanh thu theo tháng</h2>
        <BarChart
          data={revenueByMonth.map((r) => ({ label: r.period, value: Math.round(r.totalRevenue) }))}
          formatValue={(v) => `${v} tr`}
        />
      </div>

      {/* Hóa đơn gần đây — nhấp để chuyển sang popup chi tiết đơn hàng */}
      <div className="bg-white rounded-2xl border border-[#e5e7eb] p-6 shadow-sm">
        <div className="flex items-baseline justify-between mb-4 gap-3 flex-wrap">
          <h2 className="font-serif font-bold text-lg text-[#0a0a0a]">Hóa đơn gần đây</h2>
          <span className="text-xs text-[#9ca3af]">(nhấp một dòng để xem chi tiết đơn hàng)</span>
        </div>
        <DataTable
          columns={invoiceColumns}
          rows={recentInvoices}
          rowKey={(r) => r.id}
          empty="Chưa có hóa đơn nào."
          onRowClick={(inv) => setDetailOrderId(inv.orderId)}
        />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        {/* Inventory by brand — nhấp hãng để mở popup drilldown */}
        <div className="bg-white rounded-2xl border border-[#e5e7eb] p-6 shadow-sm">
          <h2 className="font-serif font-bold text-lg text-[#0a0a0a] mb-4">Tồn kho theo hãng</h2>
          <div className="space-y-2">
            {inventory.map((b) => (
              <button
                key={b.brandId}
                onClick={() => openBrandDrill(b)}
                className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-[#e5e7eb] transition-colors hover:bg-[#fafafa]"
              >
                <span className="text-sm font-medium text-[#0a0a0a]">{b.brandName}</span>
                <span className="text-xs text-[#6b7280]">
                  {b.totalCars} dòng • {b.availableItems}/{b.totalQuantity} sẵn sàng
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* Top selling cars — nhấp xe để mở popup drilldown */}
        <div className="bg-white rounded-2xl border border-[#e5e7eb] p-6 shadow-sm">
          <h2 className="font-serif font-bold text-lg text-[#0a0a0a] mb-4">Xe bán chạy</h2>
          <div className="space-y-2">
            {topCars.length === 0 && <p className="text-sm text-[#9ca3af]">Chưa có dữ liệu bán hàng.</p>}
            {topCars.map((c) => (
              <button
                key={c.carId}
                onClick={() => openCarDrill(c)}
                className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-[#e5e7eb] transition-colors hover:bg-[#fafafa]"
              >
                <span className="text-sm font-medium text-[#0a0a0a]">{c.carName}</span>
                <span className="text-xs text-[#6b7280]">
                  {c.totalSold} bán • {formatVnd(c.revenue)}
                </span>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* ===== Drilldown hiển thị dạng popup ===== */}
      <Modal
        // Chỉ mở 1 popup overlay 1 lần: khi popup chi tiết đơn đang mở thì tạm ẩn popup drilldown
        // (tránh 2 modal cùng xử lý phím Escape làm đóng cả hai).
        open={drill !== null && detailOrderId === null}
        onClose={closeDrill}
        size="xl"
        title={
          drill?.type === 'status'
            ? `Đơn hàng theo trạng thái ${drill.status}`
            : drill?.type === 'brand'
              ? `Tồn kho — ${drill.brand.brandName}`
              : drill?.type === 'car'
                ? `Đơn hàng của ${drill.car.carName}`
                : 'Chi tiết'
        }
      >
        {/* Đơn theo trạng thái */}
        {drill?.type === 'status' &&
          (drillLoading ? (
            <div className="py-10 text-center text-[#9ca3af] text-sm">Đang tải…</div>
          ) : (
            <>
              <div className="mb-3">
                <StatusPill status={drill.status} />
              </div>
              <DataTable
                columns={orderColumns}
                rows={orders}
                rowKey={(r) => r.orderId}
                empty="Không có đơn hàng nào."
                onRowClick={(r) => setDetailOrderId(r.orderId)}
              />
              <p className="mt-3 text-xs text-[#9ca3af]">
                Nhấp một dòng để xem chi tiết đơn (hợp đồng, thanh toán, hóa đơn).
              </p>
            </>
          ))}

        {/* Tồn kho theo hãng → dòng xe → danh sách xe */}
        {drill?.type === 'brand' && (
          <div className="space-y-4">
            {drillLoading ? (
              <div className="py-10 text-center text-[#9ca3af] text-sm">Đang tải…</div>
            ) : models.length === 0 ? (
              <p className="text-sm text-[#9ca3af]">Hãng này chưa có dòng xe nào.</p>
            ) : (
              <div className="space-y-2">
                {models.map((m) => (
                  <button
                    key={m.modelId}
                    onClick={() => drillModel(m)}
                    className={`w-full flex items-center justify-between px-4 py-2.5 rounded-xl border text-left transition-colors ${
                      selectedModel?.modelId === m.modelId
                        ? 'border-[#c58e00] bg-[#fffbeb]'
                        : 'border-[#e5e7eb] hover:bg-[#fafafa]'
                    }`}
                  >
                    <span className="text-sm text-[#374151]">
                      {m.modelName}{' '}
                      {m.bodyType && <span className="text-[#9ca3af]">• {m.bodyType}</span>}
                    </span>
                    <span className="text-xs text-[#6b7280]">
                      {m.availableItems}/{m.totalQuantity} sẵn sàng
                    </span>
                  </button>
                ))}
              </div>
            )}

            {selectedModel && (
              <div>
                <h3 className="text-sm font-semibold text-[#0a0a0a] mb-3">
                  Danh sách xe — {selectedModel.modelName}
                </h3>
                <DataTable
                  columns={carInventoryColumns}
                  rows={cars}
                  rowKey={(r) => r.carId}
                  empty="Không có xe nào."
                />
              </div>
            )}
          </div>
        )}

        {/* Đơn hàng của 1 xe bán chạy */}
        {drill?.type === 'car' &&
          (drillLoading ? (
            <div className="py-10 text-center text-[#9ca3af] text-sm">Đang tải…</div>
          ) : (
            <>
              <DataTable
                columns={carLineColumns}
                rows={carOrders}
                rowKey={(r) => r.orderId}
                empty="Không có đơn hàng nào."
                onRowClick={(r) => setDetailOrderId(r.orderId)}
              />
              <p className="mt-3 text-xs text-[#9ca3af]">
                Nhấp một dòng để xem chi tiết đơn (hợp đồng, thanh toán, hóa đơn).
              </p>
            </>
          ))}
      </Modal>

      {/* Popup chi tiết đơn hàng — đặt sau drilldown để nổi lên trên */}
      <OrderDetailModal orderId={detailOrderId} onClose={() => setDetailOrderId(null)} />
    </div>
  );
}
