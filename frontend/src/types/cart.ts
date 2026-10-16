// Giỏ hàng lưu ở Redis phía backend, định danh bằng sessionId (sinh ở client).
// Mỗi dòng giỏ trỏ tới MỘT chiếc xe vật lý cụ thể (car_item).
export interface CartItem {
  carItemId: number;
  quantity: number;
}
