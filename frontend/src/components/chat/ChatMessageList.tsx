'use client';

import { useEffect, useRef, useState } from 'react';
import type { ChatMessage, ChatReadState } from '@/types';

interface ChatMessageListProps {
  messages: ChatMessage[];
  currentUserId: number;
  loading?: boolean;
  onLoadMore?: () => void;
  hasMore?: boolean;
  readStates?: ChatReadState[];
  typingUsers?: string[];
}

/** Danh sách tin nhắn chat với auto-scroll và load thêm. */
export default function ChatMessageList({
  messages,
  currentUserId,
  loading,
  onLoadMore,
  hasMore,
  readStates = [],
  typingUsers = [],
}: ChatMessageListProps) {
  const bottomRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);

  // Chỉ cuộn container tin nhắn, tránh scrollIntoView làm cuộn cả document.
  useEffect(() => {
    const container = containerRef.current;

    if (autoScroll && container) {
      container.scrollTo({
        top: container.scrollHeight,
        behavior: 'smooth',
      });
    }
  }, [messages, autoScroll]);

  // Detect scroll position để bật/tắt auto-scroll
  function handleScroll() {
    const el = containerRef.current;
    if (!el) return;
    const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 100;
    setAutoScroll(nearBottom);
  }

  return (
    <div
      ref={containerRef}
      onScroll={handleScroll}
      className="flex-1 min-h-0 overflow-y-auto px-4 py-3 space-y-3"
    >
      {/* Nút tải thêm tin nhắn cũ */}
      {hasMore && (
        <div className="text-center">
          <button
            onClick={onLoadMore}
            disabled={loading}
            className="text-sm text-[#c58e00] hover:underline disabled:opacity-50"
          >
            {loading ? 'Đang tải...' : 'Tải tin nhắn cũ hơn'}
          </button>
        </div>
      )}

      {messages.length === 0 && !loading && (
        <div className="text-center text-gray-400 py-8">
          Chưa có tin nhắn nào. Hãy bắt đầu cuộc trò chuyện!
        </div>
      )}

      {messages.map((msg, index) => {
        const isMe = msg.senderId === currentUserId;
        const isSystem = msg.type === 'JOIN' || msg.type === 'LEAVE';
        const isLastOwnMessage = isMe && !messages.slice(index + 1).some(
          (item) => item.senderId === currentUserId && item.type === 'CHAT',
        );
        const hasBeenRead = isLastOwnMessage && readStates.some(
          (state) => state.userId !== currentUserId && state.lastReadAt
            && new Date(state.lastReadAt).getTime() >= new Date(msg.createdAt).getTime(),
        );

        if (isSystem) {
          return (
            <div key={msg.id} className="text-center text-xs text-gray-400 py-1">
              {msg.senderUsername}{' '}
              {msg.type === 'JOIN' ? 'đã tham gia' : 'đã rời khỏi'} phòng chat
            </div>
          );
        }

        return (
          <div
            key={msg.id}
            className={`flex ${isMe ? 'justify-end' : 'justify-start'}`}
          >
            <div
              className={`max-w-[70%] rounded-2xl px-4 py-2 ${
                isMe
                  ? 'bg-[#c58e00] text-white rounded-br-md'
                  : 'bg-gray-100 text-gray-900 rounded-bl-md'
              }`}
            >
              {!isMe && (
                <p className="text-xs font-medium text-[#c58e00] mb-0.5">
                  {msg.senderUsername}
                </p>
              )}
              <p className="text-sm whitespace-pre-wrap break-words">
                {msg.content}
              </p>
              <p
                className={`text-[10px] mt-1 ${
                  isMe ? 'text-white/70' : 'text-gray-400'
                }`}
              >
                {new Date(msg.createdAt).toLocaleTimeString('vi-VN', {
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </p>
              {hasBeenRead && (
                <p className="text-[10px] mt-0.5 text-white/80 text-right">Đã xem</p>
              )}
            </div>
          </div>
        );
      })}

      {typingUsers.length > 0 && (
        <div className="text-xs text-gray-500 italic px-2 py-1">
          {typingUsers.length === 1
            ? typingUsers[0] + ' đang nhập...'
            : typingUsers.join(', ') + ' đang nhập...'}
        </div>
      )}
      <div ref={bottomRef} />
    </div>
  );
}
