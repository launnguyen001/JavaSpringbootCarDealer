'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import { Field, Input, Select, Textarea } from '@/components/ui/Field';
import DataTable, { type Column } from '@/components/ui/DataTable';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';

export interface FieldOption {
  value: string | number;
  label: string;
  /** Dữ liệu phụ tuỳ chọn, ví dụ totalAmount để auto-fill ô khác khi chọn. */
  meta?: Record<string, unknown>;
}

export interface FieldConfig {
  name: string;
  label: string;
  type?: 'text' | 'number' | 'textarea' | 'select' | 'datetime' | 'checkbox';
  options?: FieldOption[];
  /** Nạp lựa chọn bất đồng bộ khi mở form "Thêm mới" (chỉ dùng với type: 'select'). */
  optionsLoader?: () => Promise<FieldOption[]>;
  /** Hook khi người dùng chọn một lựa chọn — trả về các ô cần cập nhật thêm (auto-fill). */
  onPick?: (option: FieldOption) => Record<string, unknown> | void;
  /** Ẩn ô này khi mở form "Sửa" (ví dụ khoá đổi đơn hàng của chứng từ). */
  hideOnEdit?: boolean;
  required?: boolean;
  placeholder?: string;
  step?: string;
  span?: 1 | 2;
}

export type ResourceId = number | string;

export interface ResourceManagerProps<T> {
  title: string;
  description?: string;
  columns: Column<T>[];
  fields: FieldConfig[];
  list: () => Promise<T[]>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  create?: (payload: any) => Promise<T>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  update?: (id: any, payload: any) => Promise<T>;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  remove?: (id: any) => Promise<void>;
  getId: (row: T) => ResourceId;
  toForm?: (row: T) => Record<string, unknown>;
  canCreate?: boolean;
  canEdit?: boolean;
  canDelete?: boolean;
  search?: (term: string) => Promise<T[]>;
}

function emptyForm(fields: FieldConfig[]): Record<string, unknown> {
  const form: Record<string, unknown> = {};
  fields.forEach((f) => {
    form[f.name] = f.type === 'checkbox' ? false : '';
  });
  return form;
}

function toIsoOrNull(value: unknown): string | null {
  if (value === null || value === undefined || value === '') return null;
  const d = new Date(String(value));
  return Number.isNaN(d.getTime()) ? null : d.toISOString().slice(0, 19);
}

function toLocalInput(value: unknown): string {
  if (!value) return '';
  const d = new Date(String(value));
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

// Chuẩn hoá payload trước khi gửi: số -> number, datetime -> ISO, '' -> null
function normalizePayload(fields: FieldConfig[], form: Record<string, unknown>): Record<string, unknown> {
  const payload: Record<string, unknown> = {};
  fields.forEach((f) => {
    const raw = form[f.name];
    if (f.type === 'number') {
      payload[f.name] = raw === '' || raw === null || raw === undefined ? null : Number(raw);
    } else if (f.type === 'datetime') {
      payload[f.name] = toIsoOrNull(raw);
    } else if (f.type === 'checkbox') {
      payload[f.name] = !!raw;
    } else {
      payload[f.name] = raw === '' ? null : raw;
    }
  });
  return payload;
}

export default function ResourceManager<T>({
  title,
  description,
  columns,
  fields,
  list,
  create,
  update,
  remove,
  getId,
  toForm,
  canCreate = true,
  canEdit = true,
  canDelete = true,
  search,
}: ResourceManagerProps<T>) {
  const toast = useToast();
  const [rows, setRows] = useState<T[]>([]);
  const [loading, setLoading] = useState(true);
  const [term, setTerm] = useState('');
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<T | null>(null);
  const [form, setForm] = useState<Record<string, unknown>>(() => emptyForm(fields));
  const [submitting, setSubmitting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState<T | null>(null);
  const [dynamicOptions, setDynamicOptions] = useState<Record<string, FieldOption[]>>({});
  const [loadingOptions, setLoadingOptions] = useState<Record<string, boolean>>({});

  // Giữ tham chiếu mới nhất để tránh reload vòng lặp khi caller truyền inline function.
  const listRef = useRef(list);
  listRef.current = list;

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await listRef.current();
      setRows(data);
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [toast]);

  useEffect(() => {
    load();
  }, [load]);

  const loadDynamicOptions = () => {
    fields.forEach((f) => {
      if (!f.optionsLoader) return;
      const loader = f.optionsLoader;
      setLoadingOptions((p) => ({ ...p, [f.name]: true }));
      loader()
        .then((opts) => setDynamicOptions((p) => ({ ...p, [f.name]: opts })))
        .catch((err) => {
          toast.error(getErrorMessage(err));
          setDynamicOptions((p) => ({ ...p, [f.name]: [] }));
        })
        .finally(() => setLoadingOptions((p) => ({ ...p, [f.name]: false })));
    });
  };

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm(fields));
    loadDynamicOptions();
    setOpen(true);
  };

  const openEdit = (row: T) => {
    setEditing(row);
    const base = toForm ? toForm(row) : (row as unknown as Record<string, unknown>);
    const next: Record<string, unknown> = {};
    fields.forEach((f) => {
      const v = base[f.name];
      next[f.name] = f.type === 'datetime' ? toLocalInput(v) : (v ?? (f.type === 'checkbox' ? false : ''));
    });
    setForm(next);
    setOpen(true);
  };

  const submit = async () => {
    const missing = fields.find(
      (f) => f.required && (form[f.name] === '' || form[f.name] === null || form[f.name] === undefined),
    );
    if (missing) {
      toast.error(`Vui lòng nhập "${missing.label}"`);
      return;
    }
    setSubmitting(true);
    try {
      const payload = normalizePayload(fields, form);
      if (editing && update) {
        await update(getId(editing), payload);
        toast.success('Cập nhật thành công');
      } else if (create) {
        await create(payload);
        toast.success('Thêm mới thành công');
      }
      setOpen(false);
      await load();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const doDelete = async () => {
    if (!confirmDelete || !remove) return;
    setSubmitting(true);
    try {
      await remove(getId(confirmDelete));
      toast.success('Đã xóa');
      setConfirmDelete(null);
      await load();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const runSearch = async (value: string) => {
    setTerm(value);
    if (!search) return;
    if (!value.trim()) {
      await load();
      return;
    }
    setLoading(true);
    try {
      setRows(await search(value.trim()));
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  };

  const allColumns: Column<T>[] = [
    ...columns,
    ...(canEdit || canDelete
      ? [
          {
            key: '__actions',
            label: 'Thao tác',
            className: 'text-right whitespace-nowrap',
            render: (row: T) => (
              <div className="flex justify-end gap-2">
                {canEdit && update && (
                  <Button size="sm" variant="outline" onClick={() => openEdit(row)}>
                    Sửa
                  </Button>
                )}
                {canDelete && remove && (
                  <Button size="sm" variant="danger" onClick={() => setConfirmDelete(row)}>
                    Xóa
                  </Button>
                )}
              </div>
            ),
          } as Column<T>,
        ]
      : []),
  ];

  const visibleFields = fields.filter((f) => !(f.hideOnEdit && editing));

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-serif font-bold text-[#0a0a0a]">{title}</h1>
          {description && <p className="text-sm text-[#6b7280] mt-1">{description}</p>}
        </div>
        <div className="flex items-center gap-3">
          {search && (
            <Input
              value={term}
              onChange={(e) => runSearch(e.target.value)}
              placeholder="Tìm kiếm…"
              className="w-48"
            />
          )}
          <Button variant="ghost" size="sm" onClick={load} title="Tải lại">
            ↻
          </Button>
          {canCreate && create && (
            <Button variant="gold" onClick={openCreate}>
              + Thêm mới
            </Button>
          )}
        </div>
      </div>

      {loading ? (
        <div className="py-16 text-center text-[#9ca3af] text-sm">Đang tải…</div>
      ) : (
        <DataTable columns={allColumns} rows={rows} rowKey={(row) => getId(row)} />
      )}

      <Modal
        open={open}
        title={editing ? `Sửa ${title}` : `Thêm ${title}`}
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button variant="outline" onClick={() => setOpen(false)}>
              Hủy
            </Button>
            <Button variant="gold" onClick={submit} loading={submitting}>
              {editing ? 'Lưu thay đổi' : 'Tạo mới'}
            </Button>
          </>
        }
      >
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {visibleFields.map((f) => (
            <Field
              key={f.name}
              label={f.label}
              required={f.required}
              className={f.span === 2 || f.type === 'textarea' ? 'sm:col-span-2' : ''}
            >
              {f.type === 'textarea' ? (
                <Textarea
                  value={String(form[f.name] ?? '')}
                  onChange={(e) => setForm((p) => ({ ...p, [f.name]: e.target.value }))}
                  placeholder={f.placeholder}
                />
              ) : f.type === 'select' ? (
                <Select
                  value={String(form[f.name] ?? '')}
                  disabled={f.optionsLoader ? !!loadingOptions[f.name] : false}
                  onChange={(e) => {
                    const value = e.target.value;
                    const opts = f.optionsLoader
                      ? (dynamicOptions[f.name] ?? [])
                      : (f.options ?? []);
                    const picked = opts.find((o) => String(o.value) === value);
                    setForm((p) => {
                      const next: Record<string, unknown> = { ...p, [f.name]: value };
                      if (picked && f.onPick) {
                        const patch = f.onPick(picked);
                        if (patch) Object.assign(next, patch);
                      }
                      return next;
                    });
                  }}
                >
                  <option value="">
                    {f.optionsLoader && loadingOptions[f.name]
                      ? 'Đang tải…'
                      : f.placeholder ?? '— Chọn —'}
                  </option>
                  {(f.optionsLoader ? (dynamicOptions[f.name] ?? []) : (f.options ?? [])).map((o) => (
                    <option key={String(o.value)} value={String(o.value)}>
                      {o.label}
                    </option>
                  ))}
                  {f.optionsLoader &&
                    !loadingOptions[f.name] &&
                    (dynamicOptions[f.name] ?? []).length === 0 && (
                      <option key="__none" value="" disabled>
                        Không có lựa chọn phù hợp
                      </option>
                    )}
                </Select>
              ) : f.type === 'checkbox' ? (
                <label className="flex items-center gap-2 text-sm text-[#374151] py-2.5">
                  <input
                    type="checkbox"
                    checked={!!form[f.name]}
                    onChange={(e) => setForm((p) => ({ ...p, [f.name]: e.target.checked }))}
                  />
                  {f.placeholder ?? 'Bật'}
                </label>
              ) : (
                <Input
                  type={f.type === 'number' ? 'number' : f.type === 'datetime' ? 'datetime-local' : 'text'}
                  step={f.step}
                  value={String(form[f.name] ?? '')}
                  onChange={(e) => setForm((p) => ({ ...p, [f.name]: e.target.value }))}
                  placeholder={f.placeholder}
                />
              )}
            </Field>
          ))}
        </div>
      </Modal>

      <Modal
        open={!!confirmDelete}
        title="Xác nhận xóa"
        onClose={() => setConfirmDelete(null)}
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => setConfirmDelete(null)}>
              Hủy
            </Button>
            <Button variant="danger" onClick={doDelete} loading={submitting}>
              Xóa
            </Button>
          </>
        }
      >
        <p className="text-sm text-[#374151]">
          Bạn có chắc muốn xóa bản ghi này? Hành động không thể hoàn tác.
        </p>
      </Modal>
    </div>
  );
}
