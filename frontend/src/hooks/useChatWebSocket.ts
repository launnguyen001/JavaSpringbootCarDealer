'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getToken } from '@/lib/auth/token';
import type { ChatMessage, ChatMessageRequest, ChatRealtimeEvent } from '@/types';

interface UseChatWebSocketOptions {
  roomId: string | null;
  userId: number | null;
  onMessage?: (message: ChatMessage) => void;
  onNotification?: (message: ChatMessage) => void;
  onEvent?: (event: ChatRealtimeEvent) => void;
}

interface UseChatWebSocketReturn {
  connected: boolean;
  sendMessage: (content: string) => boolean;
  sendTyping: (typing: boolean) => void;
  markRead: (messageId: string) => void;
}

export function useChatWebSocket({
  roomId, userId, onMessage, onNotification, onEvent,
}: UseChatWebSocketOptions): UseChatWebSocketReturn {
  const [connected, setConnected] = useState(false);
  const clientRef = useRef<Client | null>(null);
  const roomSubscriptionRef = useRef<{ unsubscribe: () => void } | null>(null);
  const eventSubscriptionRef = useRef<{ unsubscribe: () => void } | null>(null);
  const notificationSubscriptionRef = useRef<{ unsubscribe: () => void } | null>(null);
  const onMessageRef = useRef(onMessage);
  const onNotificationRef = useRef(onNotification);
  const onEventRef = useRef(onEvent);

  useEffect(() => { onMessageRef.current = onMessage; }, [onMessage]);
  useEffect(() => { onNotificationRef.current = onNotification; }, [onNotification]);
  useEffect(() => { onEventRef.current = onEvent; }, [onEvent]);

  useEffect(() => {
    const token = getToken();
    if (!token || userId === null) { setConnected(false); return; }
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        notificationSubscriptionRef.current?.unsubscribe();
        notificationSubscriptionRef.current = client.subscribe(
          `/topic/user/${userId}/chat-notifications`,
          (frame) => {
            try { onNotificationRef.current?.(JSON.parse(frame.body) as ChatMessage); }
            catch (error) { console.error('Không thể đọc notification chat:', error); }
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
      eventSubscriptionRef.current?.unsubscribe();
      notificationSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = null;
      eventSubscriptionRef.current = null;
      notificationSubscriptionRef.current = null;
      clientRef.current = null;
      void client.deactivate();
      setConnected(false);
    };
  }, [userId]);

  useEffect(() => {
    const client = clientRef.current;
    if (!connected || !client?.connected || !roomId) {
      roomSubscriptionRef.current?.unsubscribe();
      eventSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = null;
      eventSubscriptionRef.current = null;
      return;
    }
    roomSubscriptionRef.current?.unsubscribe();
    eventSubscriptionRef.current?.unsubscribe();
    roomSubscriptionRef.current = client.subscribe(`/topic/chat/${roomId}`, (frame) => {
      try { onMessageRef.current?.(JSON.parse(frame.body) as ChatMessage); }
      catch (error) { console.error('Không thể đọc tin nhắn chat:', error); }
    });
    eventSubscriptionRef.current = client.subscribe(`/topic/chat/${roomId}/events`, (frame) => {
      try { onEventRef.current?.(JSON.parse(frame.body) as ChatRealtimeEvent); }
      catch (error) { console.error('Không thể đọc chat event:', error); }
    });
    return () => {
      roomSubscriptionRef.current?.unsubscribe();
      eventSubscriptionRef.current?.unsubscribe();
      roomSubscriptionRef.current = null;
      eventSubscriptionRef.current = null;
    };
  }, [connected, roomId]);

  const sendMessage = useCallback((content: string): boolean => {
    const client = clientRef.current;
    if (!client?.connected || !roomId || !content.trim()) return false;
    const request: ChatMessageRequest = { roomId, content: content.trim() };
    client.publish({ destination: '/app/chat.send', body: JSON.stringify(request) });
    return true;
  }, [roomId]);

  const sendTyping = useCallback((typing: boolean) => {
    const client = clientRef.current;
    if (!client?.connected || !roomId) return;
    client.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ roomId, typing }),
    });
  }, [roomId]);

  const markRead = useCallback((messageId: string) => {
    const client = clientRef.current;
    if (!client?.connected || !roomId || !messageId) return;
    client.publish({
      destination: '/app/chat.read',
      body: JSON.stringify({ roomId, lastReadMessageId: messageId }),
    });
  }, [roomId]);

  return { connected, sendMessage, sendTyping, markRead };
}
