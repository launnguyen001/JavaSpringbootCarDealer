export default function Footer() {
  return (
    <footer className="border-t border-[#e5e7eb] bg-[#0a0a0a] text-[#e5e7eb] pt-12 pb-8">
      <div className="px-6 lg:px-8">
        <div className="flex flex-col lg:flex-row gap-12 mb-10">
          <div>
            <span className="font-serif font-bold text-xl text-white tracking-tighter">CARDEALER</span>
            <p className="mt-3 text-[#9ca3af] text-sm max-w-xs leading-relaxed">
              Đại lý xe hạng sang — trải nghiệm mua sắm đẳng cấp, dịch vụ khách hàng tận tâm.
            </p>
          </div>
          <div className="flex gap-12">
            <div>
              <h4 className="text-white font-medium text-sm mb-4 uppercase tracking-widest">Danh mục</h4>
              <ul className="space-y-2 text-[#9ca3af] text-sm">
                {/* <li><a href="/" className="hover:text-[#c58e00] transition-colors">Hãng xe</a></li>
                <li><a href="/cars" className="hover:text-[#c58e00] transition-colors">Danh sách xe</a></li> */}
                <li><a href="#" className="hover:text-[#c58e00] transition-colors">Lịch lái thử</a></li>
              </ul>
            </div>
            <div>
              <h4 className="text-white font-medium text-sm mb-4 uppercase tracking-widest">Hỗ trợ</h4>
              <ul className="space-y-2 text-[#9ca3af] text-sm">
                <li><a href="#" className="hover:text-[#c58e00] transition-colors">Liên hệ</a></li>
                <li><a href="#" className="hover:text-[#c58e00] transition-colors">Về chúng tôi</a></li>
                <li><a href="#" className="hover:text-[#c58e00] transition-colors">Điều khoản</a></li>
              </ul>
            </div>
          </div>
        </div>
        <div className="border-t border-[#1f2937] pt-8 text-center text-[#6b7280] text-xs">
          © {new Date().getFullYear()} Car Dealer. Thiết kế cho trải nghiệm xa xỉ.
        </div>
      </div>
    </footer>
  );
}
