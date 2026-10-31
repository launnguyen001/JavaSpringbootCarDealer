export function formatPrice(price: number | null | undefined): string {
  if (price == null || !Number.isFinite(price)) {
    return 'Liên hệ';
  }
  // Backend trả đơn vị (ví dụ 950 = 950 triệu VND)
  return `${price.toLocaleString('vi-VN')} triệu`;
}

export function formatPriceRaw(price: number | null | undefined): string {
  if (price == null || !Number.isFinite(price)) {
    return 'Liên hệ';
  }
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(price * 1_000_000);
}

export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return '—';
  try {
    return new Date(dateStr).toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  } catch {
    return dateStr;
  }
}

export function formatRating(rating: number | null | undefined): string {
  if (rating == null || !Number.isFinite(rating)) {
    return 'Chưa có đánh giá';
  }
  return rating.toFixed(1);
}

export function formatDateTime(dateStr: string | null | undefined): string {
  if (!dateStr) return '—';
  try {
    return new Date(dateStr).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  } catch {
    return dateStr;
  }
}

/**
 * Định dạng tiền tệ VND từ đơn vị "triệu" của backend (950 -> 950.000.000 ₫).
 */
export function formatVnd(millions: number | null | undefined): string {
  if (millions == null || !Number.isFinite(millions)) return '—';
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(millions * 1_000_000);
}


/**
 * Định dạng doanh thu từ đơn vị triệu đồng do backend trả về.
 * Chỉ chuyển đơn vị khi hiển thị; không thay đổi giá trị dữ liệu gốc.
 */
export function formatCurrentcy(amount: number | null | undefined): string {
  if (amount == null || !Number.isFinite(amount)) return '—';

  const formatter = new Intl.NumberFormat('vi-VN', {
    maximumFractionDigits: 2,
  });

  if (Math.abs(amount) >= 1_000) {
    return `${formatter.format(amount / 1_000)} tỷ`;
  }

  return `${formatter.format(amount)} triệu`;
}
