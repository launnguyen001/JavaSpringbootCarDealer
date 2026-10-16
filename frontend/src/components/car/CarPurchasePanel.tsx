'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import { Field, Input, Textarea } from '@/components/ui/Field';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/context/AuthContext';
import { getErrorMessage } from '@/lib/utils/error';
import { getCustomerId, getSessionId } from '@/lib/auth/token';
import { addItem, createTestDrive } from '@/lib/api';

interface Props {
  carId: number;
  isAvailable: boolean;
  carName: string;
}

export default function CarPurchasePanel({ carId, isAvailable, carName }: Props) {
  const { isAuthenticated } = useAuth();
  const router = useRouter();
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const [testOpen, setTestOpen] = useState(false);

  const addToCart = async () => {
    if (!isAuthenticated) {
      router.push(`/login?next=/cars/${carId}`);
      return;
    }
    setBusy(true);
    try {
      await addItem(getSessionId(), carId, 1);
      toast.success('Đã thêm xe vào giỏ hàng');
      router.push('/cart');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const openTestDrive = () => {
    if (!isAuthenticated) {
      router.push(`/login?next=/cars/${carId}`);
      return;
    }
    if (!getCustomerId()) {
      toast.info('Vui lòng tạo hồ sơ khách hàng trong trang tài khoản trước.');
      router.push('/account');
      return;
    }
    setTestOpen(true);
  };

  return (
    <>
      <Button
        variant="primary"
        size="lg"
        className="w-full uppercase tracking-widest"
        disabled={!isAvailable}
        loading={busy}
        onClick={addToCart}
      >
        {isAvailable ? 'Đặt mua ngay' : 'Đã hết hàng'}
      </Button>
      <button
        type="button"
        onClick={openTestDrive}
        disabled={!isAvailable}
        className="mt-3 w-full py-3 rounded-xl border border-[#e5e7eb] text-[#0a0a0a] font-medium text-sm transition-colors hover:bg-[#f3f4f6] disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {isAvailable ? 'Đăng ký lái thử' : 'Không thể đặt'}
      </button>

      <Modal
        open={testOpen}
        title={`Đăng ký lái thử — ${carName}`}
        onClose={() => setTestOpen(false)}
      >
        <TestDriveForm carId={carId} onDone={() => setTestOpen(false)} />
      </Modal>
    </>
  );
}

function TestDriveForm({ carId, onDone }: { carId: number; onDone: () => void }) {
  const toast = useToast();
  const [scheduledAt, setScheduledAt] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const customerId = getCustomerId();
    if (!customerId) {
      toast.error('Không tìm thấy hồ sơ khách hàng.');
      return;
    }
    setBusy(true);
    try {
      const iso = scheduledAt.length === 16 ? `${scheduledAt}:00` : scheduledAt;
      await createTestDrive({
        scheduledAt: iso,
        status: 'SCHEDULED',
        note: note || null,
        customerId,
        carId,
      });
      toast.success('Đã đăng ký lịch lái thử. Chúng tôi sẽ liên hệ xác nhận.');
      onDone();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={submit} className="space-y-4">
      <Field label="Thời gian mong muốn" required>
        <Input
          type="datetime-local"
          value={scheduledAt}
          onChange={(e) => setScheduledAt(e.target.value)}
          required
        />
      </Field>
      <Field label="Ghi chú">
        <Textarea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="Ví dụ: muốn lái thử vào buổi sáng…"
        />
      </Field>
      <Button type="submit" variant="gold" className="w-full" loading={busy}>
        Gửi đăng ký
      </Button>
    </form>
  );
}
