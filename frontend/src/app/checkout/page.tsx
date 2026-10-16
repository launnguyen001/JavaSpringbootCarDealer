'use client';

import { useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import Button from '@/components/ui/Button';
import { Field, Input, Textarea } from '@/components/ui/Field';
import Loading from '@/components/ui/Loading';
import ErrorMessage from '@/components/ui/ErrorMessage';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/context/AuthContext';
import { getErrorMessage } from '@/lib/utils/error';
import { formatVnd } from '@/lib/utils/format';
import { getSessionId, setCustomerId } from '@/lib/auth/token';
import {
  getCart,
  createOrder,
  clearCart,
  getCarItem,
  getMyProfile,
  updateMyProfile,
} from '@/lib/api';
import type { CarItem, CartItem, Customer, Order } from '@/types';

export default function CheckoutPage() {
  const { user, isAuthenticated, loading: authLoading } = useAuth();
  const toast = useToast();

  const [profile, setProfile] = useState<Customer | null>(null);
  const [items, setItems] = useState<CartItem[]>([]);
  const [carItems, setCarItems] = useState<Record<number, CarItem>>({});
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [placed, setPlaced] = useState<Order | null>(null);

  // form tạo hồ sơ khách hàng
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    try {
      // Giỏ hàng + chi tiết từng chiếc xe đang có trong giỏ.
      const cart = await getCart(getSessionId());
      setItems(cart);
      const detailed = await Promise.all(cart.map((it) => getCarItem(it.carItemId)));
      setCarItems(Object.fromEntries(detailed.map((c) => [c.id, c])));
      // Hồ sơ khách hàng gắn với tài khoản đang đăng nhập (nếu đã có).
      try {
        const p = await getMyProfile();
        setProfile(p);
        setCustomerId(p.id);
      } catch {
        setProfile(null);
      }
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

  const submitProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      // PUT /api/customers/me: tạo hồ sơ nếu chưa có, cập nhật nếu đã có.
      const saved = await updateMyProfile({
        fullName,
        phone: phone || null,
        address: address || null,
        email: user?.email ?? null,
      });
      setProfile(saved);
      setCustomerId(saved.id);
      toast.success('Đã lưu hồ sơ khách hàng');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const placeOrder = async () => {
    if (!profile || items.length === 0) return;
    setBusy(true);
    try {
      const order = await createOrder({
        customerId: profile.id,
        items: items.map((i) => ({ carItemId: i.carItemId })),
      });
      await clearCart(getSessionId());
      setPlaced(order);
      setItems([]);
      toast.success('Đặt hàng thành công!');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const subtotal = items.reduce(
    (sum, it) => sum + (carItems[it.carItemId]?.price ?? 0) * it.quantity,
    0,
  );

  if (authLoading || (loading && isAuthenticated)) {
    return (
      <div className="min-h-[60vh] flex items-center justify-center">
        <Loading />
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <div className="max-w-2xl mx-auto px-6 py-20 text-center">
        <h1 className="text-3xl font-serif font-bold mb-4">Thanh toán</h1>
        <p className="text-[#6b7280] mb-8">Vui lòng đăng nhập để tiến hành đặt hàng.</p>
        <Link
          href="/login?next=/checkout"
          className="inline-block px-8 py-3.5 rounded-xl bg-[#0a0a0a] text-white font-medium hover:bg-[#c58e00] transition-colors"
        >
          Đăng nhập
        </Link>
      </div>
    );
  }

  if (placed) {
    return (
      <div className="max-w-2xl mx-auto px-6 py-20 text-center">
        <div className="w-16 h-16 mx-auto rounded-full bg-[#ecfdf5] flex items-center justify-center mb-6">
          <svg width="30" height="30" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5">
            <path d="M20 6L9 17l-5-5" />
          </svg>
        </div>
        <h1 className="text-3xl font-serif font-bold mb-3">Đặt hàng thành công!</h1>
        <p className="text-[#6b7280] mb-2">
          Mã đơn hàng của bạn: <b className="text-[#0a0a0a]">{placed.orderCode}</b>
        </p>
        <p className="text-[#6b7280] mb-8">
          Tổng tiền: <b className="text-[#0a0a0a]">{formatVnd(placed.totalAmount)}</b>
        </p>
        <div className="flex justify-center gap-3">
          <Link
            href="/account"
            className="px-6 py-3 rounded-xl bg-[#0a0a0a] text-white font-medium hover:bg-[#c58e00] transition-colors"
          >
            Xem đơn hàng
          </Link>
          <Link
            href="/cars"
            className="px-6 py-3 rounded-xl border border-[#e5e7eb] font-medium hover:bg-[#f9fafb] transition-colors"
          >
            Tiếp tục mua sắm
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-6 lg:px-8 py-12">
      <h1 className="text-3xl font-serif font-bold text-[#0a0a0a] mb-8">Thanh toán</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          {!profile ? (
            <div className="rounded-2xl border border-[#e5e7eb] bg-white p-6">
              <h2 className="font-serif font-bold text-lg mb-1">Thông tin khách hàng</h2>
              <p className="text-sm text-[#6b7280] mb-5">
                Bạn cần tạo hồ sơ khách hàng trước khi đặt hàng.
              </p>
              <form onSubmit={submitProfile} className="space-y-4">
                <Field label="Họ và tên" required>
                  <Input
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    required
                    placeholder="Nguyễn Văn A"
                  />
                </Field>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Field label="Số điện thoại">
                    <Input value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="09xx xxx xxx" />
                  </Field>
                  <Field label="Email">
                    <Input value={user?.email ?? ''} disabled />
                  </Field>
                </div>
                <Field label="Địa chỉ">
                  <Textarea value={address} onChange={(e) => setAddress(e.target.value)} />
                </Field>
                <Button type="submit" variant="gold" loading={busy}>
                  Lưu hồ sơ
                </Button>
              </form>
            </div>
          ) : (
            <div className="rounded-2xl border border-[#e5e7eb] bg-white p-6">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-[#ecfdf5] flex items-center justify-center">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5">
                    <path d="M20 6L9 17l-5-5" />
                  </svg>
                </div>
                <div>
                  <p className="font-medium text-[#0a0a0a]">Hồ sơ khách hàng đã sẵn sàng</p>
                  <p className="text-sm text-[#6b7280]">
                    {profile.fullName} • Mã khách hàng: #{profile.id}
                  </p>
                </div>
              </div>
            </div>
          )}

          <div className="rounded-2xl border border-[#e5e7eb] bg-white p-6">
            <h2 className="font-serif font-bold text-lg mb-4">Sản phẩm ({items.length})</h2>
            {items.length === 0 ? (
              <p className="text-sm text-[#6b7280]">
                Giỏ hàng trống.{' '}
                <Link href="/cars" className="text-[#c58e00] hover:underline">
                  Chọn xe
                </Link>
              </p>
            ) : (
              <div className="space-y-3">
                {items.map((it) => {
                  const car = carItems[it.carItemId];
                  if (!car) return null;
                  return (
                    <div key={it.carItemId} className="flex justify-between text-sm">
                      <span className="text-[#374151]">
                        {car.carName}
                        {car.vin ? ` (${car.vin})` : ''} × {it.quantity}
                      </span>
                      <span className="font-medium">{formatVnd(car.price * it.quantity)}</span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        <aside className="lg:col-span-1">
          <div className="sticky top-24 rounded-2xl border border-[#e5e7eb] bg-white p-6">
            <h2 className="font-serif font-bold text-lg mb-4">Tổng cộng</h2>
            <div className="flex justify-between items-center border-b border-[#f3f4f6] pb-4 mb-4">
              <span className="font-medium">Thành tiền</span>
              <span className="font-serif font-bold text-xl text-[#0a0a0a]">
                {formatVnd(subtotal)}
              </span>
            </div>
            {!profile && (
              <div className="mb-4">
                <ErrorMessage message="Vui lòng tạo hồ sơ khách hàng trước." />
              </div>
            )}
            <Button
              variant="gold"
              size="lg"
              className="w-full"
              disabled={!profile || items.length === 0}
              loading={busy}
              onClick={placeOrder}
            >
              Xác nhận đặt hàng
            </Button>
            <p className="text-xs text-[#9ca3af] mt-3 text-center">
              Thanh toán sẽ được xử lý sau khi đơn hàng được xác nhận.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}
