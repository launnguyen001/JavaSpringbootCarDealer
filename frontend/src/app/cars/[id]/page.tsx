// import { notFound } from 'next/navigation';
// import {
//   getCar,
//   getCarMedia,
//   getCarSpecs,
//   getCarReviews,
//   getCarReviewSummary,
// } from '@/lib/api';
// import { formatDate, formatRating } from '@/lib/utils/format';
// import PriceTag from '@/components/ui/PriceTag';
// import StatusBadge from '@/components/ui/StatusBadge';
// import RatingStars from '@/components/ui/RatingStars';
// import CarPurchasePanel from '@/components/car/CarPurchasePanel';
// import CarGallery from '@/components/car/CarGallery';
// import { SelectedItemProvider } from '@/components/car/SelectedItemContext';
// import { SelectedItemTotal, SelectedItemSpecs } from '@/components/car/SelectedItemSummary';
// import ReviewForm from '@/components/car/ReviewForm';
// import ReviewItem from '@/components/car/ReviewItem';
// import Link from 'next/link';
// import type { CarSpecification, CarReview } from '@/types';

// export const dynamic = 'force-dynamic';

// interface CarDetailPageProps {
//   params: Promise<{ id: string }>;
// }

// export async function generateMetadata({ params }: CarDetailPageProps) {
//   const { id } = await params;
//   let name = 'Xe';
//   try {
//     const car = await getCar(Number(id));
//     name = car.name;
//   } catch {
//     // ignore
//   }
//   return {
//     title: `${name} — Car Dealer`,
//     description: `Chi tiết xe ${name} tại đại lý xe hạng sang Car Dealer.`,
//   };
// }

// export default async function CarDetailPage({ params }: CarDetailPageProps) {
//   const { id } = await params;
//   const carId = Number(id);

//   let car;
//   try {
//     car = await getCar(carId);
//   } catch {
//     notFound();
//   }
//   if (!car) notFound();

//   const [mediaRes, specsRes, reviewsRes, summaryRes] = await Promise.allSettled([
//     getCarMedia(car.id),
//     getCarSpecs(car.id),
//     getCarReviews(car.id),
//     getCarReviewSummary(car.id),
//   ]);

//   const carMedia: Awaited<ReturnType<typeof getCarMedia>> | null =
//     mediaRes.status === 'fulfilled' ? mediaRes.value : null;
//   const carSpecs: CarSpecification | null =
//     specsRes.status === 'fulfilled' ? specsRes.value : null;
//   const carReviews: CarReview[] =
//     reviewsRes.status === 'fulfilled' ? reviewsRes.value : [];
//   const reviewSummary: Awaited<ReturnType<typeof getCarReviewSummary>> | null =
//     summaryRes.status === 'fulfilled' ? summaryRes.value : null;

//   const images: string[] = (
//     carMedia?.images?.length ? carMedia.images : [carMedia?.thumbnail ?? '']
//   ).filter((img): img is string => Boolean(img));
//   const videos: string[] = (carMedia?.videos?.length ? carMedia.videos : []).filter(Boolean);
//   const thumbnail = carMedia?.thumbnail ?? null;

//   // Giá hiển thị theo các chiếc thực tế (priceFrom..priceTo), nếu chỉ một mức thì lấy mức đó.
//   const priceFrom = car.priceFrom ?? car.basePrice;
//   const priceTo = car.priceTo ?? car.priceFrom ?? car.basePrice;
//   const isPriceRange = priceFrom != null && priceTo != null && priceFrom !== priceTo;

//   return (
//     <div className="min-h-[calc(100vh-200px)] py-12 px-6 lg:px-12 max-w-7xl mx-auto">
//       <nav className="mb-8 flex items-center gap-2 text-[#6b7280] text-sm">
//         <Link href="/" className="hover:text-[#c58e00] transition-colors">Trang chủ</Link>
//         <span className="mx-1">/</span>
//         <Link href="/cars" className="hover:text-[#c58e00] transition-colors">Danh sách xe</Link>
//         <span className="mx-1">/</span>
//         <span className="font-medium text-[#0a0a0a]">{car.name}</span>
//       </nav>

//       <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
//         {/* Gallery — client component, bấm thumbnail để đổi ảnh chính */}
//         <div className="lg:col-span-7">
//           <CarGallery
//             images={images}
//             videos={videos}
//             thumbnail={thumbnail}
//             alt={car.name}
//             statusBadge={<StatusBadge status={car.status} className="shadow-lg" />}
//           />
//         </div>

//         {/* Details */}
//         <div className="lg:col-span-5 flex flex-col gap-8">
//           <SelectedItemProvider items={car.items}>
//           <div>
//             <div className="flex items-baseline gap-3 mb-2">
//               <PriceTag price={priceFrom} className="text-3xl lg:text-4xl" />
//               {isPriceRange && (
//                 <span className="text-[#6b7280] text-lg font-medium">
//                   – <PriceTag price={priceTo} className="text-lg" />
//                 </span>
//               )}
//               {car.availableItems > 0 && (
//                 <span className="text-[#6b7280] text-sm font-medium">
//                   {car.availableItems} xe còn lại
//                 </span>
//               )}
//             </div>
//             <h1 className="text-4xl lg:text-5xl font-serif font-bold text-[#0a0a0a] leading-tight mb-4">
//               {car.name}
//             </h1>
//             {car.brandName && car.modelName && (
//               <p className="text-[#6b7280] text-lg tracking-wide uppercase font-medium mb-6">
//                 {car.brandName} • {car.modelName}
//               </p>
//             )}

//             <div className="space-y-4 mb-8">
//               <div className="flex items-start gap-4">
//                 <div className="w-12 h-12 rounded-xl bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
//                   <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6b7280" strokeWidth="1.5">
//                     <circle cx="12" cy="12" r="3" />
//                     <path d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
//                   </svg>
//                 </div>
//                 <div>
//                   <p className="text-xs text-[#6b7280] uppercase tracking-widest font-medium">Diện tích</p>
//                   <p className="text-[#0a0a0a] font-medium">{carSpecs?.dimensions ?? '—'}</p>
//                 </div>
//               </div>
//               <div className="flex items-start gap-4">
//                 <div className="w-12 h-12 rounded-xl bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
//                   <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6b7280" strokeWidth="1.5">
//                     <path d="M19 11H5c-1.1 0-2-.9-2-2V5c0-1.1.9-2 2-2h14c1.1 0 2 .9 2 2v4c0 1.1-.9 2-2 2z" />
//                     <line x1="12" y1="19" x2="12" y2="23" />
//                     <line x1="8" y1="23" x2="16" y2="23" />
//                   </svg>
//                 </div>
//                 <div>
//                   <p className="text-xs text-[#6b7280] uppercase tracking-widest font-medium">Nhiên liệu</p>
//                   <p className="text-[#0a0a0a] font-medium">{carSpecs?.fuelType ?? '—'}</p>
//                 </div>
//               </div>
//               <div className="flex items-start gap-4">
//                 <div className="w-12 h-12 rounded-xl bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
//                   <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#6b7280" strokeWidth="1.5">
//                     <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
//                     <path d="M16 21V5a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2z" />
//                   </svg>
//                 </div>
//                 <div>
//                   <p className="text-xs text-[#6b7280] uppercase tracking-widest font-medium">Ghế</p>
//                   <p className="text-[#0a0a0a] font-medium">{carSpecs?.seats ?? '—'}</p>
//                 </div>
//               </div>
//               <SelectedItemSpecs />
//             </div>

//             {car.description && (
//               <div className="mb-8">
//                 <p className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-3">Mô tả</p>
//                 <p className="text-[#4b5563] leading-relaxed text-sm">{car.description}</p>
//               </div>
//             )}
//           </div>

//           {/* CTA */}
//           <div className="border border-[#e5e7eb] rounded-2xl p-6 bg-[#fafafa]">
//             <div className="flex items-center justify-between mb-6">
//               <span className="text-sm font-semibold text-[#6b7280] uppercase tracking-widest">Tổng tiền</span>
//               <SelectedItemTotal fallback={priceFrom} />
//             </div>
//             <CarPurchasePanel carId={car.id} carName={car.name} />
//           </div>
//           </SelectedItemProvider>
//         </div>
//       </div>

//       {/* Specs */}
//       {carSpecs && (
//         <section className="mt-20">
//           <h2 className="text-2xl font-serif font-bold text-[#0a0a0a] mb-8 flex items-center gap-3">
//             <div className="w-1 h-8 bg-[#c58e00]" />
//             Thông số kỹ thuật
//           </h2>
//           <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
//             <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl p-6">
//               <h3 className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-4">Động cơ</h3>
//               <p className="text-[#4b5563] text-sm">{carSpecs.engine ?? '—'}</p>
//             </div>
//             <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl p-6">
//               <h3 className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-4">Hộp số</h3>
//               <p className="text-[#4b5563] text-sm">{carSpecs.transmission ?? '—'}</p>
//             </div>
//             <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl p-6">
//               <h3 className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-4">Trọng lượng</h3>
//               <p className="text-[#4b5563] text-sm">{carSpecs.weightKg != null ? `${carSpecs.weightKg} kg` : '—'}</p>
//             </div>
//             <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl p-6">
//               <h3 className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-4">Tính năng an toàn</h3>
//               <ul className="text-[#4b5563] text-sm space-y-2">
//                 {(carSpecs.safetyFeatures?.length ?? 0) > 0 ? (
//                   carSpecs.safetyFeatures.map((f: string) => (
//                     <li key={f} className="flex items-center gap-2">
//                       <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2">
//                         <polyline points="20 6 9 17 4 12" />
//                       </svg>
//                       {f}
//                     </li>
//                   ))
//                 ) : (
//                   <p className="text-[#9ca3af]">—</p>
//                 )}
//               </ul>
//             </div>
//           </div>
//         </section>
//       )}

//       {/* Reviews */}
//       <section className="mt-20">
//         <div className="flex items-center justify-between mb-8">
//           <h2 className="text-2xl font-serif font-bold text-[#0a0a0a] flex items-center gap-3">
//             <div className="w-1 h-8 bg-[#c58e00]" />
//             Đánh giá khách hàng
//           </h2>
//           <div className="flex items-center gap-2">
//             <RatingStars rating={reviewSummary?.averageRating ?? 0} size={20} />
//             <span className="font-serif font-bold text-[#0a0a0a]">
//               {formatRating(reviewSummary?.averageRating ?? 0)}
//             </span>
//             <span className="text-[#6b7280] text-sm ml-2">
//               ({reviewSummary?.totalReviews ?? 0} đánh giá)
//             </span>
//           </div>
//         </div>

//         <div className="mb-6">
//           <ReviewForm carId={car.id} />
//         </div>

//         {carReviews.length === 0 ? (
//           <div className="text-center py-8 text-[#9ca3af] text-sm">Chưa có đánh giá nào cho xe này.</div>
//         ) : (
//           <div className="space-y-6">
//             {carReviews.map((review) => (
//               <div key={review.id} className="border-b border-[#e5e7eb] pb-6 last:border-0">
//                 <div className="flex items-start gap-3 mb-2">
//                   <div className="w-10 h-10 rounded-full bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
//                     <span className="text-xs font-medium text-[#0a0a0a]">
//                       {review.customerName.charAt(0)}
//                     </span>
//                   </div>
//                   <div>
//                     <p className="text-sm font-medium text-[#0a0a0a]">{review.customerName}</p>
//                     <div className="flex items-center gap-1 mt-0.5">
//                       <RatingStars rating={review.rating} size={14} />
//                       <span className="text-xs text-[#6b7280]">{formatDate(review.createdAt)}</span>
//                     </div>
//                   </div>
//                 </div>
//                 {review.title && (
//                   <h4 className="text-sm font-semibold text-[#0a0a0a] mb-1">{review.title}</h4>
//                 )}
//                 <p className="text-[#4b5563] text-sm leading-relaxed">{review.content}</p>
//                 {review.replies?.length && (
//                   <div className="mt-3 pl-8 border-l-2 border-[#e5e7eb] space-y-2">
//                     {review.replies.map((reply) => (
//                       <div key={reply.createdAt} className="text-[#4b5563] text-sm leading-relaxed">
//                         <span className="font-medium text-[#6b7280]">Trả lời:</span> {reply.content}
//                       </div>
//                     ))}
//                   </div>
//                 )}
//               </div>
//             ))}
//           </div>
//         )}
//       </section>
//     </div>
//   );
// }
import { notFound } from 'next/navigation';
import {
  getCar,
  getCarMedia,
  getCarSpecs,
  getCarReviews,
  getCarReviewSummary,
} from '@/lib/api';
import { formatDate, formatRating } from '@/lib/utils/format';
import PriceTag from '@/components/ui/PriceTag';
import StatusBadge from '@/components/ui/StatusBadge';
import RatingStars from '@/components/ui/RatingStars';
import { ItemPicker, PurchaseActions } from '@/components/car/CarPurchasePanel';
import CarGallery from '@/components/car/CarGallery';
import { SelectedItemProvider } from '@/components/car/SelectedItemContext';
import { SelectedItemTotal, SelectedItemSpecs } from '@/components/car/SelectedItemSummary';
import ReviewForm from '@/components/car/ReviewForm';
import ReviewItem from '@/components/car/ReviewItem';
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

  const images: string[] = (
    carMedia?.images?.length ? carMedia.images : [carMedia?.thumbnail ?? '']
  ).filter((img): img is string => Boolean(img));
  const videos: string[] = (carMedia?.videos?.length ? carMedia.videos : []).filter(Boolean);
  const thumbnail = carMedia?.thumbnail ?? null;

  // Giá hiển thị theo các chiếc thực tế (priceFrom..priceTo), nếu chỉ một mức thì lấy mức đó.
  const priceFrom = car.priceFrom ?? car.basePrice;
  const priceTo = car.priceTo ?? car.priceFrom ?? car.basePrice;
  const isPriceRange = priceFrom != null && priceTo != null && priceFrom !== priceTo;

  return (
    <div className="min-h-[calc(100vh-200px)] py-12 px-6 lg:px-12 max-w-7xl mx-auto">
      <nav className="mb-8 flex items-center gap-2 text-[#6b7280] text-sm">
        <Link href="/" className="hover:text-[#c58e00] transition-colors">Trang chủ</Link>
        <span className="mx-1">/</span>
        <Link href="/cars" className="hover:text-[#c58e00] transition-colors">Danh sách xe</Link>
        <span className="mx-1">/</span>
        <span className="font-medium text-[#0a0a0a]">{car.name}</span>
      </nav>

      <SelectedItemProvider items={car.items}>
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
          {/* ===== CỘT TRÁI: gallery + mô tả + thông số kỹ thuật ===== */}
          <div className="lg:col-span-7 flex flex-col gap-10">
            <CarGallery
              images={images}
              videos={videos}
              thumbnail={thumbnail}
              alt={car.name}
              statusBadge={<StatusBadge status={car.status} className="shadow-lg" />}
            />

            {car.description && (
              <div>
                <p className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-3">Mô tả</p>
                <p className="text-[#4b5563] leading-relaxed text-sm">{car.description}</p>
              </div>
            )}

            {carSpecs && (
              <section>
                <h2 className="text-2xl font-serif font-bold text-[#0a0a0a] mb-6 flex items-center gap-3">
                  <div className="w-1 h-8 bg-[#c58e00]" />
                  Thông số kỹ thuật
                </h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <SpecCard title="Động cơ">{carSpecs.engine ?? '—'}</SpecCard>
                  <SpecCard title="Hộp số">{carSpecs.transmission ?? '—'}</SpecCard>
                  <SpecCard title="Trọng lượng">
                    {carSpecs.weightKg != null ? `${carSpecs.weightKg} kg` : '—'}
                  </SpecCard>
                  <SpecCard title="Tính năng an toàn">
                    {(carSpecs.safetyFeatures?.length ?? 0) > 0 ? (
                      <ul className="space-y-2">
                        {carSpecs.safetyFeatures.map((f: string) => (
                          <li key={f} className="flex items-center gap-2">
                            <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2">
                              <polyline points="20 6 9 17 4 12" />
                            </svg>
                            {f}
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <span className="text-[#9ca3af]">—</span>
                    )}
                  </SpecCard>
                </div>
              </section>
            )}
          </div>

          {/* ===== CỘT PHẢI: sticky – chọn xe / specs xe đã chọn / tổng tiền / nút mua ===== */}
          <aside className="lg:col-span-5 flex flex-col gap-6 lg:sticky lg:top-24 lg:self-start lg:max-h-[calc(100vh-7rem)] lg:overflow-y-auto">
            {/* Tiêu đề + giá */}
            <div>
              {car.brandName && car.modelName && (
                <p className="text-[#6b7280] text-sm tracking-wide uppercase font-medium mb-2">
                  {car.brandName} • {car.modelName}
                </p>
              )}
              <h1 className="text-3xl lg:text-4xl font-serif font-bold text-[#0a0a0a] leading-tight mb-3">
                {car.name}
              </h1>
              <div className="flex items-baseline flex-wrap gap-x-3 gap-y-1">
                <PriceTag price={priceFrom} className="text-2xl lg:text-3xl" />
                {isPriceRange && (
                  <span className="text-[#6b7280] text-lg font-medium">
                    – <PriceTag price={priceTo} className="text-lg" />
                  </span>
                )}
                {car.availableItems > 0 && (
                  <span className="text-[#6b7280] text-sm font-medium">
                    {car.availableItems} xe còn lại
                  </span>
                )}
              </div>
            </div>

            {/* Thông số nhanh */}
            <div className="grid grid-cols-3 gap-3">
              <QuickSpec label="Kích thước" value={carSpecs?.dimensions} />
              <QuickSpec label="Nhiên liệu" value={carSpecs?.fuelType} />
              <QuickSpec label="Ghế" value={carSpecs?.seats} />
            </div>

            {/* Card thao tác: chọn xe → specs xe đã chọn → tổng tiền → nút mua */}
            <div className="border border-[#e5e7eb] rounded-2xl p-5 bg-[#fafafa] flex flex-col gap-5">
              <ItemPicker />

              <div className="border-t border-[#e5e7eb] pt-4">
                <SelectedItemSpecs />
              </div>

              <div className="flex items-center justify-between border-t border-[#e5e7eb] pt-4">
                <span className="text-sm font-semibold text-[#6b7280] uppercase tracking-widest">Tổng tiền</span>
                <SelectedItemTotal fallback={priceFrom} />
              </div>

              <div>
                <PurchaseActions carId={car.id} carName={car.name} />
              </div>
            </div>
          </aside>
        </div>
      </SelectedItemProvider>

      {/* ===== Đánh giá ===== */}
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
              <ReviewItem key={review.id} review={review} carId={car.id} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

function SpecCard({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="bg-[#fafafa] border border-[#e5e7eb] rounded-2xl p-5">
      <h3 className="text-xs font-semibold text-[#6b7280] uppercase tracking-widest mb-3">{title}</h3>
      <div className="text-[#4b5563] text-sm">{children}</div>
    </div>
  );
}

function QuickSpec({ label, value }: { label: string; value?: string | number | null }) {
  return (
    <div className="rounded-xl bg-[#f3f4f6] px-3 py-3">
      <p className="text-[10px] text-[#6b7280] uppercase tracking-widest font-medium mb-1">{label}</p>
      <p className="text-sm text-[#0a0a0a] font-medium truncate">{value ?? '—'}</p>
    </div>
  );
}