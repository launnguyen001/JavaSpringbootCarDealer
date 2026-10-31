-- Chạy thủ công một lần sau khi ứng dụng đã khởi động và Hibernate đã thêm các cột mới.
-- Script chỉ chuẩn hóa dữ liệu lịch sử; không chạy tự động khi ứng dụng khởi động.

-- Các đơn lịch sử đã được xác nhận trước khi có confirmed_at.
UPDATE sales_orders
SET confirmed_at = order_date
WHERE status IN ('CONFIRMED', 'COMPLETED')
  AND confirmed_at IS NULL;

-- Đơn PENDING/CONFIRMED chưa có hợp đồng phải giữ xe ở RESERVED.
-- Chỉ sửa xe SOLD nếu không có bất kỳ hợp đồng nào khác gắn với chiếc xe đó.
UPDATE car_items ci
JOIN order_items oi ON oi.car_item_id = ci.id
JOIN sales_orders so ON so.id = oi.order_id
LEFT JOIN contracts ct ON ct.order_id = so.id
SET ci.status = 'RESERVED'
WHERE so.status IN ('PENDING', 'CONFIRMED', 'COMPLETED')
  AND ct.id IS NULL
  AND ci.status IN ('AVAILABLE', 'SOLD')
  AND NOT EXISTS (
      SELECT 1
      FROM order_items oi2
      JOIN contracts ct2 ON ct2.order_id = oi2.order_id
      WHERE oi2.car_item_id = ci.id
  );

-- Hợp đồng đã tồn tại là dấu mốc xe chính thức chuyển sang SOLD.
UPDATE car_items ci
JOIN order_items oi ON oi.car_item_id = ci.id
JOIN contracts ct ON ct.order_id = oi.order_id
SET ci.status = 'SOLD'
WHERE ci.status <> 'SOLD';

-- Đồng bộ số tiền đã thu vào hóa đơn từ các giao dịch PAID/PARTIAL lịch sử.
UPDATE invoices i
JOIN sales_orders so ON so.id = i.order_id
LEFT JOIN (
    SELECT order_id, SUM(amount) AS paid_total
    FROM payments
    WHERE status IN ('PAID', 'PARTIAL')
    GROUP BY order_id
) p ON p.order_id = so.id
SET i.paid_amount = COALESCE(p.paid_total, 0),
    i.payment_status = CASE
        WHEN COALESCE(p.paid_total, 0) >= i.total THEN 'PAID'
        WHEN COALESCE(p.paid_total, 0) > 0 AND i.due_date IS NOT NULL AND i.due_date < CURRENT_DATE THEN 'OVERDUE'
        WHEN COALESCE(p.paid_total, 0) > 0 THEN 'PARTIALLY_PAID'
        WHEN i.due_date IS NOT NULL AND i.due_date < CURRENT_DATE THEN 'OVERDUE'
        ELSE 'UNPAID'
    END;
