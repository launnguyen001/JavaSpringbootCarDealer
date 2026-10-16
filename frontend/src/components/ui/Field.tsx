'use client';

import type {
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react';

const baseField =
  'w-full px-3.5 py-2.5 rounded-xl border border-[#e5e7eb] bg-white text-[#0a0a0a] text-sm focus:outline-none focus:ring-2 focus:ring-[#c58e00] disabled:bg-[#f9fafb]';

interface FieldProps {
  label?: string;
  htmlFor?: string;
  required?: boolean;
  hint?: string;
  children: ReactNode;
  className?: string;
}

export function Field({ label, htmlFor, required, hint, children, className = '' }: FieldProps) {
  return (
    <div className={className}>
      {label && (
        <label
          htmlFor={htmlFor}
          className="block text-[11px] font-semibold text-[#6b7280] uppercase tracking-wider mb-1.5"
        >
          {label} {required && <span className="text-[#ef4444]">*</span>}
        </label>
      )}
      {children}
      {hint && <p className="mt-1 text-xs text-[#9ca3af]">{hint}</p>}
    </div>
  );
}

export function Input({ className = '', ...rest }: InputHTMLAttributes<HTMLInputElement>) {
  return <input {...rest} className={`${baseField} ${className}`} />;
}

export function Textarea({ className = '', ...rest }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea {...rest} className={`${baseField} min-h-[90px] ${className}`} />;
}

export function Select({
  className = '',
  children,
  ...rest
}: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select {...rest} className={`${baseField} appearance-none ${className}`}>
      {children}
    </select>
  );
}
