'use client';

import { useEffect, useRef, useState } from 'react';
import Button from '@/components/ui/Button';
import Modal from '@/components/ui/Modal';
import { Field, Input, Select, Textarea } from '@/components/ui/Field';
import { useToast } from '@/components/ui/Toast';
import { getErrorMessage } from '@/lib/utils/error';
import type { FieldConfig, FieldOption } from '@/components/admin/ResourceManager';

interface ResourceFormModalProps {
  /**
   * Component nên được mount có điều kiện: `{state && <ResourceFormModal ... />}`.
   * Mỗi lần mount sẽ khởi tạo form từ `fields` và `initialValues` đúng một lần.
   */
  open?: boolean;
  title: string;
  fields: FieldConfig[];
  /** Giá trị điền sẵn (ví dụ orderId, amount). Chỉ đọc lúc mount. */
  initialValues?: Record<string, unknown>;
  /** Tên các ô cần ẩn khỏi form (vẫn được gửi đi với giá trị từ initialValues). */
  hiddenFields?: string[];
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  create: (payload: any) => Promise<unknown>;
  onClose: () => void;
  onSuccess?: () => void;
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

export default function ResourceFormModal({
  open = true,
  title,
  fields,
  initialValues,
  hiddenFields,
  create,
  onClose,
  onSuccess,
}: ResourceFormModalProps) {
  const toast = useToast();
  const toastRef = useRef(toast);
  useEffect(() => {
    toastRef.current = toast;
  }, [toast]);

  // Chụp fields / initialValues một lần lúc mount để effect nạp options không chạy lại
  // mỗi khi component cha render (dù cha truyền inline object/array).
  const [initialFields] = useState(fields);
  const [initial] = useState<Record<string, unknown>>(() => initialValues ?? {});

  const [form, setForm] = useState<Record<string, unknown>>(() => ({
    ...emptyForm(fields),
    ...(initialValues ?? {}),
  }));
  const [submitting, setSubmitting] = useState(false);
  const [dynamicOptions, setDynamicOptions] = useState<Record<string, FieldOption[]>>({});
  const [loadingOptions, setLoadingOptions] = useState<Record<string, boolean>>(() => {
    const state: Record<string, boolean> = {};
    fields.forEach((f) => {
      if (f.optionsLoader) state[f.name] = true;
    });
    return state;
  });

  // Chỉ gọi setState trong callback bất đồng bộ (sau khi promise xong)
  useEffect(() => {
    let active = true;
    initialFields.forEach((f) => {
      if (!f.optionsLoader) return;
      f.optionsLoader()
        .then((opts) => {
          if (!active) return;
          setDynamicOptions((p) => ({ ...p, [f.name]: opts }));
          // Nếu đã điền sẵn giá trị (vd orderId), chạy onPick để auto-fill các ô liên quan
          const preset = initial[f.name];
          if (preset !== undefined && preset !== '' && f.onPick) {
            const selected = opts.find((o) => String(o.value) === String(preset));
            const patch = selected ? f.onPick(selected) : undefined;
            if (patch) setForm((prev) => ({ ...prev, ...patch, ...initial }));
          }
        })
        .catch((err) => {
          if (!active) return;
          toastRef.current.error(getErrorMessage(err));
          setDynamicOptions((p) => ({ ...p, [f.name]: [] }));
        })
        .finally(() => {
          if (active) setLoadingOptions((p) => ({ ...p, [f.name]: false }));
        });
    });
    return () => {
      active = false;
    };
  }, [initialFields, initial]);

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
      await create(normalizePayload(fields, form));
      toast.success('Thêm mới thành công');
      onClose();
      onSuccess?.();
    } catch (err) {
      toast.error(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const visibleFields = fields.filter((f) => !hiddenFields?.includes(f.name) && !f.hideOnCreate);

  return (
    <Modal
      open={open}
      title={`Thêm ${title}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Hủy
          </Button>
          <Button variant="gold" onClick={submit} loading={submitting}>
            Tạo mới
          </Button>
        </>
      }
    >
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {visibleFields.map((f) => {
          const opts = f.optionsLoader ? (dynamicOptions[f.name] ?? []) : (f.options ?? []);
          const isLoading = !!f.optionsLoader && !!loadingOptions[f.name];
          return (
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
                  disabled={isLoading}
                  onChange={(e) => {
                    const value = e.target.value;
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
                  <option value="">{isLoading ? 'Đang tải…' : (f.placeholder ?? '— Chọn —')}</option>
                  {opts.map((o) => (
                    <option key={String(o.value)} value={String(o.value)}>
                      {o.label}
                    </option>
                  ))}
                  {f.optionsLoader && !isLoading && opts.length === 0 && (
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
                  type={
                    f.type === 'number'
                      ? 'number'
                      : f.type === 'datetime'
                        ? 'datetime-local'
                        : f.type === 'date'
                          ? 'date'
                          : 'text'
                  }
                  step={f.step}
                  value={String(form[f.name] ?? '')}
                  onChange={(e) => setForm((p) => ({ ...p, [f.name]: e.target.value }))}
                  placeholder={f.placeholder}
                />
              )}
            </Field>
          );
        })}
      </div>
    </Modal>
  );
}