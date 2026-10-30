'use client';

import { useEffect, useState } from 'react';
import Modal from '@/components/ui/Modal';
import Button from '@/components/ui/Button';
import { Field, Input, Textarea } from '@/components/ui/Field';
import RatingStars from '@/components/ui/RatingStars';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/context/AuthContext';
import { replyToReview } from '@/lib/api';
import { formatDateTime } from '@/lib/utils/format';
import { getErrorMessage } from '@/lib/utils/error';
import type { CarReview, Reply } from '@/types';

interface ReviewReplyModalProps {
  open: boolean;
  review: CarReview | null;
  onClose: () => void;
  onSuccess: (updatedReview: CarReview) => void;
}

function isDealerAuthor(author: string): boolean {
  if (!author) return false;
  const lower = author.toLowerCase();
  return (
    lower.includes('đại lý') ||
    lower.includes('dai ly') ||
    lower.includes('admin') ||
    lower.includes('quản trị') ||
    lower.includes('quan tri') ||
    lower.includes('nhân viên') ||
    lower.includes('nhan vien')
  );
}

export default function ReviewReplyModal({
  open,
  review,
  onClose,
  onSuccess,
}: ReviewReplyModalProps) {
  const { user } = useAuth();
  const toast = useToast();

  const [author, setAuthor] = useState('Ban Quản Trị Car Dealer');
  const [content, setContent] = useState('');
  const [busy, setBusy] = useState(false);
  const [replies, setReplies] = useState<Reply[]>([]);

  useEffect(() => {
    if (review) {
      setReplies(review.replies ?? []);
      setContent('');
      const defaultAuthor = user?.username
        ? `${user.username} (Đại lý)`
        : 'Ban Quản Trị Car Dealer';
      setAuthor(defaultAuthor);
    }
  }, [review, user]);

  if (!review) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) {
      toast.error('Vui lòng nhập nội dung phản hồi');
      return;
    }
    const finalAuthor = author.trim() || 'Ban Quản Trị Car Dealer';
    setBusy(true);
    try {
      const updated = await replyToReview(review.id, finalAuthor, content.trim());
      toast.success('Đã gửi phản hồi thành công!');
      setReplies(updated.replies ?? []);
      setContent('');
      onSuccess(updated);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      open={open}
      title="Phản hồi đánh giá"
      size="lg"
      onClose={onClose}
      footer={
        <div className="flex justify-end gap-2 w-full">
          <Button variant="outline" onClick={onClose} disabled={busy}>
            Đóng
          </Button>
          <Button variant="gold" onClick={handleSubmit} loading={busy}>
            Gửi phản hồi
          </Button>
        </div>
      }
    >
      <div className="space-y-6">
        {/* Thông tin đánh giá gốc */}
        <div className="rounded-xl border border-[#e5e7eb] bg-[#fafafa] p-4">
          <div className="flex items-start justify-between gap-3 mb-2">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-semibold text-sm text-[#0a0a0a]">
                  {review.customerName}
                </span>
                <span className="text-xs text-[#6b7280]">(ID: {review.customerId})</span>
              </div>
              <div className="flex items-center gap-2 mt-1">
                <RatingStars rating={review.rating} size={14} />
                <span className="text-xs text-[#9ca3af]">
                  {formatDateTime(review.createdAt)}
                </span>
              </div>
            </div>
            <span className="px-2.5 py-1 rounded-full text-xs font-medium bg-white border border-[#e5e7eb] text-[#374151]">
              Xe: {review.carName ?? `#${review.carId}`}
            </span>
          </div>

          {review.title && (
            <h4 className="text-sm font-semibold text-[#0a0a0a] mt-2 mb-1">
              {review.title}
            </h4>
          )}
          <p className="text-sm text-[#4b5563] leading-relaxed whitespace-pre-wrap">
            {review.content}
          </p>
        </div>

        {/* Danh sách phản hồi đã có */}
        <div>
          <h4 className="text-xs font-semibold uppercase tracking-wider text-[#6b7280] mb-3">
            Lịch sử phản hồi ({replies.length})
          </h4>
          {replies.length === 0 ? (
            <div className="text-sm text-[#9ca3af] italic py-3 text-center rounded-lg border border-dashed border-[#e5e7eb]">
              Chưa có phản hồi nào cho đánh giá này.
            </div>
          ) : (
            <div className="space-y-3 max-h-56 overflow-y-auto pr-1">
              {replies.map((reply, idx) => (
                <div
                  key={reply.createdAt || idx}
                  className="rounded-lg border border-[#e5e7eb] bg-white p-3 space-y-1"
                >
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-1.5">
                      <span className="text-xs font-semibold text-[#0a0a0a]">
                        {reply.author}
                      </span>
                      {isDealerAuthor(reply.author) && (
                        <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-bold bg-[#0a0a0a] text-[#c58e00]">
                          Đại lý
                        </span>
                      )}
                    </div>
                    <span className="text-[11px] text-[#9ca3af]">
                      {formatDateTime(reply.createdAt)}
                    </span>
                  </div>
                  <p className="text-xs text-[#4b5563] leading-relaxed whitespace-pre-wrap">
                    {reply.content}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Form thêm phản hồi mới */}
        <form onSubmit={handleSubmit} className="border-t border-[#e5e7eb] pt-4 space-y-3">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-[#6b7280]">
            Thêm phản hồi của đại lý
          </h4>
          <Field label="Tên người phản hồi" required>
            <Input
              value={author}
              onChange={(e) => setAuthor(e.target.value)}
              placeholder="Ban Quản Trị Car Dealer"
              required
            />
          </Field>
          <Field label="Nội dung phản hồi" required>
            <Textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Nhập nội dung giải đáp hoặc cảm ơn khách hàng..."
              rows={3}
              required
            />
          </Field>
        </form>
      </div>
    </Modal>
  );
}
