export interface CarMedia {
  id: string;
  carId: number;
  carName: string | null;
  thumbnail: string | null;
  images: string[];
  videos: string[];
  updatedAt: string;
}
