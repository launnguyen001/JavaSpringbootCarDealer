
'use client';

import { useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';

import KpiCard from '@/components/ui/KpiCard';
import BarChart from '@/components/ui/BarChart';
import RevenueTubeChart from '@/components/admin/RevenueTubeChart';
import DataTable, { type Column } from '@/components/ui/DataTable';
import StatusPill from '@/components/ui/StatusPill';

import DashboardDrawer from '@/components/admin/DashboardDrawer';
import DashboardOrdersDrawer from '@/components/admin/DashboardOrdersDrawer';
import DashboardRevenueOrdersDrawer from '@/components/admin/DashboardRevenueOrdersDrawer';
import OrderDetailModal from '@/components/admin/OrderDetailModal';

import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import { formatVnd, formatDateTime } from '@/lib/utils/format';

import {
    getDashboardData,
    listBrands,
    getCarModelsByBrand,
} from '@/lib/api';

import type {
    Brand,
    CarModel,
    DashboardData,
    OrderSummary,
    RevenueCategory,
} from '@/types';

// Chuyển Date sang định dạng YYYY-MM-DD theo múi giờ local
const asDate = (d: Date) => {
    const local = new Date(
        d.getTime() - d.getTimezoneOffset() * 60000
    );

    return local.toISOString().slice(0, 10);
};

// Ngày kết thúc mặc định: hôm nay
const endDate = asDate(new Date());

// Ngày bắt đầu mặc định: 30 ngày gần nhất
const start = new Date();
start.setDate(start.getDate() - 29);

const startDate = asDate(start);

// Các cột của bảng đơn hàng
const orderColumns: Column<OrderSummary>[] = [
    {
        key: 'orderCode',
        label: 'Mã đơn',
    },
    {
        key: 'customerName',
        label: 'Khách hàng',
        render: (r) => r.customerName ?? '—',
    },
    {
        key: 'orderDate',
        label: 'Ngày đặt',
        render: (r) => formatDateTime(r.orderDate),
    },
    {
        key: 'totalAmount',
        label: 'Tổng tiền',
        className: 'text-right',
        render: (r) => formatVnd(r.totalAmount),
    },
    {
        key: 'status',
        label: 'Trạng thái',
        render: (r) => <StatusPill status={r.status} />,
    },
];

export default function AdminDashboardPage() {
    const toast = useToast();
    const router = useRouter();

    // =========================
    // 1. State quản lý bộ lọc
    // =========================

    const [from, setFrom] = useState(startDate);
    const [to, setTo] = useState(endDate);

    const [brandId, setBrandId] = useState('');
    const [modelId, setModelId] = useState('');

    // =========================
    // 2. State lưu dữ liệu
    // =========================

    const [brands, setBrands] = useState<Brand[]>([]);
    const [models, setModels] = useState<CarModel[]>([]);

    const [data, setData] = useState<DashboardData | null>(null);
    const [loading, setLoading] = useState(true);

    // =========================
    // 3. State quản lý Drawer / Modal
    // =========================

    const [drawer, setDrawer] = useState<{
        status: string | null;
        title: string;
        count: number;
    } | null>(null);

    const [orderDrawer, setOrderDrawer] = useState<{
        status: string;
        title: string;
        count: number;
    } | null>(null);

    const [revenueDrawer, setRevenueDrawer] = useState<{
        period: string;
        category: RevenueCategory;
        title: string;
        amount: number;
    } | null>(null);

    const [detailOrderId, setDetailOrderId] = useState<number | null>(null);

    // =========================
    // 4. Tạo bộ lọc
    // =========================

    const filters = useMemo(
        () => ({
            from,
            to,
            ...(brandId ? { brandId: Number(brandId) } : {}),
            ...(modelId ? { modelId: Number(modelId) } : {}),
        }),
        [from, to, brandId, modelId]
    );

    // Bộ lọc riêng cho doanh thu
    const revenueFilters = useMemo(
        () => ({
            brandId: filters.brandId,
            modelId: filters.modelId,
        }),
        [filters.brandId, filters.modelId]
    );

    // =========================
    // 5. Lấy danh sách hãng xe
    // =========================

    useEffect(() => {
        let active = true;

        listBrands()
            .then((rows) => {
                if (active) {
                    setBrands(rows);
                }
            })
            .catch((e) => {
                if (active) {
                    toast.error(getErrorMessage(e));
                }
            });

        return () => {
            active = false;
        };
    }, [toast]);

    // =========================
    // 6. Lấy model theo hãng xe
    // =========================

    useEffect(() => {
        if (!brandId) return;

        let active = true;

        getCarModelsByBrand(Number(brandId))
            .then((rows) => {
                if (active) {
                    setModels(rows);
                }
            })
            .catch((e) => {
                if (active) {
                    toast.error(getErrorMessage(e));
                }
            });

        return () => {
            active = false;
        };
    }, [brandId, toast]);

    // =========================
    // 7. Lấy dữ liệu Dashboard
    // =========================

    useEffect(() => {
        if (!from || !to || from > to) {
            toast.error('Vui lòng chọn khoảng ngày hợp lệ.');
            return;
        }

        let active = true;

        const fetchDashboard = async () => {
            try {
                const result = await getDashboardData(filters);

                if (active) {
                    setData(result);
                }
            } catch (e) {
                if (active) {
                    toast.error(getErrorMessage(e));
                }
            } finally {
                if (active) {
                    setLoading(false);
                }
            }
        };

        void fetchDashboard();

        return () => {
            active = false;
        };
    }, [filters, from, to, toast]);

    // =========================
    // 8. Tính tỷ lệ tăng trưởng
    // =========================

    const growth = (now: number, prev: number) => {
        if (prev === 0) {
            return now > 0 ? 'Mới phát sinh' : 'Không đổi';
        }

        return (
            (now - prev > 0 ? '+' : '') +
            (
                ((now - prev) / Math.abs(prev)) *
                100
            ).toFixed(1) +
            '% so với kỳ trước'
        );
    };

    // =========================
    // 9. Mở Drawer tồn kho
    // =========================

    const open = (
        status: string | null,
        title: string,
        count: number
    ) => {
        setDrawer({
            status,
            title,
            count,
        });
    };

    return (
        <div className="space-y-7">
            {/* =========================
                SECTION 1: DASHBOARD
            ========================= */}

            <section className="flex min-h-[calc(100dvh-8rem)] flex-col gap-4">

                {/* Tiêu đề và bộ lọc */}
                <div className="flex flex-wrap items-end justify-between gap-3">
                    <div>
                        <h1 className="text-2xl font-serif font-bold text-[#111827]">
                            Dashboard
                        </h1>

                        <p className="mt-1 text-sm text-[#6b7280]">
                            Tổng quan tồn kho và hoạt động kinh doanh
                        </p>
                    </div>

                    <div className="flex flex-wrap items-end gap-2">

                        {/* Lọc từ ngày */}
                        <label className="text-xs text-[#6b7280]">
                            Từ ngày

                            <input
                                type="date"
                                value={from}
                                max={to}
                                onChange={(e) => {
                                    setLoading(true);
                                    setFrom(e.target.value);
                                }}
                                className="mt-1 block rounded-lg border border-[#e5e7eb] bg-white px-3 py-2 text-sm"
                            />
                        </label>

                        {/* Lọc đến ngày */}
                        <label className="text-xs text-[#6b7280]">
                            Đến ngày

                            <input
                                type="date"
                                value={to}
                                min={from}
                                onChange={(e) => {
                                    setLoading(true);
                                    setTo(e.target.value);
                                }}
                                className="mt-1 block rounded-lg border border-[#e5e7eb] bg-white px-3 py-2 text-sm"
                            />
                        </label>

                        {/* Lọc hãng xe */}
                        <label className="text-xs text-[#6b7280]">
                            Hãng xe

                            <select
                                value={brandId}
                                onChange={(e) => {
                                    setLoading(true);
                                    setBrandId(e.target.value);
                                    setModelId('');
                                    setModels([]);
                                }}
                                className="mt-1 block min-w-32 rounded-lg border border-[#e5e7eb] bg-white px-3 py-2 text-sm"
                            >
                                <option value="">Tất cả hãng</option>

                                {brands.map((b) => (
                                    <option key={b.id} value={b.id}>
                                        {b.name}
                                    </option>
                                ))}
                            </select>
                        </label>

                        {/* Lọc model */}
                        <label className="text-xs text-[#6b7280]">
                            Model

                            <select
                                value={modelId}
                                disabled={!brandId}
                                onChange={(e) => setModelId(e.target.value)}
                                className="mt-1 block min-w-36 rounded-lg border border-[#e5e7eb] bg-white px-3 py-2 text-sm disabled:bg-[#f3f4f6]"
                            >
                                <option value="">Tất cả model</option>

                                {models.map((m) => (
                                    <option key={m.id} value={m.id}>
                                        {m.name}
                                    </option>
                                ))}
                            </select>
                        </label>
                    </div>
                </div>

                {/* Nội dung Dashboard */}
                {data && (
                    <>
                        <div className="flex items-center justify-between">
                            <h2 className="text-sm font-semibold text-[#374151]">
                                Tồn kho hiện tại
                            </h2>

                            {loading && (
                                <span className="text-xs text-[#9ca3af]">
                                    Đang cập nhật…
                                </span>
                            )}
                        </div>

                        {/* KPI: Tồn kho */}
                        <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5">

                            <button
                                onClick={() =>
                                    open(
                                        'IN_STOCK',
                                        'Xe đang thuộc kho',
                                        data.totalCars
                                    )
                                }
                                className="block h-full text-left"
                            >
                                <KpiCard
                                    label="Xe trong kho"
                                    value={data.totalCars.toLocaleString('vi-VN')}
                                    hint="Sẵn sàng + bảo trì"
                                />
                            </button>

                            <button
                                onClick={() =>
                                    open(
                                        'AVAILABLE',
                                        'Xe sẵn sàng bán',
                                        data.availableCars
                                    )
                                }
                                className="block h-full text-left"
                            >
                                <KpiCard
                                    label="Sẵn sàng bán"
                                    value={data.availableCars.toLocaleString('vi-VN')}
                                    hint="AVAILABLE"
                                />
                            </button>

                            <button
                                onClick={() =>
                                    open(
                                        'MAINTENANCE',
                                        'Xe đang bảo trì',
                                        data.maintenanceCars
                                    )
                                }
                                className="block h-full text-left"
                            >
                                <KpiCard
                                    label="Đang bảo trì"
                                    value={data.maintenanceCars.toLocaleString('vi-VN')}
                                    hint="MAINTENANCE"
                                />
                            </button>
                        </div>

                        {/* KPI: Xe có giao dịch */}
                        <div className="space-y-2">
                            <h3 className="text-sm font-semibold text-[#6b7280]">
                                Xe đã có cam kết giao dịch hoặc đã xuất bán
                            </h3>

                            <div className="grid grid-cols-2 gap-3 xl:grid-cols-3">

                                <button
                                    onClick={() =>
                                        open(
                                            'RESERVED',
                                            'Xe đã đặt trước',
                                            data.reservedCars
                                        )
                                    }
                                    className="block h-full text-left"
                                >
                                    <KpiCard
                                        label="Đã đặt trước"
                                        value={data.reservedCars.toLocaleString('vi-VN')}
                                        hint="Đang có ràng buộc giao dịch"
                                    />
                                </button>

                                <button
                                    onClick={() =>
                                        open(
                                            'SOLD_ALL',
                                            'Xe đã bán',
                                            data.soldCars
                                        )
                                    }
                                    className="block h-full text-left"
                                >
                                    <KpiCard
                                        label="Đã bán"
                                        value={data.soldCars.toLocaleString('vi-VN')}
                                        hint="Không còn trong kho"
                                        accent
                                    />
                                </button>

                                <button
                                    onClick={() =>
                                        open(
                                            'SOLD',
                                            'Xe đã bán trong kỳ',
                                            data.soldInPeriod
                                        )
                                    }
                                    className="block h-full text-left"
                                >
                                    <KpiCard
                                        label="Đã bán trong kỳ"
                                        value={data.soldInPeriod.toLocaleString('vi-VN')}
                                        hint={growth(
                                            data.soldInPeriod,
                                            data.previousSoldInPeriod
                                        )}
                                    />
                                </button>
                            </div>
                        </div>

                        {/* Biểu đồ */}
                        <div className="grid min-h-0 flex-1 grid-cols-1 gap-4 xl:grid-cols-2">

                            {/* Biểu đồ đơn hàng theo trạng thái */}
                            <div className="min-h-[220px] rounded-2xl border border-[#e5e7eb] bg-gradient-to-br from-white via-white to-[#fbf7ec] p-4 shadow-sm">

                                <div className="mb-3 flex items-center justify-between gap-2">
                                    <div className="flex items-center gap-2">
                                        <span className="h-2.5 w-2.5 rounded-full bg-[#c58e00]" />

                                        <h2 className="font-semibold text-[#111827]">
                                            Đơn hàng theo trạng thái
                                        </h2>
                                    </div>

                                    <span className="rounded-full bg-white px-2 py-0.5 text-[10px] font-medium text-[#9ca3af] ring-1 ring-[#f0efe9]">
                                        Trong kỳ
                                    </span>
                                </div>

                                <BarChart
                                    data={data.orderStatuses.map((s) => ({
                                        label: s.status,
                                        value: s.total,
                                    }))}
                                    onSelect={(status) => {
                                        const item = data.orderStatuses.find(
                                            (s) => s.status === status
                                        );

                                        setOrderDrawer({
                                            status,
                                            title: `Đơn hàng ${status}`,
                                            count: item?.total ?? 0,
                                        });
                                    }}
                                />
                            </div>

                            {/* Biểu đồ doanh thu */}
                            <div className="min-h-[220px] rounded-2xl border border-[#e5e7eb] bg-gradient-to-br from-white via-white to-[#f5f8fd] p-4 shadow-sm">

                                <div className="mb-3 flex items-center justify-between gap-2">
                                    <div className="flex items-center gap-2">
                                        <span className="h-2.5 w-2.5 rounded-full bg-[#5277c3]" />

                                        <h2 className="font-semibold text-[#111827]">
                                            Doanh thu theo tháng
                                        </h2>
                                    </div>

                                    <span className="rounded-full bg-white px-2 py-0.5 text-[10px] font-medium text-[#6b7280] ring-1 ring-[#f0efe9]">
                                        {growth(
                                            data.revenue,
                                            data.previousRevenue
                                        )}
                                    </span>
                                </div>

                                <RevenueTubeChart
                                    data={data.revenueByMonth}
                                    onSelect={(period, category, amount) =>
                                        setRevenueDrawer({
                                            period,
                                            category,
                                            amount,
                                            title:
                                                category === 'PENDING'
                                                    ? 'Doanh thu chờ'
                                                    : category === 'RECOGNIZED'
                                                        ? 'Doanh thu đã ghi nhận'
                                                        : 'Thực nhận lũy kế',
                                        })
                                    }
                                />
                            </div>
                        </div>
                    </>
                )}

                {/* Trạng thái loading */}
                {loading && !data && (
                    <p className="py-12 text-center text-sm text-[#9ca3af]">
                        Đang tải dashboard…
                    </p>
                )}
            </section>

            {/* =========================
                SECTION 2: BẢNG DỮ LIỆU
            ========================= */}

            {data && (
                <section className="grid grid-cols-1 gap-5 xl:grid-cols-2">

                    {/* Đơn hàng gần nhất */}
                    <div className="rounded-2xl border border-[#e5e7eb] bg-white p-5 shadow-sm">

                        <div className="mb-4 flex items-center justify-between">
                            <h2 className="font-semibold text-[#111827]">
                                Đơn hàng gần nhất
                            </h2>

                            <button
                                onClick={() => router.push('/admin/orders')}
                                className="group inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium text-[#a87900] transition-all hover:bg-[#fffbeb] hover:text-[#8a6d00]"
                            >
                                Tất cả đơn hàng

                                <span className="transition-transform group-hover:translate-x-0.5">
                                    →
                                </span>
                            </button>
                        </div>

                        <DataTable
                            columns={orderColumns}
                            rows={data.recentOrders}
                            rowKey={(r) => r.orderId}
                            empty="Không có đơn hàng trong kỳ."
                            onRowClick={(r) => setDetailOrderId(r.orderId)}
                        />
                    </div>

                    {/* Xe bán chạy */}
                    <div className="rounded-2xl border border-[#e5e7eb] bg-white p-5 shadow-sm">

                        <h2 className="mb-4 font-semibold text-[#111827]">
                            Xe bán chạy
                        </h2>

                        <div className="space-y-2">

                            {data.topSellingCars.length === 0 ? (
                                <p className="py-8 text-center text-sm text-[#9ca3af]">
                                    Chưa có dữ liệu bán xe.
                                </p>
                            ) : (
                                data.topSellingCars.map((car, index) => (
                                    <div
                                        key={car.carId}
                                        className="flex items-center justify-between gap-3 rounded-xl border border-[#f0f0f0] px-4 py-3"
                                    >
                                        <div className="flex min-w-0 items-center gap-3">

                                            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-[#fffbeb] text-xs font-semibold text-[#a87900]">
                                                {index + 1}
                                            </span>

                                            <span className="truncate text-sm font-medium text-[#374151]">
                                                {car.carName}
                                            </span>
                                        </div>

                                        <div className="shrink-0 text-right">

                                            <p className="text-sm font-semibold text-[#111827]">
                                                {car.totalSold} xe
                                            </p>

                                            <p className="text-xs text-[#9ca3af]">
                                                {formatVnd(car.revenue)}
                                            </p>
                                        </div>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </section>
            )}

            {/* =========================
                SECTION 3: DRAWER / MODAL
            ========================= */}

            {/* Drawer tồn kho */}
            {drawer && (
                <DashboardDrawer
                    status={drawer.status}
                    title={drawer.title}
                    count={drawer.count}
                    brandId={filters.brandId}
                    modelId={filters.modelId}
                    from={from}
                    to={to}
                    onClose={() => setDrawer(null)}
                />
            )}

            {/* Drawer đơn hàng theo trạng thái */}
            {orderDrawer && (
                <DashboardOrdersDrawer
                    status={orderDrawer.status}
                    title={orderDrawer.title}
                    count={orderDrawer.count}
                    filters={filters}
                    onClose={() => setOrderDrawer(null)}
                    onSelectOrder={(id) => {
                        setOrderDrawer(null);
                        setDetailOrderId(id);
                    }}
                />
            )}

            {/* Drawer đơn hàng theo doanh thu */}
            {revenueDrawer && (
                <DashboardRevenueOrdersDrawer
                    period={revenueDrawer.period}
                    category={revenueDrawer.category}
                    title={revenueDrawer.title}
                    amount={revenueDrawer.amount}
                    filters={revenueFilters}
                    onClose={() => setRevenueDrawer(null)}
                    onSelectOrder={(id) => {
                        setRevenueDrawer(null);
                        setDetailOrderId(id);
                    }}
                />
            )}

            {/* Modal chi tiết đơn hàng */}
            <OrderDetailModal
                orderId={detailOrderId}
                onClose={() => setDetailOrderId(null)}
            />
        </div>
    );
}
