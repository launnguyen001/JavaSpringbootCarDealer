'use client';

interface RatingStarsProps {
  rating: number | null | undefined;
  className?: string;
  size?: number;
}

export default function RatingStars({ rating, className, size = 14 }: RatingStarsProps) {
  const stars = rating == null || !Number.isFinite(rating) ? 0 : Math.round(rating);
  return (
    <div
      className={`flex items-center gap-0.5 ${className ?? ''}`}
      style={{ color: '#f59e0b' }}
    >
      {[1, 2, 3, 4, 5].map((i) => (
        <svg
          key={i}
          width={size}
          height={size}
          viewBox="0 0 24 24"
          fill="none"
          stroke={i <= stars ? '#f59e0b' : '#e5e7eb'}
          strokeWidth="2"
          className={`transition-all duration-200 ${i <= stars ? 'fill-[#f59e0b]' : ''}`}
        >
          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
        </svg>
      ))}
    </div>
  );
}
