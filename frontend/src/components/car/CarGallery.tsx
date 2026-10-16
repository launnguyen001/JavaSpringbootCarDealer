'use client';

import { useState } from 'react';

// Gallery ảnh xe: ảnh chính + hàng thumbnail bấm được (đổi ảnh đang xem).
// Phải là client component vì có state — trước đây nằm trong server component
// nên các <button> thumbnail không có onClick và bấm không ăn.
interface CarGalleryProps {
  images: string[];
  thumbnail?: string | null;
  alt: string;
  statusBadge?: React.ReactNode;
}

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=1200&q=80';
const THUMB_CLASS =
  'w-12 h-12 rounded-xl bg-white/90 backdrop-blur border transition-all overflow-hidden ' +
  'hover:border-[#c58e00] focus:outline-none focus:ring-2 focus:ring-[#c58e00]/40';

export default function CarGallery({ images, thumbnail, alt, statusBadge }: CarGalleryProps) {
  const fallback = thumbnail ?? '';
  const list = images.filter(Boolean);
  const gallery = list.length ? list : fallback ? [fallback] : [FALLBACK_IMAGE];

  const [active, setActive] = useState(0);
  // ảnh lỗi (URL 404) thì thay bằng ảnh dự phòng thay vì để trống
  const [broken, setBroken] = useState<Set<number>>(new Set());

  const markBroken = (index: number) => setBroken((prev) => new Set(prev).add(index));

  const srcFor = (index: number) => (broken.has(index) ? FALLBACK_IMAGE : gallery[index] ?? FALLBACK_IMAGE);
  const activeIndex = Math.min(active, gallery.length - 1);

  return (
    <div>
      <div className="relative h-[55vh] max-h-[520px] min-h-[260px] rounded-3xl border border-[#e5e7eb] bg-[#f3f4f6] overflow-hidden shadow-lg">
        <img
          src={srcFor(activeIndex)}
          alt={alt}
          onError={() => markBroken(activeIndex)}
          className="w-full h-full object-cover object-center"
        />
        {statusBadge && <div className="absolute top-4 right-4 z-10">{statusBadge}</div>}

        {gallery.length > 1 && (
          <div className="absolute bottom-4 left-4 z-10 flex gap-2">
            {gallery.slice(0, 6).map((img, i) => (
              <button
                key={i}
                type="button"
                onClick={() => setActive(i)}
                aria-label={`Xem ảnh ${i + 1}`}
                aria-pressed={i === activeIndex}
                className={
                  i === activeIndex
                    ? `${THUMB_CLASS} border-[#c58e00] ring-2 ring-[#c58e00]/40`
                    : `${THUMB_CLASS} border-[#e5e7eb]`
                }
              >
                <img
                  src={srcFor(i)}
                  alt=""
                  onError={() => markBroken(i)}
                  className="w-full h-full object-cover object-center"
                />
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
