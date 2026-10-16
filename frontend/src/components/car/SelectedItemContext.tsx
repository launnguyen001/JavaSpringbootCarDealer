'use client';

import { createContext, useContext, useMemo, useState } from 'react';
import type { CarItem } from '@/types';

interface SelectedItemContextValue {
  /** Tất cả các chiếc của dòng xe. */
  items: CarItem[];
  /** Những chiếc còn sẵn sàng để chọn mua / lái thử. */
  available: CarItem[];
  /** Chiếc đang được chọn (mặc định là chiếc khả dụng đầu tiên). */
  selected: CarItem | null;
  selectedId: number | null;
  setSelectedId: (id: number) => void;
}

const SelectedItemContext = createContext<SelectedItemContextValue | null>(null);

export function SelectedItemProvider({
  items,
  children,
}: {
  items: CarItem[];
  children: React.ReactNode;
}) {
  const [selectedId, setSelectedId] = useState<number | null>(items[0]?.id ?? null);

  const available = useMemo(() => items.filter((i) => i.status === 'AVAILABLE'), [items]);
  const selected = available.find((i) => i.id === selectedId) ?? available[0] ?? null;

  const value = useMemo(
    () => ({ items, available, selected, selectedId, setSelectedId }),
    [items, available, selected, selectedId],
  );

  return <SelectedItemContext.Provider value={value}>{children}</SelectedItemContext.Provider>;
}

export function useSelectedItem(): SelectedItemContextValue {
  const ctx = useContext(SelectedItemContext);
  if (!ctx) {
    throw new Error('useSelectedItem phải được dùng bên trong <SelectedItemProvider>');
  }
  return ctx;
}
