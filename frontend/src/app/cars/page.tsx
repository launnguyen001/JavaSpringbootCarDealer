import {
  listCars,
  listBrands,
  listCarModels,
  searchCarsByName,
  searchCarsByPrice,
  searchCarsByStock,
  searchCarsAdvanced,
  searchCarsAdvancedByBrand,
} from '@/lib/api';
import CarGrid from '@/components/ui/CarGrid';
import EmptyState from '@/components/ui/EmptyState';
import Pagination from '@/components/ui/Pagination';
import SearchBar from '@/components/ui/SearchBar';
import FilterPanel from '@/components/ui/FilterPanel';
import { Brand } from '@/types/brand';
import { CarModel } from '@/types/carModel';
import { Car } from '@/types/car';
import Link from 'next/link';

export const dynamic = 'force-dynamic';

// Số xe/trang: vừa 2 hàng lưới (4 cột trên màn rộng) để danh sách lọt trọn 1 view
const PAGE_SIZE = 8;

interface CarsPageProps {
  searchParams: Promise<{
    q?: string;
    brand?: string;
    model?: string;
    minPrice?: string;
    maxPrice?: string;
    minQuantity?: string;
    page?: string;
  }>;
}

export default async function CarsPage({ searchParams }: CarsPageProps) {
  const params = await searchParams;
  const q = params.q ?? undefined;
  const brand = params.brand ?? undefined;
  const model = params.model ?? undefined;
  const minPrice = params.minPrice ?? undefined;
  const maxPrice = params.maxPrice ?? undefined;
  const minQuantity = params.minQuantity ?? undefined;
  const page = Math.max(1, parseInt(params.page ?? '1', 10));
  const size = PAGE_SIZE;

  let brands: Brand[] = [];
  let models: CarModel[] = [];
  let cars: Car[] = [];

  try {
    [brands, models] = await Promise.all([listBrands(), listCarModels()]);
  } catch {
    brands = [];
    models = [];
  }

  // Resolve tên hãng/dòng từ id nếu có
  let brandName: string | undefined;
  let modelName: string | undefined;

  if (brand) {
    const b = brands.find((x) => String(x.id) === brand);
    brandName = b?.name;
  }
  if (model) {
    const m = models.find((x) => String(x.id) === model);
    modelName = m?.name;
  }

  // Chọn endpoint theo bảng 4.2 trong task3.md
  try {
    if (q && !brand && !model && !minPrice && !maxPrice && !minQuantity) {
      cars = await searchCarsByName(q);
    } else if (model) {
      cars = await searchCarsAdvanced({
        modelName: modelName ?? '',
        minPrice: minPrice ? parseFloat(minPrice) : 0,
        maxPrice: maxPrice ? parseFloat(maxPrice) : 1_000_000_000,
        minQuantity: minQuantity ? parseInt(minQuantity, 10) : 0,
      });
    } else if (brand) {
      cars = await searchCarsAdvancedByBrand({
        brandName: brandName ?? '',
        minPrice: minPrice ? parseFloat(minPrice) : 0,
        maxPrice: maxPrice ? parseFloat(maxPrice) : 1_000_000_000,
        minQuantity: minQuantity ? parseInt(minQuantity, 10) : 0,
      });
    } else if (minPrice || maxPrice) {
      cars = await searchCarsByPrice(
        minPrice ? parseFloat(minPrice) : 0,
        maxPrice ? parseFloat(maxPrice) : 1_000_000_000,
      );
    } else if (minQuantity) {
      cars = await searchCarsByStock(parseInt(minQuantity, 10));
    } else {
      cars = await listCars();
    }
  } catch {
    cars = [];
  }

  // Phân trang phía client
  const total = cars.length;
  const start = (page - 1) * size;
  const pagedCars = cars.slice(start, start + size);

  return (
    // Cột dọc vừa 1 view: breadcrumb → tiêu đề + tìm kiếm → bộ lọc ngang → lưới (flex-1) → phân trang
    <div className="min-h-[calc(100vh-4rem)] flex flex-col max-w-[1400px] mx-auto px-6 lg:px-10 py-6">
      <nav className="mb-4 flex items-center gap-2 text-[#6b7280] text-sm">
        <Link href="/" className="hover:text-[#c58e00] transition-colors">Trang chủ</Link>
        <span className="mx-1">/</span>
        <span className="font-medium text-[#0a0a0a]">Danh sách xe</span>
      </nav>

      <div className="mb-4 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-serif font-bold text-[#0a0a0a] mb-1">Danh sách xe</h1>
          <p className="text-[#6b7280] text-xs tracking-wide uppercase font-medium">
            {total} kết quả tìm thấy
          </p>
        </div>
        <div className="w-full sm:w-80">
          <SearchBar placeholder="Tìm xe theo tên…" />
        </div>
      </div>

      {/* Bộ lọc thanh ngang, full width */}
      <FilterPanel
        brands={brands}
        models={models}
        selectedBrand={brand}
        selectedModel={model}
        minPrice={minPrice}
        maxPrice={maxPrice}
        minQuantity={minQuantity}
      />

      <div className="flex-1 mt-5">
        {cars.length === 0 ? (
          <EmptyState
            title="Không có xe phù hợp"
            subtitle="Hãy thử thay đổi bộ lọc hoặc từ khóa tìm kiếm."
          />
        ) : (
          <div className="flex flex-col h-full">
            <CarGrid cars={pagedCars} compact />
            <div className="mt-5">
              <Pagination totalItems={total} pageSize={size} />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
