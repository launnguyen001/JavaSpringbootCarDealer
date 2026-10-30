/** Loại phòng chat */
export type ChatRoomType = 'PRIVATE' | 'GROUP';

/** Loại tin nhắn */
export type ChatMessageType = 'CHAT' | 'JOIN' | 'LEAVE';

/** Phòng chat */
export interface ChatRoom {
  id: string;
  name: string | null;
  type: ChatRoomType;
  participantIds: number[];
  participantUsernames: string[];
  createdBy: number;
  createdAt: string;
  lastMessageContent: string | null;
  lastMessageAt: string | null;
  unreadCount?: number;
}

/** Tin nhắn chat */
export interface ChatMessage {
  id: string;
  roomId: string;
  senderId: number;
  senderUsername: string;
  content: string;
  type: ChatMessageType;
  createdAt: string;
}

/** Request gửi tin nhắn qua WebSocket */
export interface ChatMessageRequest {
  roomId: string;
  content: string;
}

/** Request tạo phòng chat nhóm */
export interface ChatRoomRequest {
  name: string;
  participantIds: number[];
}

/** Page response cho tin nhắn */
export interface ChatMessagePage {
  content: ChatMessage[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

/** Người dùng có thể bắt đầu cuộc trò chuyện mới. */
export interface ChatUser {
  id: number;
  username: string;
  role: 'ADMIN' | 'EMPLOYEE' | 'USER';
}


/** Event realtime tách biệt với ChatMessage. */
export interface ChatRealtimeEvent {
  type: 'TYPING' | 'READ';
  roomId: string;
  userId: number;
  username: string;
  typing: boolean | null;
  lastReadMessageId: string | null;
  lastReadAt: string | null;
}

export interface ChatReadState {
  userId: number;
  lastReadMessageId: string | null;
  lastReadAt: string | null;
}

/** Presence event từ WebSocket. */
export interface ChatPresenceEvent { userId: number; online: boolean; }
