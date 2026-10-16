// Giỏ hàng lưu ở Redis phía backend, định danh bằng sessionId (sinh ở client).
export interface CartItem {
  carId: number;
  quantity: number;
}
