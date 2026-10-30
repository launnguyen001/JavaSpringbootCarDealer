'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from '@/lib/auth/token';
import type { ChatMessage, ChatMessageRequest } from '@/types';

interface UseChatWebSocketOptions {
  /** Phòng chat đang mở. */
  roomId: string | null;
  /** ID user hiện tại, dùng cho kênh notification cá nhân. */
  userId: number | null;
  onMessage?: (message: ChatMessage) => void;
  onNotification?: (message: ChatMessage) => void;
}

interface UseChatWebSocketReturn {
  connected: boolean;
  sendMessage: (content: string) => boolean;
}

/**
 * Quản lý một kết nối STOMP theo phiên đăng nhập.
 * Việc đổi phòng chỉ unsubscribe/subscribe topic, không tạo lại WebSocket.
 */
export function useChatWebSocket({
  roomId,
  userId,
  onMessage,
  onNotification,
}: UseChatWebSocketOptions): UseChatWebSocketReturn {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const roomSubscriptionRef = useRef<{ unsubscribe: () => void } | null>(null);
  const notificationSubscriptionRef = useRef<{ unsubscribe: () => void } | null>(null);
  const onMessageRef = useRef(onMessage);
  const onNotificationRef = useRef(onNotification);

  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    onNotificationRef.current = onNotification;
  }, [onNotification]);

  // Chỉ tạo kết nối khi user đăng nhập / đổi tài khoản.
  useEffect(() => {
    const token = getToken();
    if (!token || userId === null) {
      setConnected(false);
      return;
    }

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
        notificationSubscriptionRef.current?.unsubscribe();
        notificationSubscriptionRef.current = client.subscribe(
          `/topic/user/${userId}/chat-notifications`,
          (frame) => {
            try {
              onNotificationRef.current?.(JSON.parse(frame.body) as ChatMessage);
            } catch (error) {
              console.error('Không thể đọc notification chat:', error);
            }
          },
        );
      },
      onDisconnect: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers?.message ?? frame.body);
        setConnected(false);
      },
    });

    clientRef.current = client;
    client.activate();

    return () => {
      roomSubscriptionRef.current?.unsubscribe();
      notificationSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = null;
      notificationSubscriptionRef.current = null;
      clientRef.current = null;
      void client.deactivate();
      setConnected(false);
    };
  }, [userId]);

  // Đổi phòng chỉ thay subscription. Kết nối STOMP được giữ nguyên.
  useEffect(() => {
    const client = clientRef.current;
    if (!connected || !client?.connected || !roomId) {
      roomSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = null;
      return;
    }

    roomSubscriptionRef.current?.unsubscribe();
    roomSubscriptionRef.current = client.subscribe(
      `/topic/chat/${roomId}`,
      (frame) => {
        try {
          onMessageRef.current?.(JSON.parse(frame.body) as ChatMessage);
        } catch (error) {
          console.error('Không thể đọc tin nhắn chat:', error);
        }
      },
    );

    return () => {
      roomSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = null;
    };
  }, [connected, roomId]);

  const sendMessage = useCallback(
    (content: string): boolean => {
      const client = clientRef.current;
      if (!client?.connected || !roomId || !content.trim()) return false;

      const request: ChatMessageRequest = {
        roomId,
        content: content.trim(),
      };

      client.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(request),
      });
      return true;
    },
    [roomId],
  );

  return { connected, sendMessage };
}
