export interface CarMedia {
  id: string;
  carId: number;
  thumbnail: string | null;
  images: string[];
  videos: string[];
  updatedAt: string;
}
