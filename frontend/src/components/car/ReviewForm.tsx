'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Button from '@/components/ui/Button';
import { Field, Input, Textarea } from '@/components/ui/Field';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/context/AuthContext';
import { getErrorMessage } from '@/lib/utils/error';
import { getCustomerId } from '@/lib/auth/token';
import { createCarReview } from '@/lib/api';

export default function ReviewForm({ carId }: { carId: number }) {
  const { user, isAuthenticated } = useAuth();
  const router = useRouter();
  const toast = useToast();

  const [open, setOpen] = useState(false);
  const [rating, setRating] = useState(5);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [busy, setBusy] = useState(false);

  const start = () => {
    if (!isAuthenticated) {
      router.push(`/login?next=/cars/${carId}`);
      return;
    }
    if (!getCustomerId()) {
      toast.info('Vui lòng tạo hồ sơ khách hàng trong trang tài khoản trước.');
      router.push('/account');
      return;
    }
    setOpen(true);
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    const customerId = getCustomerId();
    if (!customerId) return;
    setBusy(true);
    try {
      await createCarReview({
        carId,
        customerId,
        customerName: user?.username ?? 'Khách hàng',
        rating,
        title: title || null,
        content,
      });
      toast.success('Cảm ơn bạn đã đánh giá!');
      setOpen(false);
      setTitle('');
      setContent('');
      setRating(5);
      router.refresh();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  if (!open) {
    return (
      <Button variant="gold" size="sm" onClick={start}>
        Viết đánh giá
      </Button>
    );
  }

  return (
    <form
      onSubmit={submit}
      className="w-full rounded-2xl border border-[#e5e7eb] bg-[#fafafa] p-5 space-y-4"
    >
      <Field label="Điểm đánh giá" required>
        <div className="flex gap-2">
          {[1, 2, 3, 4, 5].map((n) => (
            <button
              key={n}
              type="button"
              onClick={() => setRating(n)}
              className={`w-9 h-9 rounded-lg border font-medium transition-colors ${
                n <= rating
                  ? 'bg-[#f59e0b] text-white border-[#f59e0b]'
                  : 'bg-white text-[#6b7280] border-[#e5e7eb]'
              }`}
            >
              {n}
            </button>
          ))}
        </div>
      </Field>
      <Field label="Tiêu đề">
        <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Rất hài lòng…" />
      </Field>
      <Field label="Nội dung" required>
        <Textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          required
          placeholder="Chia sẻ trải nghiệm của bạn về xe này"
        />
      </Field>
      <div className="flex gap-3">
        <Button type="submit" variant="gold" loading={busy}>
          Gửi đánh giá
        </Button>
        <Button type="button" variant="outline" onClick={() => setOpen(false)}>
          Hủy
        </Button>
      </div>
    </form>
  );
}
