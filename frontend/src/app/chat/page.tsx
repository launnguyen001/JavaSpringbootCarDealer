'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';
import * as chatApi from '@/lib/api/chat';
import { useChatWebSocket } from '@/hooks/useChatWebSocket';
import ChatRoomList from '@/components/chat/ChatRoomList';
import ChatMessageList from '@/components/chat/ChatMessageList';
import ChatInput from '@/components/chat/ChatInput';
import NewChatDialog from '@/components/chat/NewChatDialog';
import type { ChatRoom, ChatMessage, ChatUser } from '@/types';

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
  const [error, setError] = useState<string | null>(null);
  const [showRoomList, setShowRoomList] = useState(true);
  const [showNewChat, setShowNewChat] = useState(false);
  const [startingUserId, setStartingUserId] = useState<number | null>(null);
  const messageRequestId = useRef(0);

  useEffect(() => {
    if (!authLoading && !isAuthenticated) router.replace('/login');
  }, [authLoading, isAuthenticated, router]);

  useEffect(() => {
    if (!isAuthenticated) return;

    let active = true;
    setLoadingRooms(true);
    chatApi
      .getRooms()
      .then((data) => {
        if (active) setRooms(data);
      })
      .catch((err: unknown) => {
        if (active) {
          console.error('Không thể tải danh sách phòng chat:', err);
          setError('Không thể tải danh sách cuộc trò chuyện. Vui lòng thử tải lại.');
        }
      })
      .finally(() => {
        if (active) setLoadingRooms(false);
      });

    return () => {
      active = false;
    };
  }, [isAuthenticated]);

  // Mỗi lần đổi phòng tạo một request generation mới để response cũ
  // không ghi đè tin nhắn của phòng vừa chọn.
  useEffect(() => {
    const requestId = ++messageRequestId.current;

    if (!selectedRoom) {
      setMessages([]);
      setHasMore(false);
      setPage(0);
      setLoadingMessages(false);
      return;
    }

    setMessages([]);
    setPage(0);
    setHasMore(false);
    setError(null);
    setLoadingMessages(true);

    chatApi
      .getMessages(selectedRoom.id, 0)
      .then((res) => {
        if (messageRequestId.current !== requestId) return;
        // Backend trả newest-first; UI hiển thị oldest-first.
        setMessages([...res.content].reverse());
        setHasMore(!res.last);
      })
      .catch((err: unknown) => {
        if (messageRequestId.current !== requestId) return;
        console.error('Không thể tải lịch sử chat:', err);
        setError('Không thể tải lịch sử tin nhắn. Vui lòng thử lại.');
      })
      .finally(() => {
        if (messageRequestId.current === requestId) setLoadingMessages(false);
      });

    return () => {
      // Invalidate response nếu người dùng chuyển phòng trước khi request hoàn tất.
      if (messageRequestId.current === requestId) messageRequestId.current += 1;
    };
  }, [selectedRoom]);

  const loadMore = useCallback(() => {
    if (!selectedRoom || loadingMessages || !hasMore) return;

    const requestId = messageRequestId.current;
    const nextPage = page + 1;
    setLoadingMessages(true);

    chatApi
      .getMessages(selectedRoom.id, nextPage)
      .then((res) => {
        if (messageRequestId.current !== requestId) return;
        setMessages((prev) => [...[...res.content].reverse(), ...prev]);
        setHasMore(!res.last);
        setPage(nextPage);
      })
      .catch((err: unknown) => {
        if (messageRequestId.current !== requestId) return;
        console.error('Không thể tải thêm tin nhắn:', err);
        setError('Không thể tải thêm tin nhắn cũ.');
      })
      .finally(() => {
        if (messageRequestId.current === requestId) setLoadingMessages(false);
      });
  }, [selectedRoom, page, loadingMessages, hasMore]);

  const handleNewMessage = useCallback((msg: ChatMessage) => {
    setMessages((prev) => {
      if (prev.some((item) => item.id === msg.id)) return prev;
      return [...prev, msg];
    });
    setRooms((prev) =>
      prev
        .map((room) =>
          room.id === msg.roomId
            ? {
                ...room,
                lastMessageContent: msg.content,
                lastMessageAt: msg.createdAt,
              }
            : room,
        )
        .sort((a, b) => {
          const aTime = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0;
          const bTime = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0;
          return bTime - aTime;
        }),
    );
  }, []);

  const handleNotification = useCallback((msg: ChatMessage) => {
    setRooms((prev) =>
      prev
        .map((room) =>
          room.id === msg.roomId
            ? {
                ...room,
                lastMessageContent: msg.content,
                lastMessageAt: msg.createdAt,
              }
            : room,
        )
        .sort((a, b) => {
          const aTime = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0;
          const bTime = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0;
          return bTime - aTime;
        }),
    );

    // Người nhận có thể chưa có phòng này trong danh sách (tin nhắn đầu tiên).
    // Đồng bộ lại danh sách từ REST để phòng mới xuất hiện mà không cần reload.
    void chatApi.getRooms().then(setRooms).catch((err: unknown) => {
      console.error('Không thể đồng bộ danh sách phòng chat:', err);
    });
  }, []);

  const handleRealtimeEvent = useCallback((event: ChatRealtimeEvent) => {
    if (event.type === 'READ') {
      setReadStates((prev) => [
        ...prev.filter((state) => state.userId !== event.userId),
        { userId: event.userId, lastReadMessageId: event.lastReadMessageId, lastReadAt: event.lastReadAt },
      ]);
      return;
    }
    if (event.type === 'TYPING' && event.userId !== user?.id) {
      const oldTimer = typingTimers.current[event.userId];
      if (oldTimer) clearTimeout(oldTimer);
      if (event.typing) {
        setTypingUsers((prev) => ({ ...prev, [event.userId]: event.username }));
        typingTimers.current[event.userId] = setTimeout(() => {
          setTypingUsers((prev) => {
            const next = { ...prev };
            delete next[event.userId];
            return next;
          });
        }, 2500);
      } else {
        setTypingUsers((prev) => {
          const next = { ...prev };
          delete next[event.userId];
          return next;
        });
      }
    }
  }, [user?.id]);

  const { connected, sendMessage, sendTyping, markRead } = useChatWebSocket({
    roomId: selectedRoom?.id ?? null,
    userId: user?.id ?? null,
    onMessage: handleNewMessage,
    onNotification: handleNotification,
    onEvent: handleRealtimeEvent,
  });

  useEffect(() => {
    const latest = messages[messages.length - 1];
    if (!selectedRoom || !latest || !connected || document.visibilityState !== 'visible') return;
    const key = selectedRoom.id + ':' + latest.id;
    if (lastMarkedRef.current === key) return;
    lastMarkedRef.current = key;
    markRead(latest.id);
    setRooms((prev) => prev.map((room) => room.id === selectedRoom.id ? { ...room, unreadCount: 0 } : room));
  }, [messages, selectedRoom, connected, markRead]);

  useEffect(() => {
    const cleanup = () => Object.values(typingTimers.current).forEach(clearTimeout);
    return cleanup;
  }, []);

  function handleSend(content: string) {
    const sent = sendMessage(content);
    if (!sent) {
      setError('Tin nhắn chưa được gửi vì kết nối bị gián đoạn. Anh thử gửi lại nhé.');
    } else {
      setError(null);
    }
  }

  function handleSelectRoom(room: ChatRoom) {
    setSelectedRoom(room);
    setShowRoomList(false);
  }

  async function handleStartConversation(target: ChatUser) {
    setStartingUserId(target.id);
    setError(null);
    try {
      const room = await chatApi.getOrCreatePrivateRoom(target.id);
      setRooms((prev) => [room, ...prev.filter((item) => item.id !== room.id)]);
      setSelectedRoom(room);
      setShowNewChat(false);
      setShowRoomList(false);
    } catch (err) {
      console.error('Không thể mở cuộc trò chuyện:', err);
      setError('Không thể tạo cuộc trò chuyện. Vui lòng thử lại.');
    } finally {
      setStartingUserId(null);
    }
  }

  if (authLoading) {
    return (
      <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center">
        <div className="animate-spin w-8 h-8 border-4 border-[#c58e00] border-t-transparent rounded-full" />
      </div>
    );
  }

  if (!isAuthenticated) return null;

  return (
    <div className="h-[calc(100dvh-4rem)] min-h-[420px] flex overflow-hidden bg-white">
      {/* Danh sách phòng: ẩn trên mobile khi đang mở một cuộc trò chuyện. */}
      <aside
        className={`w-full md:w-80 md:flex border-r border-gray-200 flex-col shrink-0 ${
          showRoomList ? 'flex' : 'hidden'
        }`}
      >
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
            onNewConversation={() => setShowNewChat(true)}
          />
        )}
      </aside>

      {/* Khu vực hội thoại */}
      <main className={`flex-1 min-w-0 flex-col ${showRoomList ? 'hidden md:flex' : 'flex'}`}>
        {selectedRoom ? (
          <>
            <header className="px-3 sm:px-6 py-3 border-b border-gray-200 flex items-center justify-between bg-white shrink-0">
              <div className="flex items-center gap-3 min-w-0">
                <button
                  type="button"
                  onClick={() => setShowRoomList(true)}
                  className="md:hidden p-2 -ml-2 rounded-lg text-gray-600 hover:bg-gray-100"
                  aria-label="Quay lại danh sách cuộc trò chuyện"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="w-5 h-5">
                    <path strokeLinecap="round" strokeLinejoin="round" d="m15 18-6-6 6-6" />
                  </svg>
                </button>
                <div className="min-w-0">
                  <h3 className="font-semibold text-gray-900 truncate">
                    {selectedRoom.name ||
                      selectedRoom.participantUsernames
                        .filter((name) => name !== user?.username)
                        .join(', ') ||
                      'Chat'}
                  </h3>
                  <p className="text-xs text-gray-400 flex items-center gap-1">
                    <span className={`w-2 h-2 rounded-full inline-block ${connected ? 'bg-green-500' : 'bg-amber-500'}`} />
                    {connected ? 'Đang kết nối' : 'Đang kết nối lại…'}
                  </p>
                </div>
              </div>
              <div className="text-xs text-gray-400 shrink-0 ml-2">
                {selectedRoom.participantUsernames.length} thành viên
              </div>
            </header>

            {error && (
              <div role="alert" className="px-4 py-2 text-sm text-red-700 bg-red-50 border-b border-red-100">
                {error}
                <button type="button" onClick={() => setError(null)} className="ml-3 underline">
                  Đóng
                </button>
              </div>
            )}

            <ChatMessageList
              messages={messages}
              currentUserId={user!.id}
              loading={loadingMessages}
              onLoadMore={loadMore}
              hasMore={hasMore}
              readStates={readStates}
              typingUsers={Object.values(typingUsers)}
            />
            <ChatInput onSend={handleSend} onTyping={sendTyping} disabled={!connected} />
          </>
        ) : (
          <div className="flex-1 flex flex-col items-center justify-center text-gray-400 px-6 text-center">
            {error && (
              <p role="alert" className="mb-4 rounded-lg bg-red-50 px-4 py-2 text-sm text-red-700">
                {error}
              </p>
            )}
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
      </main>
      {showNewChat && (
        <NewChatDialog
          onClose={() => setShowNewChat(false)}
          onSelectUser={handleStartConversation}
          startingUserId={startingUserId}
        />
      )}
    </div>
  );
}
