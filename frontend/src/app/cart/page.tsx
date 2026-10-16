'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import Button from '@/components/ui/Button';
import EmptyState from '@/components/ui/EmptyState';
import Loading from '@/components/ui/Loading';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/context/AuthContext';
import { getErrorMessage } from '@/lib/utils/error';
import { formatVnd } from '@/lib/utils/format';
import { getSessionId } from '@/lib/auth/token';
import { getCart, removeItem, clearCart, listCars } from '@/lib/api';
import type { Car, CartItem } from '@/types';

const PLACEHOLDER = 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d?w=800&q=80';

export default function CartPage() {
  const { isAuthenticated, loading: authLoading } = useAuth();
  const toast = useToast();
  const [items, setItems] = useState<CartItem[]>([]);
  const [cars, setCars] = useState<Record<number, Car>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [cart, allCars] = await Promise.all([getCart(getSessionId()), listCars()]);
      setItems(cart);
      setCars(Object.fromEntries(allCars.map((c) => [c.id, c])));
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    if (!authLoading && isAuthenticated) load();
    if (!authLoading && !isAuthenticated) setLoading(false);
  }, [authLoading, isAuthenticated, load]);

  const handleRemove = async (carId: number) => {
    setBusy(true);
    try {
      const updated = await removeItem(getSessionId(), carId);
      setItems(updated);
      toast.success('Đã xóa khỏi giỏ hàng');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const handleClear = async () => {
    setBusy(true);
    try {
      await clearCart(getSessionId());
      setItems([]);
      toast.success('Đã xóa toàn bộ giỏ hàng');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const subtotal = items.reduce((sum, it) => sum + (cars[it.carId]?.price ?? 0) * it.quantity, 0);

  if (authLoading || loading) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <Loading />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="max-w-3xl mx-auto px-6 py-20 text-center">
        <h1 className="text-3xl font-serif font-bold mb-4">Giỏ hàng</h1>
        <p className="text-[#6b7280] mb-8">
          Vui lòng đăng nhập để xem và quản lý giỏ hàng của bạn.
        </p>
        <Link
          href="/login?next=/cart"
          className="inline-block px-8 py-3.5 rounded-xl bg-[#0a0a0a] text-white font-medium hover:bg-[#c58e00] transition-colors"
        >
          Đăng nhập
        </Link>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-6 lg:px-8 py-12">
      <h1 className="text-3xl font-serif font-bold text-[#0a0a0a] mb-8">Giỏ hàng của bạn</h1>

      {items.length === 0 ? (
        <EmptyState
          title="Giỏ hàng trống"
          subtitle="Hãy khám phá các dòng xe và thêm vào giỏ hàng nhé."
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-4">
            {items.map((item) => {
              const car = cars[item.carId];
              if (!car) return null;
              return (
                <div
                  key={item.carId}
                  className="flex items-center gap-4 p-4 rounded-2xl border border-[#e5e7eb] bg-white"
                >
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={PLACEHOLDER}
                    alt={car.name}
                    className="w-24 h-20 object-cover rounded-xl"
                  />
                  <div className="flex-1 min-w-0">
                    <Link
                      href={`/cars/${car.id}`}
                      className="font-serif font-bold text-[#0a0a0a] hover:text-[#c58e00]"
                    >
                      {car.name}
                    </Link>
                    <p className="text-xs text-[#9ca3af] mt-0.5">
                      {car.brandName ?? ''} {car.modelName ? `• ${car.modelName}` : ''}
                    </p>
                    <p className="text-sm text-[#6b7280] mt-1">
                      Số lượng: <b>{item.quantity}</b>
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-serif font-bold text-[#0a0a0a]">
                      {formatVnd(car.price * item.quantity)}
                    </p>
                    <button
                      onClick={() => handleRemove(item.carId)}
                      disabled={busy}
                      className="text-xs text-[#ef4444] hover:underline mt-2"
                    >
                      Xóa
                    </button>
                  </div>
                </div>
              );
            })}
            <button
              onClick={handleClear}
              disabled={busy}
              className="text-sm text-[#6b7280] hover:text-[#ef4444]"
            >
              Xóa toàn bộ giỏ hàng
            </button>
          </div>

          <aside className="lg:col-span-1">
            <div className="sticky top-24 rounded-2xl border border-[#e5e7eb] bg-white p-6">
              <h2 className="font-serif font-bold text-lg mb-4">Tổng cộng</h2>
              <div className="flex justify-between text-sm text-[#6b7280] mb-2">
                <span>Số xe</span>
                <span>{items.length}</span>
              </div>
              <div className="flex justify-between items-center border-t border-[#f3f4f6] pt-4 mt-4">
                <span className="font-medium">Tạm tính</span>
                <span className="font-serif font-bold text-xl text-[#0a0a0a]">
                  {formatVnd(subtotal)}
                </span>
              </div>
              <Link
                href="/checkout"
                className="mt-6 block text-center px-6 py-3.5 rounded-xl bg-[#c58e00] text-white font-medium hover:bg-[#8a6d00] transition-colors"
              >
                Tiến hành đặt hàng
              </Link>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
