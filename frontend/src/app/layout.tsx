import type { Metadata } from 'next';
import { Suspense } from 'react';
import { Inter } from 'next/font/google';
import './globals.css';
import Header from '@/components/layout/Header';
import ConditionalFooter from '@/components/layout/ConditionalFooter';
import { AuthProvider } from '@/context/AuthContext';
import { ToastProvider } from '@/components/ui/Toast';

const inter = Inter({ subsets: ['latin'], variable: '--font-inter' });

export const metadata: Metadata = {
  title: 'Car Dealer — Đại lý xe hạng sang',
  description: 'Khám phá dòng xe luxury, siêu xe và các mẫu xe đẳng cấp với dịch vụ tận tâm.',
  openGraph: {
    title: 'Car Dealer — Xe hạng sang',
    description: 'Trải nghiệm mua xe xa xỉ, dịch vụ khách hàng đẳng cấp.',
    type: 'website',
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="vi" className="scroll-smooth">
      <body className={`${inter.variable} font-sans antialiased bg-[#ffffff] text-[#0a0a0a] selection:bg-[#c58e00] selection:text-white`}>
        <AuthProvider>
          <ToastProvider>
            <Suspense fallback={
              <header className="sticky top-0 z-40 border-b border-[#e5e7eb] bg-white/80 backdrop-blur-md">
                <div className="px-6 lg:px-8 h-16" />
              </header>
            }>
              <Header />
            </Suspense>
            <Suspense fallback={<div className="min-h-[600px] flex items-center justify-center" />}>
              <main>{children}</main>
            </Suspense>
            <ConditionalFooter />
          </ToastProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
