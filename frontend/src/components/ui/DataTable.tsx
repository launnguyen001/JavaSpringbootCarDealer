'use client';

import type { ReactNode } from 'react';

export interface Column<T> {
  key: string;
  label: string;
  className?: string;
  render?: (row: T) => ReactNode;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T, index: number) => string | number;
  empty?: string;
  onRowClick?: (row: T) => void;
}

export default function DataTable<T>({
  columns,
  rows,
  rowKey,
  empty = 'Không có dữ liệu.',
  onRowClick,
}: DataTableProps<T>) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-[#e5e7eb]">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-[#fafafa] border-b border-[#e5e7eb]">
            {columns.map((c) => (
              <th
                key={c.key}
                className={`px-4 py-3 text-left text-[11px] font-semibold text-[#6b7280] uppercase tracking-wider ${c.className ?? ''}`}
              >
                {c.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-10 text-center text-[#9ca3af]">
                {empty}
              </td>
            </tr>
          ) : (
            rows.map((row, index) => (
              <tr
                key={rowKey(row, index)}
                onClick={() => onRowClick?.(row)}
                className={`border-b border-[#f3f4f6] last:border-0 hover:bg-[#fafafa] transition-colors ${
                  onRowClick ? 'cursor-pointer' : ''
                }`}
              >
                {columns.map((c) => (
                  <td key={c.key} className={`px-4 py-3 text-[#374151] ${c.className ?? ''}`}>
                    {c.render ? c.render(row) : ((row as Record<string, unknown>)[c.key] as ReactNode)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  );
}
