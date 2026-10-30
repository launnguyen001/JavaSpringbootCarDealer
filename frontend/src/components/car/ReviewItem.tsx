'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Button from '@/components/ui/Button';
import RatingStars from '@/components/ui/RatingStars';
import { useToast } from '@/components/ui/Toast';
import { useAuth } from '@/context/AuthContext';
import { replyToReview } from '@/lib/api';
import { formatDate, formatDateTime } from '@/lib/utils/format';
import { getErrorMessage } from '@/lib/utils/error';
import type { CarReview, Reply } from '@/types';

interface ReviewItemProps {
  review: CarReview;
  carId: number;
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

export default function ReviewItem({ review, carId }: ReviewItemProps) {
  const router = useRouter();
  const toast = useToast();
  const { user, isAuthenticated, isStaff, isAdmin } = useAuth();

  const [replies, setReplies] = useState<Reply[]>(review.replies ?? []);
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleStartReply = () => {
    if (!isAuthenticated) {
      toast.info('Vui lòng đăng nhập để phản hồi đánh giá.');
      router.push(`/login?next=/cars/${carId}`);
      return;
    }
    setShowReplyForm((prev) => !prev);
  };

  const handleSubmitReply = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) {
      toast.error('Vui lòng nhập nội dung phản hồi.');
      return;
    }

    // Xác định tên hiển thị: nếu là staff/admin thì gắn kèm nhãn Đại lý
    let author = user?.username ?? 'Khách hàng';
    if ((isStaff || isAdmin) && !isDealerAuthor(author)) {
      author = `${author} (Đại lý)`;
    }

    setSubmitting(true);
    try {
      const updated = await replyToReview(review.id, author, content.trim());
      setReplies(updated.replies ?? []);
      setContent('');
      setShowReplyForm(false);
      toast.success('Đã gửi phản hồi thành công!');
      router.refresh();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="border-b border-[#e5e7eb] pb-6 last:border-0">
      {/* Thông tin khách hàng & số sao */}
      <div className="flex items-start gap-3 mb-2">
        <div className="w-10 h-10 rounded-full bg-[#f3f4f6] flex items-center justify-center flex-shrink-0">
          <span className="text-xs font-semibold text-[#0a0a0a]">
            {review.customerName ? review.customerName.charAt(0).toUpperCase() : '?'}
          </span>
        </div>
        <div>
          <p className="text-sm font-medium text-[#0a0a0a]">{review.customerName}</p>
          <div className="flex items-center gap-1 mt-0.5">
            <RatingStars rating={review.rating} size={14} />
            <span className="text-xs text-[#6b7280]">{formatDate(review.createdAt)}</span>
          </div>
        </div>
      </div>

      {/* Tiêu đề & nội dung đánh giá */}
      {review.title && (
        <h4 className="text-sm font-semibold text-[#0a0a0a] mb-1">{review.title}</h4>
      )}
      <p className="text-[#4b5563] text-sm leading-relaxed whitespace-pre-wrap">
        {review.content}
      </p>

      {/* Danh sách phản hồi cấp 1 */}
      {replies.length > 0 && (
        <div className="mt-4 pl-4 sm:pl-6 border-l-2 border-[#e5e7eb] space-y-3">
          {replies.map((reply, idx) => (
            <div
              key={reply.createdAt || idx}
              className="bg-[#fafafa] rounded-xl p-3 border border-[#f3f4f6] space-y-1"
            >
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-semibold text-[#0a0a0a]">
                    {reply.author}
                  </span>
                  {isDealerAuthor(reply.author) && (
                    <span className="inline-flex items-center px-1.5 py-0.2 rounded text-[10px] font-bold bg-[#0a0a0a] text-[#c58e00]">
                      Đại lý
                    </span>
                  )}
                </div>
                <span className="text-[11px] text-[#9ca3af]">
                  {formatDateTime(reply.createdAt)}
                </span>
              </div>
              <p className="text-[#4b5563] text-sm leading-relaxed whitespace-pre-wrap">
                {reply.content}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Nút kích hoạt mở khung Trả lời */}
      <div className="mt-3 flex items-center gap-3">
        <button
          type="button"
          onClick={handleStartReply}
          className="inline-flex items-center gap-1.5 text-xs font-medium text-[#6b7280] hover:text-[#c58e00] transition-colors"
        >
          <svg
            className="w-3.5 h-3.5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          Trả lời {replies.length > 0 ? `(${replies.length})` : ''}
        </button>
      </div>

      {/* Form trả lời inline */}
      {showReplyForm && (
        <form
          onSubmit={handleSubmitReply}
          className="mt-3 pl-4 sm:pl-6 border-l-2 border-[#c58e00] space-y-3"
        >
          <div className="rounded-xl border border-[#e5e7eb] bg-[#fafafa] p-3.5 space-y-2.5">
            <div className="text-xs text-[#6b7280]">
              Trả lời với tư cách:{' '}
              <strong className="text-[#0a0a0a]">
                {user?.username}
                {isStaff || isAdmin ? ' (Đại lý)' : ''}
              </strong>
            </div>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Viết câu trả lời của bạn..."
              rows={2}
              required
              className="w-full text-sm rounded-lg border border-[#e5e7eb] p-2.5 bg-white text-[#0a0a0a] focus:outline-none focus:border-[#c58e00] transition-colors resize-none placeholder:text-[#9ca3af]"
            />
            <div className="flex justify-end gap-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => {
                  setShowReplyForm(false);
                  setContent('');
                }}
                disabled={submitting}
              >
                Hủy
              </Button>
              <Button type="submit" variant="gold" size="sm" loading={submitting}>
                Gửi phản hồi
              </Button>
            </div>
          </div>
        </form>
      )}
    </div>
  );
}
