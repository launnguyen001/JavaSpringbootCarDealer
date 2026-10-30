'use client';

import { useEffect, useRef, useState } from 'react';
import * as chatApi from '@/lib/api/chat';
import type { ChatUser } from '@/types';

interface NewChatDialogProps {
  onClose: () => void;
  onSelectUser: (user: ChatUser) => void;
  startingUserId?: number | null;
}

/** Hộp thoại tìm người dùng và bắt đầu chat riêng. */
export default function NewChatDialog({
  onClose,
  onSelectUser,
  startingUserId = null,
}: NewChatDialogProps) {
  const [keyword, setKeyword] = useState('');
  const [users, setUsers] = useState<ChatUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const requestId = useRef(0);

  useEffect(() => {
    const normalized = keyword.trim();
    const currentRequest = ++requestId.current;

    if (normalized.length < 2) {
      setUsers([]);
      setLoading(false);
      setError(null);
      return;
    }

    const timer = window.setTimeout(() => {
      setLoading(true);
      setError(null);
      chatApi
        .searchChatUsers(normalized)
        .then((result) => {
          if (requestId.current === currentRequest) setUsers(result);
        })
        .catch((err: unknown) => {
          if (requestId.current !== currentRequest) return;
          console.error('Không thể tìm người dùng chat:', err);
          setError('Không thể tìm người dùng. Vui lòng thử lại.');
          setUsers([]);
        })
        .finally(() => {
          if (requestId.current === currentRequest) setLoading(false);
        });
    }, 300);

    return () => window.clearTimeout(timer);
  }, [keyword]);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="new-chat-title"
        className="w-full max-w-md overflow-hidden rounded-2xl bg-white shadow-2xl"
      >
        <header className="flex items-center justify-between border-b border-gray-100 px-5 py-4">
          <div>
            <h2 id="new-chat-title" className="text-lg font-semibold text-gray-900">
              Cuộc trò chuyện mới
            </h2>
            <p className="mt-0.5 text-xs text-gray-500">
              Tìm tài khoản theo tên đăng nhập
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Đóng"
            className="rounded-lg p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-700"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5">
              <path strokeLinecap="round" strokeLinejoin="round" d="m6 6 12 12M18 6 6 18" />
            </svg>
          </button>
        </header>

        <div className="p-5">
          <label htmlFor="chat-user-search" className="sr-only">
            Tìm người dùng
          </label>
          <div className="relative">
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
            >
              <circle cx="11" cy="11" r="7" />
              <path strokeLinecap="round" d="m20 20-4-4" />
            </svg>
            <input
              id="chat-user-search"
              autoFocus
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              placeholder="Nhập username (ít nhất 2 ký tự)"
              className="w-full rounded-xl border border-gray-300 py-3 pl-10 pr-3 text-sm outline-none transition focus:border-[#c58e00] focus:ring-2 focus:ring-[#c58e00]/20"
            />
          </div>

          <div className="mt-4 max-h-72 min-h-24 overflow-y-auto">
            {loading && (
              <div className="py-8 text-center text-sm text-gray-500">
                Đang tìm kiếm…
              </div>
            )}

            {!loading && error && (
              <p role="alert" className="py-8 text-center text-sm text-red-600">
                {error}
              </p>
            )}

            {!loading && !error && keyword.trim().length < 2 && (
              <p className="py-8 text-center text-sm text-gray-400">
                Nhập ít nhất 2 ký tự để tìm kiếm
              </p>
            )}

            {!loading && !error && keyword.trim().length >= 2 && users.length === 0 && (
              <p className="py-8 text-center text-sm text-gray-400">
                Không tìm thấy tài khoản phù hợp
              </p>
            )}

            {!loading && users.map((user) => (
              <button
                type="button"
                key={user.id}
                disabled={startingUserId !== null}
                onClick={() => onSelectUser(user)}
                className="flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left transition hover:bg-gray-50 disabled:cursor-wait disabled:opacity-50"
              >
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#c58e00]/15 text-sm font-semibold uppercase text-[#a87800]">
                  {user.username.charAt(0)}
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium text-gray-900">
                    {user.username}
                  </span>
                  <span className="block text-xs text-gray-500">
                    {user.role === 'ADMIN'
                      ? 'Quản trị viên'
                      : user.role === 'EMPLOYEE'
                        ? 'Nhân viên'
                        : 'Người dùng'}
                  </span>
                </span>
                {startingUserId === user.id ? (
                  <span className="text-xs text-[#a87800]">Đang mở…</span>
                ) : (
                  <span className="text-xs font-medium text-[#a87800]">Nhắn tin</span>
                )}
              </button>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
