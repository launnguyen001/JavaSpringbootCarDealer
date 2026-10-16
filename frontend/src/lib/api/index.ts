// ---- Catalog (đọc) ----
export {
  listCars,
  getCar,
  getCarItems,
  searchCarsByName,
  searchCarsByPrice,
  searchCarsByStock,
  searchCarsAdvanced,
  searchCarsAdvancedByBrand,
  getCarsByBrand,
  getCarsByModel,
  createCar,
  updateCar,
  deleteCar,
} from './cars';
export type { CarRequest } from './cars';

export {
  listCarItems,
  listAvailableCarItems,
  getCarItem,
  createCarItem,
  updateCarItem,
  updateCarItemStatus,
  deleteCarItem,
} from './carItems';

export { listUsers, updateUserStatus, updateUserRole } from './users';

export { listBrands, getBrand, createBrand, updateBrand, deleteBrand } from './brands';
export type { BrandRequest } from './brands';

export {
  listCarModels,
  getCarModel,
  searchCarModelsByName,
  getCarModelsByBrand,
  createCarModel,
  updateCarModel,
  deleteCarModel,
} from './carModels';
export type { CarModelRequest } from './carModels';

export { getCarSpecs, listCarSpecs, upsertCarSpecs, deleteCarSpecs } from './carSpecs';
export type { CarSpecificationRequest } from './carSpecs';

export { getCarMedia, listCarMedia, upsertCarMedia, deleteCarMedia } from './carMedia';
export type { CarMediaRequest } from './carMedia';

export {
  getCarReviews,
  getCarReviewSummary,
  listCarReviews,
  createCarReview,
  updateCarReview,
  deleteCarReview,
  replyToReview,
} from './carReviews';
export type { CarReviewRequest } from './carReviews';

// ---- Auth ----
export { login, register, me, updateMe, logout } from './auth';

// ---- Nghiệp vụ ----
export * from './cart';
export * from './customers';
export * from './orders';
export * from './testDrives';
export * from './employees';
export * from './payments';
export * from './contracts';
export * from './invoices';
export * from './activityLogs';
export * from './reports';

export { ApiError, fetchJson, fetchOptional, apiRequest } from './http';
