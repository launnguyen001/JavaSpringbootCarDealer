'use client';

import Link from 'next/link';
import { useSearchParams, useRouter, usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import {
  listNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead,
} from '@/lib/api';
import type { AppNotification } from '@/types';
import { formatDateTime } from '@/lib/utils/format';
import { getUnreadCounts } from '@/lib/api/chat';
import { getToken } from '@/lib/auth/token';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export default function Header() {
  const [open, setOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifications, setNotifications] = useState<AppNotification[]>([]);
  const [unread, setUnread] = useState(0);
  const [chatUnread, setChatUnread] = useState(0);
  const [notifLoading, setNotifLoading] = useState(false);
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const { user, logout, isAdmin } = useAuth();

  // Cập nhật số thông báo chưa đọc mỗi 60 giây (và khi đổi tài khoản).
  useEffect(() => {
    if (!user) return;
    let active = true;
    const refresh = () => {
      getUnreadNotificationCount()
        .then((count) => {
          if (active) setUnread(count);
        })
        .catch(() => {
          // Header không chặn UI nếu API thông báo lỗi.
        });
    };
    refresh();
    const timer = window.setInterval(refresh, 60_000);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [user]);

  // Tổng số tin nhắn chưa đọc hiển thị ngay trên Header.
  useEffect(() => {
    if (!user) { setChatUnread(0); return; }
    let active = true;
    const refreshChatUnread = () => {
      getUnreadCounts().then((counts) => {
        if (active) setChatUnread(Object.values(counts).reduce((sum, count) => sum + count, 0));
      }).catch(() => {});
    };
    refreshChatUnread();
    const token = getToken();
    let client: Client | null = null;
    if (token) {
      client = new Client({
        webSocketFactory: () => new SockJS('/ws'),
        connectHeaders: { Authorization: `Bearer ${token}` },
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          client?.subscribe(`/topic/user/${user.id}/chat-notifications`, refreshChatUnread);
        },
      });
      void client.activate();
    }
    const timer = window.setInterval(refreshChatUnread, 60000);
    return () => { active = false; window.clearInterval(timer); void client?.deactivate(); };
  }, [user]);

  const toggleNotifications = async () => {
    const next = !notifOpen;
    setNotifOpen(next);
    setMenuOpen(false);
    if (!next) return;
    setNotifLoading(true);
    try {
      const data = await listNotifications();
      setNotifications(data);
      setUnread(data.filter((item) => !item.read).length);
    } catch {
      setNotifications([]);
    } finally {
      setNotifLoading(false);
    }
  };

  const markRead = async (notification: AppNotification) => {
    if (notification.read) return;
    try {
      await markNotificationRead(notification.id);
      setNotifications((prev) =>
        prev.map((item) => (item.id === notification.id ? { ...item, read: true } : item)),
      );
      setUnread((prev) => Math.max(prev - 1, 0));
    } catch {
      // Bỏ qua lỗi mạng nhỏ; lần mở sau sẽ đồng bộ lại.
    }
  };

  const markAllRead = async () => {
    try {
      await markAllNotificationsRead();
      setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));
      setUnread(0);
    } catch {
      // Bỏ qua lỗi mạng nhỏ.
    }
  };

  // Trang hiện tại có "khớp" với href không (dùng cho hiệu ứng active trên nav)
  const isActive = (href: string) =>
    href === '/' ? pathname === '/' : pathname === href || pathname.startsWith(`${href}/`);

  // Link trên nav desktop: đang ở trang nào thì tô vàng + gạch chân
  const navLinkClass = (href: string) =>
    [
      'relative py-1 transition-colors hover:text-[#c58e00]',
      "after:absolute after:left-0 after:-bottom-0.5 after:h-0.5 after:rounded-full after:bg-[#c58e00] after:transition-all after:duration-200",
      isActive(href) ? 'text-[#c58e00] after:w-full' : 'after:w-0',
    ].join(' ');

  // Link trên nav mobile: trang hiện tại được tô nền + chữ vàng
  const mobileLinkClass = (href: string) =>
    [
      'block px-2 py-2 rounded-xl transition-colors',
      isActive(href)
        ? 'bg-[#f3f4f6] text-[#c58e00] font-medium'
        : 'text-[#0a0a0a] hover:bg-[#f3f4f6] hover:text-[#c58e00]',
    ].join(' ');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const q = (e.target as HTMLFormElement).elements.namedItem('q') as HTMLInputElement;
    const params = new URLSearchParams(searchParams.toString());
    if (q.value.trim()) {
      params.set('q', q.value.trim());
    } else {
      params.delete('q');
    }
    router.push(`/cars?${params.toString()}`);
    setOpen(false);
  };

  const handleLogout = async () => {
    await logout();
    setMenuOpen(false);
    setOpen(false);
    setNotifOpen(false);
    setNotifications([]);
    setUnread(0);
    setChatUnread(0);
    router.push('/');
    router.refresh();
  };

  return (
    <header className="sticky top-0 z-40 border-b border-[#e5e7eb] bg-white/80 backdrop-blur-md">
      <div className="px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <Link href="/" className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-[#0a0a0a] flex items-center justify-center shadow-lg shadow-[#c58e00]/30">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#ffffff" strokeWidth="2.5">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                <circle cx="9" cy="7" r="4" />
                <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                <path d="M16 3.13a4 4 0 0 1 0 7.75" />
              </svg>
            </div>
            <span className="font-serif font-bold text-xl text-[#0a0a0a] tracking-tighter">CARDEALER</span>
          </Link>

          <nav className="hidden lg:flex items-center gap-8 text-sm font-medium text-[#0a0a0a] uppercase tracking-widest">
            {[
              { href: '/', label: 'Trang chủ' },
              { href: '/cars', label: 'Danh sách xe' },
            ].map((item) => (
              <Link
                key={item.href}
                href={item.href}
                aria-current={isActive(item.href) ? 'page' : undefined}
                className={navLinkClass(item.href)}
              >
                {item.label}
              </Link>
            ))}
            {user && (
              <Link
                href="/chat"
                aria-current={isActive('/chat') ? 'page' : undefined}
                className={navLinkClass('/chat')}
              >
                Tin nhắn
                {chatUnread > 0 && <span className="absolute -top-2 -right-4 min-w-[18px] h-[18px] px-1 rounded-full bg-[#ef4444] text-white text-[10px] font-bold flex items-center justify-center">{chatUnread > 99 ? '99+' : chatUnread}</span>}
              </Link>
            )}
            <a href="#" className="relative py-1 transition-colors hover:text-[#c58e00]">
              Liên hệ
            </a>
          </nav>

          <div className="flex items-center gap-2 lg:gap-3">
            <Link
              href="/cart"
              className={`relative p-2 transition-colors hover:text-[#c58e00] ${
                isActive('/cart') ? 'text-[#c58e00]' : 'text-[#0a0a0a]'
              }`}
              title="Giỏ hàng"
              aria-current={isActive('/cart') ? 'page' : undefined}
            >
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="9" cy="21" r="1" />
                <circle cx="20" cy="21" r="1" />
                <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
              </svg>
            </Link>

            {user && (
              <div className="relative">
                <button
                  onClick={toggleNotifications}
                  className={`relative p-2 transition-colors hover:text-[#c58e00] ${
                    notifOpen ? 'text-[#c58e00]' : 'text-[#0a0a0a]'
                  }`}
                  title="Thông báo"
                  aria-label="Thông báo"
                  aria-expanded={notifOpen}
                >
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
                  </svg>
                  {unread > 0 && (
                    <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 rounded-full bg-[#ef4444] text-white text-[10px] font-bold flex items-center justify-center">
                      {unread > 9 ? '9+' : unread}
                    </span>
                  )}
                </button>

                {notifOpen && (
                  <div className="absolute right-0 mt-2 w-80 max-w-[calc(100vw-2rem)] bg-white rounded-xl border border-[#e5e7eb] shadow-xl overflow-hidden animate-fade-in z-50">
                    <div className="flex items-center justify-between px-4 py-3 border-b border-[#f3f4f6]">
                      <span className="text-sm font-semibold text-[#0a0a0a]">Thông báo</span>
                      {unread > 0 && (
                        <button
                          onClick={markAllRead}
                          className="text-xs font-medium text-[#c58e00] hover:underline"
                        >
                          Đọc tất cả
                        </button>
                      )}
                    </div>
                    <div className="max-h-80 overflow-y-auto">
                      {notifLoading ? (
                        <p className="px-4 py-8 text-center text-sm text-[#9ca3af]">Đang tải…</p>
                      ) : notifications.length === 0 ? (
                        <p className="px-4 py-8 text-center text-sm text-[#9ca3af]">
                          Chưa có thông báo nào.
                        </p>
                      ) : (
                        notifications.slice(0, 8).map((notification) => (
                          <button
                            key={notification.id}
                            onClick={() => markRead(notification)}
                            className={`w-full text-left px-4 py-3 border-b border-[#f3f4f6] last:border-0 hover:bg-[#fafafa] transition-colors ${
                              notification.read ? '' : 'bg-[#fffbeb]'
                            }`}
                          >
                            <div className="flex items-start gap-2">
                              <span
                                className={`mt-1.5 h-2 w-2 rounded-full shrink-0 ${
                                  notification.read ? 'bg-transparent' : 'bg-[#c58e00]'
                                }`}
                              />
                              <div className="min-w-0">
                                <p className="text-sm font-medium text-[#0a0a0a] truncate">
                                  {notification.title}
                                </p>
                                <p className="mt-0.5 text-xs text-[#6b7280] line-clamp-2">
                                  {notification.message}
                                </p>
                                <p className="mt-1 text-[10px] text-[#9ca3af]">
                                  {formatDateTime(notification.createdAt)}
                                </p>
                              </div>
                            </div>
                          </button>
                        ))
                      )}
                    </div>
                    <Link
                      href="/account"
                      onClick={() => setNotifOpen(false)}
                      className="block px-4 py-3 text-center text-xs font-medium text-[#c58e00] border-t border-[#f3f4f6] hover:bg-[#fafafa]"
                    >
                      Xem tất cả thông báo
                    </Link>
                  </div>
                )}
              </div>
            )}

            {user ? (
              <div className="relative hidden lg:block">
                <button
                  onClick={() => {
                    setMenuOpen((v) => !v);
                    setNotifOpen(false);
                  }}
                  className="flex items-center gap-2 px-3 py-2 rounded-xl text-sm font-medium text-[#0a0a0a] hover:bg-[#f3f4f6] transition-colors"
                >
                  <span className="w-7 h-7 rounded-full bg-[#c58e00] text-white flex items-center justify-center text-xs font-bold uppercase">
                    {user.username.charAt(0)}
                  </span>
                  {user.username}
                </button>
                {menuOpen && (
                  <div
                    className="absolute right-0 mt-2 w-52 bg-white rounded-xl border border-[#e5e7eb] shadow-xl py-2 animate-fade-in"
                    onMouseLeave={() => setMenuOpen(false)}
                  >
                    <div className="px-4 py-2 border-b border-[#f3f4f6]">
                      <p className="text-xs text-[#9ca3af] uppercase tracking-wider">{user.role}</p>
                    </div>
                    <Link
                      href="/account"
                      className={`block px-4 py-2 text-sm hover:bg-[#f9fafb] ${
                        isActive('/account') ? 'text-[#c58e00] font-medium' : 'text-[#374151]'
                      }`}
                      onClick={() => setMenuOpen(false)}
                    >
                      Tài khoản của tôi
                    </Link>
                    <Link
                      href="/cart"
                      className={`block px-4 py-2 text-sm hover:bg-[#f9fafb] ${
                        isActive('/cart') ? 'text-[#c58e00] font-medium' : 'text-[#374151]'
                      }`}
                      onClick={() => setMenuOpen(false)}
                    >
                      Giỏ hàng
                    </Link>
                    {isAdmin && (
                      <>
                        <Link
                          href="/admin"
                          className={`block px-4 py-2 text-sm font-medium hover:bg-[#f9fafb] ${
                            isActive('/admin') ? 'bg-[#f9fafb] text-[#c58e00]' : 'text-[#c58e00]'
                          }`}
                          onClick={() => setMenuOpen(false)}
                        >
                          Trang quản trị
                        </Link>
                        <Link
                          href="/admin/users"
                          className={`block px-4 py-2 text-sm font-medium hover:bg-[#f9fafb] ${
                            pathname === '/admin/users' ? 'bg-[#f9fafb] text-[#c58e00]' : 'text-[#c58e00]'
                          }`}
                          onClick={() => setMenuOpen(false)}
                        >
                          Người dùng
                        </Link>
                      </>
                    )}
                    <button
                      onClick={handleLogout}
                      className="w-full text-left px-4 py-2 text-sm text-[#ef4444] hover:bg-[#fef2f2]"
                    >
                      Đăng xuất
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="hidden lg:flex items-center gap-2">
                <Link
                  href="/login"
                  className="px-4 py-2 text-sm font-medium text-[#0a0a0a] hover:text-[#c58e00] transition-colors"
                >
                  Đăng nhập
                </Link>
                <Link
                  href="/register"
                  className="px-4 py-2 rounded-xl bg-[#0a0a0a] text-white text-sm font-medium hover:bg-[#c58e00] transition-colors"
                >
                  Đăng ký
                </Link>
              </div>
            )}

            <button
              onClick={() => setOpen(true)}
              className="lg:hidden p-2 text-[#0a0a0a] hover:text-[#c58e00]"
            >
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </svg>
            </button>
          </div>
        </div>

        {open && (
          <div className="lg:hidden mt-4 pb-6 border-t border-[#e5e7eb] pt-4">
            <form onSubmit={handleSearch} className="flex gap-2 mb-6">
              <input
                name="q"
                type="text"
                placeholder="Tìm xe…"
                className="flex-1 px-4 py-2 rounded-xl border border-[#e5e7eb] bg-[#fafafa] text-[#0a0a0a] focus:outline-none focus:ring-2 focus:ring-[#c58e00]"
              />
              <button type="submit" className="px-4 py-2 rounded-xl bg-[#0a0a0a] text-white font-medium text-sm hover:bg-[#c58e00] transition-colors">
                Tìm
              </button>
            </form>
            <nav className="space-y-3">
              <Link
                href="/"
                onClick={() => setOpen(false)}
                aria-current={isActive('/') ? 'page' : undefined}
                className={mobileLinkClass('/')}
              >
                Trang chủ
              </Link>
              <Link
                href="/cars"
                onClick={() => setOpen(false)}
                aria-current={isActive('/cars') ? 'page' : undefined}
                className={mobileLinkClass('/cars')}
              >
                Danh sách xe
              </Link>
              <Link
                href="/cart"
                onClick={() => setOpen(false)}
                aria-current={isActive('/cart') ? 'page' : undefined}
                className={mobileLinkClass('/cart')}
              >
                Giỏ hàng
              </Link>
              {user && (
                <Link
                  href="/chat"
                  onClick={() => setOpen(false)}
                  aria-current={isActive('/chat') ? 'page' : undefined}
                  className={mobileLinkClass('/chat')}
                >
                  <span className="flex items-center justify-between">Tin nhắn
                    {chatUnread > 0 && <span className="min-w-5 h-5 px-1 rounded-full bg-[#ef4444] text-white text-[10px] font-bold flex items-center justify-center">{chatUnread > 99 ? '99+' : chatUnread}</span>}
                  </span>
                </Link>
              )}

              {user ? (
                <>
                  <Link
                    href="/account"
                    onClick={() => setOpen(false)}
                    aria-current={isActive('/account') ? 'page' : undefined}
                    className={mobileLinkClass('/account')}
                  >
                    Tài khoản của tôi
                  </Link>
                  {isAdmin && (
                    <>
                      <Link
                        href="/admin"
                        onClick={() => setOpen(false)}
                        aria-current={isActive('/admin') ? 'page' : undefined}
                        className={`block px-2 py-2 rounded-xl transition-colors ${
                          isActive('/admin')
                            ? 'bg-[#f3f4f6] text-[#c58e00] font-medium'
                            : 'text-[#c58e00] font-medium hover:bg-[#f3f4f6]'
                        }`}
                      >
                        Trang quản trị
                      </Link>
                      <Link
                        href="/admin/users"
                        onClick={() => setOpen(false)}
                        aria-current={pathname === '/admin/users' ? 'page' : undefined}
                        className={`block px-2 py-2 rounded-xl transition-colors ${
                          pathname === '/admin/users'
                            ? 'bg-[#f3f4f6] text-[#c58e00] font-medium'
                            : 'text-[#c58e00] font-medium hover:bg-[#f3f4f6]'
                        }`}
                      >
                        Người dùng
                      </Link>
                    </>
                  )}
                  <button
                    onClick={handleLogout}
                    className="block w-full text-left px-2 py-2 rounded-xl text-[#ef4444] hover:bg-[#fef2f2] transition-colors"
                  >
                    Đăng xuất
                  </button>
                </>
              ) : (
                <>
                  <Link
                    href="/login"
                    onClick={() => setOpen(false)}
                    aria-current={isActive('/login') ? 'page' : undefined}
                    className={mobileLinkClass('/login')}
                  >
                    Đăng nhập
                  </Link>
                  <Link
                    href="/register"
                    onClick={() => setOpen(false)}
                    aria-current={isActive('/register') ? 'page' : undefined}
                    className={mobileLinkClass('/register')}
                  >
                    Đăng ký
                  </Link>
                </>
              )}
            </nav>
          </div>
        )}
      </div>
    </header>
  );
}
