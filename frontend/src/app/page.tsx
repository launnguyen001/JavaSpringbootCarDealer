import { listCars, searchCarsByName } from '@/lib/api';
import CarCard from '@/components/ui/CarCard';
import Link from 'next/link';
import type { Car } from '@/types/car';

export const dynamic = 'force-dynamic';

interface HomeProps {
  searchParams: Promise<{ q?: string }>;
}

export default async function Home({ searchParams }: HomeProps) {
  const { q } = await searchParams;

  let cars: Car[] = [];
  try {
    cars = q ? await searchCarsByName(q) : await listCars();
  } catch {
    cars = [];
  }

  const featured = cars.slice(0, 6);

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* Hero section */}
      <section className="relative h-[70vh] lg:h-[85vh] flex items-center justify-center overflow-hidden">
        <div className="absolute inset-0 z-0">
          <img
            src="https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=1920&q=80"
            alt="Hero"
            className="w-full h-full object-cover object-center opacity-60"
          />
          <div className="absolute inset-0 bg-gradient-to-b from-[#0a0a0a]/20 to-[#0a0a0a]/80" />
        </div>

        <div className="relative z-10 px-6 lg:px-12 max-w-7xl mx-auto text-center">
          <span className="inline-block px-4 py-1.5 rounded-full border border-[#c58e00]/30 bg-[#c58e00]/10 text-[#c58e00] text-[10px] font-serif font-semibold tracking-[0.3em] uppercase mb-6 animate-fade-in">
            Mùa hè 2026 — Bộ sưu tập mới
          </span>
          <h1 className="text-5xl lg:text-7xl font-serif font-bold text-white leading-tight mb-6 animate-fade-in" style={{ animationDelay: '0.1s' }}>
            Khám phá sự xa xỉ<br />
            trên bốn bánh xe
          </h1>
          <p className="text-lg lg:text-xl text-[#e5e7eb] max-w-2xl mx-auto mb-10 font-light tracking-wide animate-fade-in" style={{ animationDelay: '0.2s' }}>
            Từ siêu xe hạng sang đến dòng xe cao cấp, mỗi mẫu xe đều là tác phẩm nghệ thuật vận hành.
          </p>
          <div className="flex flex-col lg:flex-row items-center justify-center gap-4 animate-fade-in" style={{ animationDelay: '0.3s' }}>
            <Link
              href="/cars"
              className="px-8 py-4 bg-[#0a0a0a] text-white font-serif font-semibold text-lg tracking-widest uppercase hover:bg-[#c58e00] transition-all duration-300 shadow-lg shadow-[#0a0a0a]/30"
            >
              Xem toàn bộ
            </Link>
            <a
              href="#contact"
              className="px-8 py-4 border border-white/20 text-white font-serif font-semibold text-lg tracking-widest uppercase hover:bg-white/10 transition-all duration-300 backdrop-blur"
            >
              Liên hệ
            </a>
          </div>
        </div>
      </section>

      {/* Featured cars */}
      <section className="py-20 px-6 lg:px-12 max-w-7xl mx-auto">
        <div className="flex items-center justify-between mb-12">
          <div>
            <h2 className="text-3xl font-serif font-bold text-[#0a0a0a] mb-2">Danh sách nổi bật</h2>
            <p className="text-[#6b7280] text-sm tracking-wide uppercase font-medium">
              Các mẫu xe sẵn sàng giao hàng ngay
            </p>
          </div>
          <Link
            href="/cars"
            className="text-sm font-semibold text-[#c58e00] hover:text-[#8a6d00] flex items-center gap-1 transition-colors"
          >
            Xem tất cả <span aria-hidden="true">→</span>
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {featured.map((car: Car) => (
            <CarCard key={car.id} car={car} />
          ))}
        </div>
      </section>

      {/* Brands strip */}
      <section className="py-16 bg-[#fafafa] border-y border-[#e5e7eb]">
        <div className="px-6 lg:px-12 max-w-7xl mx-auto">
          <h3 className="text-2xl font-serif font-bold text-[#0a0a0a] text-center mb-12">Các hãng chúng tôi đại lý</h3>
          <div className="flex flex-wrap justify-center gap-8 md:gap-16 opacity-60 hover:opacity-100 transition-opacity duration-500">
            {['Rolls-Royce', 'Bugatti', 'Lamborghini', 'Ferrari', 'Mercedes-Benz', 'Bentley'].map((name) => (
              <div key={name} className="text-center">
                <div className="w-16 h-16 rounded-full border border-[#e5e7eb] bg-white flex items-center justify-center mx-auto mb-3 shadow-sm">
                  <span className="text-xs font-serif font-bold text-[#0a0a0a]">{name.charAt(0)}</span>
                </div>
                <p className="text-sm font-medium text-[#6b7280]">{name}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 px-6 lg:px-12">
        <div className="max-w-4xl mx-auto bg-[#0a0a0a] rounded-3xl p-10 lg:p-16 text-center overflow-hidden relative">
          <div className="absolute top-0 left-0 w-full h-1 bg-[#c58e00]" />
          <h2 className="text-3xl lg:text-4xl font-serif font-bold text-white mb-6">Đăng ký lái thử xe mơ ước của bạn</h2>
          <p className="text-[#9ca3af] text-lg mb-10 max-w-xl mx-auto font-light tracking-wide">
            Để lại thông tin, đội ngũ chuyên gia của chúng tôi sẽ liên hệ sớm nhất.
          </p>
          <form className="mx-auto w-full max-w-2xl">
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 p-2 rounded-xl bg-white/5 backdrop-blur">
              <input
                type="text"
                name="name"
                placeholder="Họ tên"
                autoComplete="name"
                required
                className="flex-1 min-w-0 h-11 px-4 rounded-xl bg-white text-[#0a0a0a] text-sm placeholder:text-[#9ca3af] focus:outline-none focus:ring-2 focus:ring-[#c58e00]"
              />
              <input
                type="email"
                name="email"
                placeholder="Email"
                autoComplete="email"
                required
                className="flex-1 min-w-0 h-11 px-4 rounded-xl bg-white text-[#0a0a0a] text-sm placeholder:text-[#9ca3af] focus:outline-none focus:ring-2 focus:ring-[#c58e00]"
              />
              <button
                type="submit"
                className="h-11 px-6 rounded-xl bg-[#c58e00] text-white text-sm font-semibold tracking-wide whitespace-nowrap transition-colors hover:bg-[#a87800] focus:outline-none focus:ring-2 focus:ring-[#c58e00]/60 focus:ring-offset-2 focus:ring-offset-[#0a0a0a]"
              >
                Gửi đăng ký
              </button>
            </div>
          </form>
        </div>
      </section>
    </div>
  );
}

async function searchCars(q: string): Promise<Car[]> {
  const res = await fetch(`/api/cars/search/name?name=${encodeURIComponent(q)}`, { cache: 'no-store' });
  if (!res.ok) return [];
  return res.json();
}
