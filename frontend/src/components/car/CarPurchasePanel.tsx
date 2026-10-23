'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import { Field, Input, Select, Textarea } from '@/components/ui/Field';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/context/AuthContext';
import { useSelectedItem } from '@/components/car/SelectedItemContext';
import { getErrorMessage } from '@/lib/utils/error';
import { formatVnd } from '@/lib/utils/format';
import { getSessionId, setCustomerId } from '@/lib/auth/token';
import { addItem, createTestDrive, getMyProfile } from '@/lib/api';

interface Props {
  carId: number;
  carName: string;
}

const CONDITION_LABEL: Record<string, string> = { NEW: 'Mới', USED: 'Đã dùng' };

/** Dropdown chọn chiếc xe cụ thể (đặt đầu card). */
export function ItemPicker() {
  const { available, selected, setSelectedId } = useSelectedItem();
  if (available.length === 0) return null;

  return (
    <Field label="Chọn xe cụ thể">
      <Select value={selected?.id ?? ''} onChange={(e) => setSelectedId(Number(e.target.value))}>
        {available.map((item) => (
          <option key={item.id} value={item.id}>
            {item.vin ?? `#${item.id}`} • {CONDITION_LABEL[item.condition] ?? item.condition}
            {item.color ? ` • ${item.color}` : ''} • {formatVnd(item.price)}
          </option>
        ))}
      </Select>
    </Field>
  );
}

/** Nút đặt mua + đăng ký lái thử (đặt cuối card, dưới tổng tiền). */
export function PurchaseActions({ carId, carName }: Props) {
  const { isAuthenticated } = useAuth();
  const router = useRouter();
  const toast = useToast();
  const [busy, setBusy] = useState(false);
  const [testOpen, setTestOpen] = useState(false);
  const [profileId, setProfileId] = useState<number | null>(null);

  // Lựa chọn chiếc xe được chia sẻ với khối giá/thông số ở trang chi tiết.
  const { available, selected } = useSelectedItem();
  const hasItems = available.length > 0;

  const addToCart = async () => {
    if (!isAuthenticated) {
      router.push(`/login?next=/cars/${carId}`);
      return;
    }
    if (!selected) return;
    setBusy(true);
    try {
      await addItem(getSessionId(), selected.id, 1);
      toast.success('Đã thêm xe vào giỏ hàng');
      router.push('/cart');
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const openTestDrive = async () => {
    if (!isAuthenticated) {
      router.push(`/login?next=/cars/${carId}`);
      return;
    }
    // Hồ sơ khách hàng gắn với tài khoản — kiểm tra qua /me thay vì chỉ dựa localStorage.
    try {
      const profile = await getMyProfile();
      setCustomerId(profile.id);
      setProfileId(profile.id);
      setTestOpen(true);
    } catch {
      toast.info('Vui lòng tạo hồ sơ khách hàng trong trang tài khoản trước.');
      router.push('/account');
    }
  };

  return (
    <>
      <Button
        variant="primary"
        size="lg"
        className="w-full uppercase tracking-widest"
        disabled={!hasItems}
        loading={busy}
        onClick={addToCart}
      >
        {hasItems ? 'Đặt mua ngay' : 'Đã hết hàng'}
      </Button>
      <button
        type="button"
        onClick={openTestDrive}
        disabled={!hasItems}
        className="mt-3 w-full py-3 rounded-xl border border-[#e5e7eb] text-[#0a0a0a] font-medium text-sm transition-colors hover:bg-[#f3f4f6] disabled:opacity-50 disabled:cursor-not-allowed"
      >
        {hasItems ? 'Đăng ký lái thử' : 'Không thể đặt'}
      </button>

      <Modal
        open={testOpen}
        title={`Đăng ký lái thử — ${carName}`}
        onClose={() => setTestOpen(false)}
      >
        {profileId && (
          <TestDriveForm
            carItemId={selected?.id ?? null}
            customerId={profileId}
            carName={carName}
            onDone={() => setTestOpen(false)}
          />
        )}
      </Modal>
    </>
  );
}

/** Giữ lại để tương thích ngược với chỗ nào còn import default. */
export default function CarPurchasePanel(props: Props) {
  return (
    <>
      <div className="mb-4">
        <ItemPicker />
      </div>
      <PurchaseActions {...props} />
    </>
  );
}

function TestDriveForm({
  carItemId,
  customerId,
  carName,
  onDone,
}: {
  carItemId: number | null;
  customerId: number;
  carName: string;
  onDone: () => void;
}) {
  const toast = useToast();
  const [scheduledAt, setScheduledAt] = useState('');
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!carItemId) {
      toast.error('Vui lòng chọn một chiếc xe cụ thể.');
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
        carItemId,
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
      <p className="text-sm text-[#6b7280]">
        Xe đã chọn: <b className="text-[#0a0a0a]">{carName}</b>
      </p>
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