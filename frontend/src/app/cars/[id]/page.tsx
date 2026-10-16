import { notFound } from 'next/navigation';
import {
  getCar,
  getCarMedia,
  getCarSpecs,
  getCarReviews,
  getCarReviewSummary,
} from '@/lib/api';
import { formatPrice, formatDate, formatRating } from '@/lib/utils/format';
import PriceTag from '@/components/ui/PriceTag';
import StatusBadge from '@/components/ui/StatusBadge';
import RatingStars from '@/components/ui/RatingStars';
import CarPurchasePanel from '@/components/car/CarPurchasePanel';
import ReviewForm from '@/components/car/ReviewForm';
import Link from 'next/link';
import type { CarSpecification, CarReview } from '@/types';

export const dynamic = 'force-dynamic';

interface CarDetailPageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: CarDetailPageProps) {
  const { id } = await params;
  let name = 'Xe';
  try {
    const car = await getCar(Number(id));
    name = car.name;
  } catch {
    // ignore
  }
  return {
    title: `${name} — Car Dealer`,
    description: `Chi tiết xe ${name} tại đại lý xe hạng sang Car Dealer.`,
  };
}

export default async function CarDetailPage({ params }: CarDetailPageProps) {
  const { id } = await params;
  const carId = Number(id);

  let car;
  try {
    car = await getCar(carId);
  } catch {
    notFound();
  }
  if (!car) notFound();

  const [mediaRes, specsRes, reviewsRes, summaryRes] = await Promise.allSettled([
    getCarMedia(car.id),
    getCarSpecs(car.id),
    getCarReviews(car.id),
    getCarReviewSummary(car.id),
  ]);

  const carMedia: Awaited<ReturnType<typeof getCarMedia>> | null =
    mediaRes.status === 'fulfilled' ? mediaRes.value : null;
  const carSpecs: CarSpecification | null =
    specsRes.status === 'fulfilled' ? specsRes.value : null;
  const carReviews: CarReview[] =
    reviewsRes.status === 'fulfilled' ? reviewsRes.value : [];
  const reviewSummary: Awaited<ReturnType<typeof getCarReviewSummary>> | null =
    summaryRes.status === 'fulfilled' ? summaryRes.value : null;

  const images = carMedia?.images?.length
    ? carMedia.images
    : [carMedia?.thumbnail ?? null].filter(Boolean);
  const thumbnail = carMedia?.thumbnail;

  const isAvailable = car.status === 'AVAILABLE';

  return (
    <div className="min-h-[calc(100vh-200px)] py-12 px-6 lg:px-12 max-w-7xl mx-auto">
      <nav className="mb-8 flex items-center gap-2 text-[#6b7280] text-sm">
        <Link href="/" className="hover:text-[#c58e00] transition-colors">Trang chủ</Link>
        <span className="mx-1">/</span>
        <Link href="/cars" className="hover:text-[#c58e00] transition-colors">Danh sách xe</Link>
        <span className="mx-1">/</span>
        <span className="font-medium text-[#0a0a0a]">{car.name}</span>
      </nav>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Gallery */}
        <div className="lg:col-span-7">
          <div className="relative h-[500px] rounded-3xl border border-[#e5e7eb] bg-[#f3f4f6] overflow-hidden shadow-lg">
            <img
              src={
                thumbnail ??
                images[0] ??
                'https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=1200&q=80'
              }
              alt={car.name}
              className="w-full h-full object-cover object-center"
            />
            <div className="absolute top-4 right-4 z-10">
              <StatusBadge status={car.status} className="shadow-lg" />
            </div>
            {images.length > 1 && (
              <div className="absolute bottom-4 left-4 z-10 flex gap-2">
                {images.slice(0, 4).map((img, i) => (
                  <button
                    key={i}
                    className="w-10 h-10 rounded-xl bg-white/90 backdrop-blur border border-[#e5e7eb] hover:border-[#c58e00] transition-all"
                  >
                    <img
                      src={img!}
                      alt={`thumb ${i}`}
                      className="w-full h-full object-cover object-center rounded-xl"
                    />
                  </button>
                ))}
              </div>
            )}
          </div>

          <div className="mt-6 grid grid-cols-3 gap-2">
            {images.slice(1, 6).map((img, i) => (
              <button
                key={i}
                className="aspect-square rounded-2xl border border-[#e5e7eb] bg-[#f3f4f6] overflow-hidden hover:border-[#c58e00] transition-all"
              >
                <img
                  src={img!}
                  alt={`thumb ${i + 1}`}
                  className="w-full h-full object-cover object-center"
                />
              </button>
            ))}
          </div>
        </div>

        {/* Details */}
        <div className="lg:col-span-5 flex flex-col gap-8">
          <div>
            <div className="flex items-baseline gap-3 mb-2">
              <PriceTag price={car.price} className="text-3xl lg:text-4xl" />
              {car.quantity != null && car.quantity > 0 && (
                <span className="text-[#6b7280] text-sm font-medium">
                  {car.quantity} xe còn lại
                </span>
              )}
            </div>
            <h1 className="text-4xl lg:text-5xl font-serif font-bold text-[#0a0a0a] leading-tight mb-4">
              {car.name}
            </h1>
            {car.brandName && car.modelName && (
              <p className="text-[#6b7280] text-lg tracking-wide uppercase font-medium mb-6">
                {car.brandName} • {car.modelName}
              </p>
            )}

            <div className="space-y-4 mb-8">
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-xl bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6b7280" strokeWidth="1.5">
                    <circle cx="12" cy="12" r="3" />
                    <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
                  </svg>
                </div>
                <div>
                  <p className="text-xs text-[#6b7280] uppercase tracking-widest font-medium">Diện tích</p>
                  <p className="text-[#0a0a0a] font-medium">{carSpecs?.dimensions ?? '—'}</p>
                </div>
              </div>
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-xl bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6b7280" strokeWidth="1.5">
                    <path d="M19 11H5c-1.1 0-2-.9-2-2V5c0-1.1.9-2 2-2h14c1.1 0 2 .9 2 2v4c0 1.1-.9 2-2 2z" />
                    <line x1="12" y1="19" x2="12" y2="23" />
                    <line x1="8" y1="23" x2="16" y2="23" />
                  </svg>
                </div>
                <div>
                  <p className="text-xs text-[#6b7280] uppercase tracking-widest font-medium">Nhiên liệu</p>
                  <p className="text-[#0a0a0a] font-medium">{carSpecs?.fuelType ?? '—'}</p>
                </div>
              </div>
              <div className="flex items-start gap-4">
                <div className="w-12 h-12 rounded-xl bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6b7280" strokeWidth="1.5">
                    <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
                    <path d="M16 21V5a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2z" />
                  </svg>
                </div>
                <div>
                  <p className="text-xs text-[#6b7280] uppercase tracking-widest font-medium">Ghế</p>
                  <p className="text-[#0a0a0a] font-medium">{carSpecs?.seats ?? '—'}</p>
                </div>
              </div>
            </div>

            {car.description && (
              <div className="mb-8">
                <p className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-3">Mô tả</p>
                <p className="text-[#4b5563] leading-relaxed text-sm">{car.description}</p>
              </div>
            )}
          </div>

          {/* CTA */}
          <div className="border border-[#e5e7eb] rounded-2xl p-6 bg-[#fafafa]">
            <div className="flex items-center justify-between mb-6">
              <span className="text-sm font-semibold text-[#6b7280] uppercase tracking-widest">Tổng tiền</span>
              <PriceTag price={car.price} className="text-xl" />
            </div>
            <CarPurchasePanel carId={car.id} isAvailable={isAvailable} carName={car.name} />
          </div>
        </div>
      </div>

      {/* Specs */}
      {carSpecs && (
        <section className="mt-20">
          <h2 className="text-2xl font-serif font-bold text-[#0a0a0a] mb-8 flex items-center gap-3">
            <div className="w-1 h-8 bg-[#c58e00]" />
            Thông số kỹ thuật
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl p-6">
              <h3 className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-4">Động cơ</h3>
              <p className="text-[#4b5563] text-sm">{carSpecs.engine ?? '—'}</p>
            </div>
            <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl p-6">
              <h3 className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-4">Hộp số</h3>
              <p className="text-[#4b5563] text-sm">{carSpecs.transmission ?? '—'}</p>
            </div>
            <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl p-6">
              <h3 className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-4">Trọng lượng</h3>
              <p className="text-[#4b5563] text-sm">{carSpecs.weightKg != null ? `${carSpecs.weightKg} kg` : '—'}</p>
            </div>
            <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl p-6">
              <h3 className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-4">Tính năng an toàn</h3>
              <ul className="text-[#4b5563] text-sm space-y-2">
                {(carSpecs.safetyFeatures?.length ?? 0) > 0 ? (
                  carSpecs.safetyFeatures.map((f: string) => (
                    <li key={f} className="flex items-center gap-2">
                      <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                      {f}
                    </li>
                  ))
                ) : (
                  <p className="text-[#9ca3af]">—</p>
                )}
              </ul>
            </div>
          </div>
        </section>
      )}

      {/* Reviews */}
      <section className="mt-20">
        <div className="flex items-center justify-between mb-8">
          <h2 className="text-2xl font-serif font-bold text-[#0a0a0a] flex items-center gap-3">
            <div className="w-1 h-8 bg-[#c58e00]" />
            Đánh giá khách hàng
          </h2>
          <div className="flex items-center gap-2">
            <RatingStars rating={reviewSummary?.averageRating ?? 0} size={20} />
            <span className="font-serif font-bold text-[#0a0a0a]">
              {formatRating(reviewSummary?.averageRating ?? 0)}
            </span>
            <span className="text-[#6b7280] text-sm ml-2">
              ({reviewSummary?.totalReviews ?? 0} đánh giá)
            </span>
          </div>
        </div>

        <div className="mb-6">
          <ReviewForm carId={car.id} />
        </div>

        {carReviews.length === 0 ? (
          <div className="text-center py-8 text-[#9ca3af] text-sm">Chưa có đánh giá nào cho xe này.</div>
        ) : (
          <div className="space-y-6">
            {carReviews.map((review) => (
              <div key={review.id} className="border-b border-[#e5e7eb] pb-6 last:border-0">
                <div className="flex items-start gap-3 mb-2">
                  <div className="w-10 h-10 rounded-full bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
                    <span className="text-xs font-medium text-[#0a0a0a]">
                      {review.customerName.charAt(0)}
                    </span>
                  </div>
                  <div>
                    <p className="text-sm font-medium text-[#0a0a0a]">{review.customerName}</p>
                    <div className="flex items-center gap-1 mt-0.5">
                      <RatingStars rating={review.rating} size={14} />
                      <span className="text-xs text-[#6b7280]">{formatDate(review.createdAt)}</span>
                    </div>
                  </div>
                </div>
                {review.title && (
                  <h4 className="text-sm font-semibold text-[#0a0a0a] mb-1">{review.title}</h4>
                )}
                <p className="text-[#4b5563] text-sm leading-relaxed">{review.content}</p>
                {review.replies?.length && (
                  <div className="mt-3 pl-8 border-l-2 border-[#e5e7eb] space-y-2">
                    {review.replies.map((reply) => (
                      <div key={reply.createdAt} className="text-[#4b5563] text-sm leading-relaxed">
                        <span className="font-medium text-[#6b7280]">Trả lời:</span> {reply.content}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
