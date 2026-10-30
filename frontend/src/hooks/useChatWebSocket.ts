'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from '@/lib/auth/token';
import type { ChatMessage, ChatMessageRequest } from '@/types';

interface UseChatWebSocketOptions {
  /** Phòng chat đang subscribe. */
  roomId: string | null;
  /** userId hiện tại (để subscribe notification channel). */
  userId: number | null;
  /** Callback khi nhận tin nhắn mới trong phòng hiện tại. */
  onMessage?: (message: ChatMessage) => void;
  /** Callback khi nhận notification tin nhắn mới (từ phòng khác). */
  onNotification?: (message: ChatMessage) => void;
}

interface UseChatWebSocketReturn {
  /** Trạng thái kết nối */
  connected: boolean;
  /** Gửi tin nhắn */
  sendMessage: (content: string) => void;
}

/**
 * Hook quản lý kết nối WebSocket STOMP cho chat realtime.
 * Tự động kết nối khi mount, ngắt khi unmount.
 * Subscribe vào /topic/chat/{roomId} và /topic/user/{userId}/chat-notifications.
 */
export function useChatWebSocket({
  roomId,
  userId,
  onMessage,
  onNotification,
}: UseChatWebSocketOptions): UseChatWebSocketReturn {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const onMessageRef = useRef(onMessage);
  const onNotificationRef = useRef(onNotification);

  // Cập nhật callback refs
  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);
  useEffect(() => {
    onNotificationRef.current = onNotification;
  }, [onNotification]);

  useEffect(() => {
    const token = getToken();
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);

        // Subscribe vào phòng chat hiện tại
        if (roomId) {
          client.subscribe(`/topic/chat/${roomId}`, (frame) => {
            const msg = JSON.parse(frame.body) as ChatMessage;
            onMessageRef.current?.(msg);
          });
        }

        // Subscribe vào notification channel của user
        if (userId) {
          client.subscribe(
            `/topic/user/${userId}/chat-notifications`,
            (frame) => {
              const msg = JSON.parse(frame.body) as ChatMessage;
              onNotificationRef.current?.(msg);
            },
          );
        }
      },
      onDisconnect: () => {
        setConnected(false);
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message);
        setConnected(false);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [roomId, userId]);

  const sendMessage = useCallback(
    (content: string) => {
      if (!clientRef.current?.connected || !roomId) return;

      const request: ChatMessageRequest = {
        roomId,
        content,
      };

      clientRef.current.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(request),
      });
    },
    [roomId],
  );

  return { connected, sendMessage };
}
