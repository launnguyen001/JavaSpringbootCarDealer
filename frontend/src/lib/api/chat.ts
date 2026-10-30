import { apiRequest } from './http';
import type { ChatRoom, ChatRoomRequest, ChatMessagePage, ChatUser } from '@/types';

/** Lấy danh sách phòng chat của user hiện tại. */
export function getRooms(): Promise<ChatRoom[]> {
  return apiRequest<ChatRoom[]>('/chat/rooms', { auth: true });
}

/** Tìm tối đa 20 tài khoản đang hoạt động theo username. */
export function searchChatUsers(keyword: string): Promise<ChatUser[]> {
  return apiRequest<ChatUser[]>(`/chat/users?keyword=${encodeURIComponent(keyword)}`, {
    auth: true,
  });
}

/** Tạo hoặc lấy phòng chat PRIVATE với user khác. */
export function getOrCreatePrivateRoom(otherUserId: number): Promise<ChatRoom> {
  return apiRequest<ChatRoom>(`/chat/rooms/private/${otherUserId}`, {
    method: 'POST',
    auth: true,
  });
}

/** Tạo phòng chat GROUP. */
export function createGroupRoom(request: ChatRoomRequest): Promise<ChatRoom> {
  return apiRequest<ChatRoom>('/chat/rooms/group', {
    method: 'POST',
    auth: true,
    body: request,
  });
}

/** Lấy lịch sử tin nhắn (phân trang). */
export function getMessages(
  roomId: string,
  page = 0,
  size = 50,
): Promise<ChatMessagePage> {
  return apiRequest<ChatMessagePage>(
    `/chat/rooms/${roomId}/messages?page=${page}&size=${size}`,
    { auth: true },
  );
}
