import Link from 'next/link';

export default function NotFound() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center text-center px-6">
      <img
        src="https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800&q=80"
        alt="404"
        className="w-64 h-64 rounded-full shadow-2xl mb-8 opacity-80 object-cover"
      />
      <h1 className="text-6xl lg:text-8xl font-serif font-bold text-[#0a0a0a] mb-2">404</h1>
      <p className="text-lg text-[#6b7280] mb-10 max-w-xs">
        Trang bạn đang tìm không tồn tại. Hãy quay lại trang chủ để tiếp tục khám phá.
      </p>
      <Link
        href="/"
        className="px-8 py-4 bg-[#0a0a0a] text-white font-serif font-semibold text-lg tracking-widest uppercase hover:bg-[#c58e00] transition-colors"
      >
        Về trang chủ
      </Link>
    </div>
  );
}
