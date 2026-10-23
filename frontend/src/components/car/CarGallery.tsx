'use client';

import { useState } from 'react';
import { parseVideo } from '@/lib/utils/video';

// Gallery ảnh/video xe: khung chính + hàng thumbnail bấm được (đổi media đang xem).
// Hỗ trợ cả link YouTube (youtu.be, watch?v=, shorts) lẫn file video trực tiếp (.mp4, .webm).
type MediaType = 'image' | 'video';

interface MediaItem {
  type: MediaType;
  src: string;
}

interface CarGalleryProps {
  images: string[];
  videos: string[];
  thumbnail?: string | null;
  alt: string;
  statusBadge?: React.ReactNode;
}

const FALLBACK_IMAGE = 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=1200&q=80';
const THUMB_CLASS =
  'relative shrink-0 w-16 h-16 rounded-xl bg-white border-2 transition-all overflow-hidden ' +
  'hover:border-[#c58e00] focus:outline-none focus:ring-2 focus:ring-[#c58e00]/40';

function PlayIcon() {
  return (
    <div className="absolute inset-0 flex items-center justify-center bg-black/25">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="white" className="drop-shadow-md">
        <polygon points="5,3 19,12 5,21" />
      </svg>
    </div>
  );
}

export default function CarGallery({
  images,
  videos,
  thumbnail,
  alt,
  statusBadge,
}: CarGalleryProps) {
  const fallback = thumbnail ?? '';
  const imageList = images.filter(Boolean);
  const videoList = videos.filter(Boolean);

  const media: MediaItem[] = [
    ...imageList.map((src): MediaItem => ({ type: 'image', src })),
    ...videoList.map((src): MediaItem => ({ type: 'video', src })),
  ];
  const gallery: MediaItem[] = media.length
    ? media
    : [{ type: 'image', src: fallback || FALLBACK_IMAGE }];

  const [active, setActive] = useState(0);
  // ảnh/video lỗi (URL 404) thì thay bằng ảnh dự phòng thay vì để trống
  const [broken, setBroken] = useState<Set<number>>(new Set());
  const markBroken = (index: number) => setBroken((prev) => new Set(prev).add(index));

  const srcFor = (index: number) =>
    broken.has(index) ? FALLBACK_IMAGE : (gallery[index]?.src ?? FALLBACK_IMAGE);

  const activeIndex = Math.min(active, gallery.length - 1);
  const activeMedia = gallery[activeIndex];
  const activeVideo = activeMedia?.type === 'video' ? parseVideo(activeMedia.src) : null;

  return (
    <div className="flex flex-col gap-4">
      {/* Khung chính */}
      <div className="relative h-[55vh] max-h-[520px] min-h-[260px] rounded-3xl border border-[#e5e7eb] bg-[#f3f4f6] overflow-hidden shadow-lg">
        {activeVideo?.type === 'youtube' ? (
          <iframe
            key={activeIndex}
            src={`${activeVideo.embedUrl}?rel=0`}
            title={alt}
            className="absolute inset-0 w-full h-full"
            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; fullscreen"
            allowFullScreen
            referrerPolicy="strict-origin-when-cross-origin"
          />
        ) : activeVideo?.type === 'file' ? (
          <video
            key={activeIndex}
            src={activeVideo.url}
            controls
            playsInline
            preload="metadata"
            className="w-full h-full object-cover object-center"
            onError={() => markBroken(activeIndex)}
          />
        ) : (
          <img
            src={srcFor(activeIndex)}
            alt={alt}
            onError={() => markBroken(activeIndex)}
            className="w-full h-full object-cover object-center"
          />
        )}

        {statusBadge && <div className="absolute top-4 right-4 z-10">{statusBadge}</div>}
      </div>

      {/* Hàng thumbnail: nằm dưới khung chính để không che thanh điều khiển video */}
      {gallery.length > 1 && (
        <div className="flex gap-3 overflow-x-auto pb-1">
          {gallery.map((item, i) => {
            const isActive = i === activeIndex;
            const cls = isActive
              ? `${THUMB_CLASS} border-[#c58e00] ring-2 ring-[#c58e00]/40`
              : `${THUMB_CLASS} border-[#e5e7eb]`;
            const v = item.type === 'video' ? parseVideo(item.src) : null;

            return (
              <button
                key={i}
                type="button"
                onClick={() => setActive(i)}
                aria-label={`Xem ${item.type === 'video' ? 'video' : 'ảnh'} ${i + 1}`}
                aria-pressed={isActive}
                className={cls}
              >
                {v?.type === 'youtube' ? (
                  <>
                    <img
                      src={broken.has(i) ? FALLBACK_IMAGE : v.thumbUrl}
                      alt=""
                      onError={() => markBroken(i)}
                      className="w-full h-full object-cover object-center"
                    />
                    <PlayIcon />
                  </>
                ) : v?.type === 'file' ? (
                  <>
                    <video
                      src={v.url}
                      muted
                      playsInline
                      preload="metadata"
                      className="w-full h-full object-cover object-center"
                      onError={() => markBroken(i)}
                    />
                    <PlayIcon />
                  </>
                ) : (
                  <img
                    src={srcFor(i)}
                    alt=""
                    onError={() => markBroken(i)}
                    className="w-full h-full object-cover object-center"
                  />
                )}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}