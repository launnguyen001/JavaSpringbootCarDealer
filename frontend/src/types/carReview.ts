export interface Reply {
  author: string;
  content: string;
  createdAt: string;
}

export interface CarReview {
  id: string;
  carId: number;
  customerId: number;
  customerName: string;
  rating: number;
  title: string | null;
  content: string;
  createdAt: string;
  replies: Reply[];
}

export interface CarReviewSummary {
  carId: number;
  totalReviews: number;
  averageRating: number;
}
