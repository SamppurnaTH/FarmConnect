import apiClient from './client';
import type { Transaction, Payment, PaymentRequest } from '../types';

export const transactionsApi = {
  getTransactions: () =>
    apiClient.get<Transaction[]>('/transactions').then((r) => r.data),

  getTransaction: (id: string) =>
    apiClient.get<Transaction>(`/transactions/${id}`).then((r) => r.data),

  submitPayment: (transactionId: string, data: PaymentRequest) =>
    apiClient.post<string>(`/transactions/${transactionId}/payments`, data).then((r) => r.data),

  getPayment: (paymentId: string) =>
    apiClient.get<Payment>(`/payments/${paymentId}`).then((r) => r.data),

  getTotalValue: () =>
    apiClient.get<number>('/transactions/total-value').then((r) => r.data),
};
