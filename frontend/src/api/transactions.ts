import apiClient from './client';
import type { Transaction, Payment, PaymentRequest } from '../types';

export const transactionsApi = {
  /**
   * GET /transactions — all transactions (Market Officer / Admin / Auditor).
   */
  getTransactions: () =>
    apiClient.get<Transaction[]>('/transactions').then((r) => r.data),

  /**
   * GET /transactions?orderIds=id1,id2,...
   * Returns only transactions whose orderId is in the provided set.
   * Used by Farmer and Trader to see only their own transactions.
   */
  getTransactionsByOrderIds: (orderIds: string[]) =>
    apiClient
      .get<Transaction[]>('/transactions', { params: { orderIds: orderIds.join(',') } })
      .then((r) => r.data),

  getTransaction: (id: string) =>
    apiClient.get<Transaction>(`/transactions/${id}`).then((r) => r.data),

  submitPayment: (transactionId: string, data: PaymentRequest) =>
    apiClient.post<string>(`/transactions/${transactionId}/payments`, data).then((r) => r.data),

  getPayment: (paymentId: string) =>
    apiClient.get<Payment>(`/payments/${paymentId}`).then((r) => r.data),

  getTotalValue: () =>
    apiClient.get<number>('/transactions/total-value').then((r) => r.data),
};
