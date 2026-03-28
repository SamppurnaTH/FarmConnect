import apiClient from './client';
import type { CropListing, CreateListingRequest, Order } from '../types';

export interface ListingFilters {
  cropType?: string;
  location?: string;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
}

export const cropsApi = {
  createListing: (data: CreateListingRequest) =>
    apiClient.post<string>('/listings', data).then((r) => r.data),

  getListings: (filters: ListingFilters = {}) =>
    apiClient.get<CropListing[]>('/listings', { params: filters }).then((r) => r.data),

  getListing: (id: string) =>
    apiClient.get<CropListing>(`/listings/${id}`).then((r) => r.data),

  updateListing: (id: string, data: Partial<CreateListingRequest> & { status?: string; rejectionReason?: string }) =>
    apiClient.put<void>(`/listings/${id}`, data),

  getMyListings: (farmerId: string) =>
    apiClient.get<CropListing[]>('/listings', { params: { farmerId } }).then((r) => r.data),

  placeOrder: (listingId: string, quantity: number) =>
    apiClient.post<string>(`/listings/${listingId}/orders`, { quantity }).then((r) => r.data),

  getOrdersForListing: (listingId: string) =>
    apiClient.get<Order[]>(`/listings/${listingId}/orders`).then((r) => r.data),

  acceptOrder: (orderId: string) =>
    apiClient.put<void>(`/orders/${orderId}/accept`),

  declineOrder: (orderId: string) =>
    apiClient.put<void>(`/orders/${orderId}/decline`),

  getTraderOrders: (traderId: string) =>
    apiClient.get<Order[]>('/orders', { params: { traderId } }).then((r) => r.data),

  getTotalVolume: () =>
    apiClient.get<number>('/listings/total-volume').then((r) => r.data),
};
