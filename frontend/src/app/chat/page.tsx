'use client';

import { useCallback, useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';
import * as chatApi from '@/lib/api/chat';
import { useChatWebSocket } from '@/hooks/useChatWebSocket';
import ChatRoomList from '@/components/chat/ChatRoomList';
import ChatMessageList from '@/components/chat/ChatMessageList';
import ChatInput from '@/components/chat/ChatInput';
import type { ChatRoom, ChatMessage } from '@/types';

export default function ChatPage() {
  const { user, loading: authLoading, isAuthenticated } = useAuth();
  const router = useRouter();

  const [rooms, setRooms] = useState<ChatRoom[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<ChatRoom | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loadingRooms, setLoadingRooms] = useState(true);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  // Redirect nếu chưa đăng nhập
  useEffect(() => {
    if (!authLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [authLoading, isAuthenticated, router]);

  // Load danh sách phòng chat
  useEffect(() => {
    if (!isAuthenticated) return;
    setLoadingRooms(true);
    chatApi
      .getRooms()
      .then(setRooms)
      .catch(console.error)
      .finally(() => setLoadingRooms(false));
  }, [isAuthenticated]);

  // Load tin nhắn khi chọn phòng
  useEffect(() => {
    if (!selectedRoom) {
      setMessages([]);
      return;
    }
    setLoadingMessages(true);
    setPage(0);
    chatApi
      .getMessages(selectedRoom.id, 0)
      .then((res) => {
        // API trả về mới nhất trước, cần đảo lại cho hiển thị
        setMessages([...res.content].reverse());
        setHasMore(!res.last);
      })
      .catch(console.error)
      .finally(() => setLoadingMessages(false));
  }, [selectedRoom]);

  // Tải thêm tin nhắn cũ
  const loadMore = useCallback(() => {
    if (!selectedRoom || loadingMessages) return;
    const nextPage = page + 1;
    setLoadingMessages(true);
    chatApi
      .getMessages(selectedRoom.id, nextPage)
      .then((res) => {
        setMessages((prev) => [...[...res.content].reverse(), ...prev]);
        setHasMore(!res.last);
        setPage(nextPage);
      })
      .catch(console.error)
      .finally(() => setLoadingMessages(false));
  }, [selectedRoom, page, loadingMessages]);

  // Callback khi nhận tin nhắn realtime
  const handleNewMessage = useCallback(
    (msg: ChatMessage) => {
      setMessages((prev) => {
        // Tránh duplicate (nếu tin nhắn do chính mình gửi)
        if (prev.some((m) => m.id === msg.id)) return prev;
        return [...prev, msg];
      });
    },
    [],
  );

  // Callback khi nhận notification từ phòng khác
  const handleNotification = useCallback(
    (msg: ChatMessage) => {
      // Cập nhật preview trong room list
      setRooms((prev) =>
        prev.map((room) =>
          room.id === msg.roomId
            ? { ...room, lastMessageContent: msg.content, lastMessageAt: msg.createdAt }
            : room,
        ),
      );
    },
    [],
  );

  // WebSocket hook
  const { connected, sendMessage } = useChatWebSocket({
    roomId: selectedRoom?.id ?? null,
    userId: user?.id ?? null,
    onMessage: handleNewMessage,
    onNotification: handleNotification,
  });

  function handleSend(content: string) {
    sendMessage(content);
  }

  function handleSelectRoom(room: ChatRoom) {
    setSelectedRoom(room);
  }

  if (authLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin w-8 h-8 border-4 border-[#c58e00] border-t-transparent rounded-full" />
      </div>
    );
  }

  if (!isAuthenticated) return null;

  return (
    <div className="h-[calc(100vh-4rem)] flex bg-white">
      {/* Sidebar: Danh sách phòng chat */}
      <div className="w-80 border-r border-gray-200 flex flex-col shrink-0">
        {loadingRooms ? (
          <div className="flex-1 flex items-center justify-center">
            <div className="animate-spin w-6 h-6 border-2 border-[#c58e00] border-t-transparent rounded-full" />
          </div>
        ) : (
          <ChatRoomList
            rooms={rooms}
            selectedRoomId={selectedRoom?.id ?? null}
            currentUserId={user!.id}
            onSelectRoom={handleSelectRoom}
          />
        )}
      </div>

      {/* Main: Chat area */}
      <div className="flex-1 flex flex-col min-w-0">
        {selectedRoom ? (
          <>
            {/* Header phòng chat */}
            <div className="px-6 py-3 border-b border-gray-200 flex items-center justify-between bg-white">
              <div>
                <h3 className="font-semibold text-gray-900">
                  {selectedRoom.name ||
                    selectedRoom.participantUsernames
                      .filter((u) => u !== user?.username)
                      .join(', ') ||
                    'Chat'}
                </h3>
                <p className="text-xs text-gray-400">
                  {connected ? (
                    <span className="flex items-center gap-1">
                      <span className="w-2 h-2 rounded-full bg-green-500 inline-block" />
                      Đang kết nối
                    </span>
                  ) : (
                    <span className="flex items-center gap-1">
                      <span className="w-2 h-2 rounded-full bg-red-400 inline-block" />
                      Đang kết nối lại...
                    </span>
                  )}
                </p>
              </div>
              <div className="text-xs text-gray-400">
                {selectedRoom.participantUsernames.length} thành viên
              </div>
            </div>

            {/* Danh sách tin nhắn */}
            <ChatMessageList
              messages={messages}
              currentUserId={user!.id}
              loading={loadingMessages}
              onLoadMore={loadMore}
              hasMore={hasMore}
            />

            {/* Input gửi tin nhắn */}
            <ChatInput onSend={handleSend} disabled={!connected} />
          </>
        ) : (
          /* Empty state */
          <div className="flex-1 flex flex-col items-center justify-center text-gray-400">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1}
              stroke="currentColor"
              className="w-16 h-16 mb-4 text-gray-300"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z"
              />
            </svg>
            <p className="text-lg font-medium">Chọn cuộc trò chuyện</p>
            <p className="text-sm mt-1">Chọn một phòng chat bên trái để bắt đầu</p>
          </div>
        )}
      </div>
    </div>
  );
}
