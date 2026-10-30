'use client';

import type { ChatRoom } from '@/types';

interface ChatRoomListProps {
  rooms: ChatRoom[];
  selectedRoomId: string | null;
  currentUserId: number;
  onSelectRoom: (room: ChatRoom) => void;
  onNewConversation: () => void;
}

/** Danh sách phòng chat bên trái. */
export default function ChatRoomList({
  rooms,
  selectedRoomId,
  currentUserId,
  onSelectRoom,
  onNewConversation,
}: ChatRoomListProps) {
  function getRoomDisplayName(room: ChatRoom): string {
    if (room.name) return room.name;
    // Phòng PRIVATE: hiển thị tên người kia
    if (room.type === 'PRIVATE') {
      const otherIdx = room.participantIds.findIndex((id) => id !== currentUserId);
      if (otherIdx >= 0 && room.participantUsernames[otherIdx]) {
        return room.participantUsernames[otherIdx];
      }
    }
    return room.participantUsernames.filter((u) => u !== '').join(', ') || 'Phòng chat';
  }

  function formatTime(isoStr: string | null): string {
    if (!isoStr) return '';
    const d = new Date(isoStr);
    const now = new Date();
    const diffMs = now.getTime() - d.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return 'Vừa xong';
    if (diffMin < 60) return `${diffMin} phút`;
    if (diffMin < 1440) return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    return d.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit' });
  }

  return (
    <div className="flex flex-col h-full">
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-lg font-semibold text-gray-900">Tin nhắn</h2>
          <button
            type="button"
            onClick={onNewConversation}
            className="rounded-lg bg-[#c58e00] px-3 py-2 text-xs font-medium text-white transition hover:bg-[#a87800]"
          >
            + Tin nhắn mới
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto">
        {rooms.length === 0 && (
          <div className="text-center text-gray-400 py-8 px-4 text-sm">
            Chưa có cuộc trò chuyện nào
          </div>
        )}

        {rooms.map((room) => {
          const isSelected = room.id === selectedRoomId;
          return (
            <button
              key={room.id}
              onClick={() => onSelectRoom(room)}
              className={`w-full text-left px-4 py-3 border-b border-gray-100 transition-colors
                ${isSelected ? 'bg-[#c58e00]/10 border-l-2 border-l-[#c58e00]' : 'hover:bg-gray-50'}`}
            >
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3 min-w-0">
                  {/* Avatar placeholder */}
                  <div className="w-10 h-10 rounded-full bg-[#c58e00]/20 flex items-center justify-center shrink-0">
                    <span className="text-sm font-semibold text-[#c58e00]">
                      {room.type === 'GROUP' ? (
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                          <path fillRule="evenodd" d="M8.25 6.75a3.75 3.75 0 1 1 7.5 0 3.75 3.75 0 0 1-7.5 0ZM15.75 9.75a3 3 0 1 1 6 0 3 3 0 0 1-6 0ZM2.25 9.75a3 3 0 1 1 6 0 3 3 0 0 1-6 0ZM6.31 15.117A6.745 6.745 0 0 1 12 12a6.745 6.745 0 0 1 6.709 7.498.75.75 0 0 1-.372.568A12.696 12.696 0 0 1 12 21.75c-2.305 0-4.47-.612-6.337-1.684a.75.75 0 0 1-.372-.568 6.787 6.787 0 0 1 1.019-4.38Z" clipRule="evenodd" />
                          <path d="M5.082 14.254a8.287 8.287 0 0 0-1.308 5.135 9.687 9.687 0 0 1-1.764-.44l-.115-.04a.563.563 0 0 1-.373-.487l-.01-.121ZM19.573 15.19a.562.562 0 0 0-.129-.058 9.72 9.72 0 0 0-3.622-.79 8.24 8.24 0 0 1 1.347 4.798 9.724 9.724 0 0 0 2.519-.867.563.563 0 0 0 .29-.52l-.005-.075-.4-2.488Z" />
                        </svg>
                      ) : (
                        getRoomDisplayName(room).charAt(0).toUpperCase()
                      )}
                    </span>
                  </div>

                  <div className="min-w-0">
                    <p className={`text-sm truncate ${room.unreadCount ? "font-bold text-gray-950" : "font-medium text-gray-900"}`}>
                      {getRoomDisplayName(room)}
                    </p>
                    {room.lastMessageContent && (
                      <p className="text-xs text-gray-500 truncate mt-0.5">
                        {room.lastMessageContent}
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex flex-col items-end gap-1 shrink-0 ml-2">
                {room.lastMessageAt && (
                  <span className="text-[10px] text-gray-400 shrink-0 ml-2">
                    {formatTime(room.lastMessageAt)}
                  </span>
                )}
                {!!room.unreadCount && (
                  <span className="min-w-5 h-5 px-1 rounded-full bg-[#c58e00] text-white text-[10px] font-bold flex items-center justify-center">
                    {room.unreadCount > 99 ? '99+' : room.unreadCount}
                  </span>
                )}
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
