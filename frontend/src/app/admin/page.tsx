'use client';

import { useCallback, useEffect, useState } from 'react';
import KpiCard from '@/components/ui/KpiCard';
import BarChart from '@/components/ui/BarChart';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';
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
} from '@/lib/api';
import type {
  CarInventoryItem,
  CarSalesOrderLine,
  DashboardSummary,
  InventoryByBrand,
  InventoryByModel,
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

const carInventoryColumns: Column<CarInventoryItem>[] = [
  { key: 'carName', label: 'Xe' },
  { key: 'vin', label: 'VIN', render: (r) => r.vin ?? '—' },
  { key: 'color', label: 'Màu', render: (r) => r.color ?? '—' },
  { key: 'year', label: 'Năm', render: (r) => r.year ?? '—' },
  { key: 'quantity', label: 'Tồn', className: 'text-center', render: (r) => r.quantity ?? 0 },
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

  // drilldown states
  const [selectedStatus, setSelectedStatus] = useState<string | null>(null);
  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [selectedBrand, setSelectedBrand] = useState<InventoryByBrand | null>(null);
  const [models, setModels] = useState<InventoryByModel[]>([]);
  const [selectedModel, setSelectedModel] = useState<InventoryByModel | null>(null);
  const [cars, setCars] = useState<CarInventoryItem[]>([]);
  const [selectedCar, setSelectedCar] = useState<TopSellingCar | null>(null);
  const [carOrders, setCarOrders] = useState<CarSalesOrderLine[]>([]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [s, inv, sc, tc, rbm] = await Promise.all([
        getDashboardSummary(),
        getInventoryByBrand(),
        getOrdersByStatusCount(),
        getTopSellingCars(5),
        getRevenueByMonth(),
      ]);
      setSummary(s);
      setInventory(inv);
      setStatusCounts(sc);
      setTopCars(tc);
      setRevenueByMonth(rbm);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const drillStatus = async (status: string) => {
    setSelectedStatus(status);
    setOrders([]);
    try {
      setOrders(await getOrdersByStatus(status));
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  const drillBrand = async (brand: InventoryByBrand) => {
    setSelectedBrand(brand);
    setSelectedModel(null);
    setCars([]);
    setModels([]);
    try {
      setModels(await getModelsByBrand(brand.brandId));
    } catch (err) {
      toast.error(getErrorMessage(err));
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

  const drillCar = async (car: TopSellingCar) => {
    setSelectedCar(car);
    setCarOrders([]);
    try {
      setCarOrders(await getOrdersByCar(car.carId));
    } catch (err) {
      toast.error(getErrorMessage(err));
    }
  };

  if (loading || !summary) {
    return <div className="py-20 text-center text-[#9ca3af] text-sm">Đang tải dashboard…</div>;
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-serif font-bold text-[#0a0a0a]">Dashboard</h1>
        <p className="text-sm text-[#6b7280] mt-1">
          Tổng quan hoạt động kinh doanh — nhấp vào biểu đồ để xem chi tiết (drilldown).
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
      </div>

      {/* Orders by status + drilldown */}
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
          onSelect={drillStatus}
        />
        {selectedStatus && (
          <div className="mt-6">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-semibold text-[#0a0a0a]">
                Đơn trạng thái <StatusPill status={selectedStatus} className="ml-1" />
              </h3>
              <button
                onClick={() => setSelectedStatus(null)}
                className="text-xs text-[#6b7280] hover:text-[#c58e00]"
              >
                Đóng
              </button>
            </div>
            <DataTable columns={orderColumns} rows={orders} rowKey={(r) => r.orderId} />
          </div>
        )}
      </div>

      {/* Revenue by month */}
      <div className="bg-white rounded-2xl border border-[#e5e7eb] p-6 shadow-sm">
        <h2 className="font-serif font-bold text-lg text-[#0a0a0a] mb-4">Doanh thu theo tháng</h2>
        <BarChart
          data={revenueByMonth.map((r) => ({ label: r.period, value: Math.round(r.totalRevenue) }))}
          formatValue={(v) => `${v} tr`}
        />
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
        {/* Inventory by brand drilldown */}
        <div className="bg-white rounded-2xl border border-[#e5e7eb] p-6 shadow-sm">
          <h2 className="font-serif font-bold text-lg text-[#0a0a0a] mb-4">Tồn kho theo hãng</h2>
          <div className="space-y-2">
            {inventory.map((b) => (
              <button
                key={b.brandId}
                onClick={() => drillBrand(b)}
                className={`w-full flex items-center justify-between px-4 py-3 rounded-xl border transition-colors ${
                  selectedBrand?.brandId === b.brandId
                    ? 'border-[#c58e00] bg-[#fffbeb]'
                    : 'border-[#e5e7eb] hover:bg-[#fafafa]'
                }`}
              >
                <span className="text-sm font-medium text-[#0a0a0a]">{b.brandName}</span>
                <span className="text-xs text-[#6b7280]">
                  {b.totalCars} xe • tồn {b.totalQuantity}
                </span>
              </button>
            ))}
          </div>

          {selectedBrand && (
            <div className="mt-5 pl-4 border-l-2 border-[#c58e00]">
              <h3 className="text-sm font-semibold text-[#0a0a0a] mb-3">
                Dòng xe của {selectedBrand.brandName}
              </h3>
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
                      {m.modelName} {m.bodyType && <span className="text-[#9ca3af]">• {m.bodyType}</span>}
                    </span>
                    <span className="text-xs text-[#6b7280]">{m.totalCars} xe</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {selectedModel && (
            <div className="mt-5">
              <h3 className="text-sm font-semibold text-[#0a0a0a] mb-3">
                Danh sách xe — {selectedModel.modelName}
              </h3>
              <DataTable
                columns={carInventoryColumns}
                rows={cars}
                rowKey={(r) => r.carId}
              />
            </div>
          )}
        </div>

        {/* Top selling cars drilldown */}
        <div className="bg-white rounded-2xl border border-[#e5e7eb] p-6 shadow-sm">
          <h2 className="font-serif font-bold text-lg text-[#0a0a0a] mb-4">Xe bán chạy</h2>
          <div className="space-y-2">
            {topCars.length === 0 && <p className="text-sm text-[#9ca3af]">Chưa có dữ liệu bán hàng.</p>}
            {topCars.map((c) => (
              <button
                key={c.carId}
                onClick={() => drillCar(c)}
                className={`w-full flex items-center justify-between px-4 py-3 rounded-xl border transition-colors ${
                  selectedCar?.carId === c.carId
                    ? 'border-[#c58e00] bg-[#fffbeb]'
                    : 'border-[#e5e7eb] hover:bg-[#fafafa]'
                }`}
              >
                <span className="text-sm font-medium text-[#0a0a0a]">{c.carName}</span>
                <span className="text-xs text-[#6b7280]">
                  {c.totalSold} bán • {formatVnd(c.revenue)}
                </span>
              </button>
            ))}
          </div>

          {selectedCar && (
            <div className="mt-5">
              <h3 className="text-sm font-semibold text-[#0a0a0a] mb-3">
                Đơn hàng chứa {selectedCar.carName}
              </h3>
              <DataTable columns={carLineColumns} rows={carOrders} rowKey={(r) => r.orderId} />
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
