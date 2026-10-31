'use client';

import type { ButtonHTMLAttributes } from 'react';

type Variant = 'primary' | 'gold' | 'outline' | 'ghost' | 'danger';
type Size = 'sm' | 'md' | 'lg';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const variantClasses: Record<Variant, string> = {
  primary: 'bg-[#0a0a0a] text-white hover:bg-[#c58e00] shadow-sm',
  gold: 'bg-[#c58e00] text-white hover:bg-[#8a6d00] shadow-sm',
  outline: 'border border-[#e5e7eb] text-[#0a0a0a] bg-white hover:bg-[#f3f4f6]',
  ghost: 'text-[#4b5563] hover:bg-[#f3f4f6] hover:text-[#0a0a0a]',
  danger: 'bg-[#ef4444] text-white hover:bg-[#dc2626] shadow-sm',
};

const sizeClasses: Record<Size, string> = {
  sm: 'px-3 py-1.5 text-xs',
  md: 'px-4 py-2.5 text-sm',
  lg: 'px-6 py-3.5 text-base',
};

export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  className = '',
  disabled,
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-xl font-medium transition-all duration-150 hover:-translate-y-px focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#c58e00]/40 focus-visible:ring-offset-1 active:translate-y-0 active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0 ${variantClasses[variant]} ${sizeClasses[size]} ${className}`}
    >
      {loading && (
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" className="animate-spin">
          <circle cx="12" cy="12" r="10" opacity="0.3" />
          <path d="M22 12a10 10 0 0 0-10-10" />
        </svg>
      )}
      {children}
    </button>
  );
}
