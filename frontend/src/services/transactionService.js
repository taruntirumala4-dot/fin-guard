import api from './api';

export const transactionService = {
  createTransaction: async (data) => {
    const response = await api.post('/api/transactions', data);
    return response.data;
  },

  getAllTransactions: async (page = 0, size = 20) => {
    const response = await api.get(`/api/transactions?page=${page}&size=${size}`);
    return response.data;
  },

  getTransactionById: async (id) => {
    const response = await api.get(`/api/transactions/${id}`);
    return response.data;
  },

  getUserTransactions: async (userId, page = 0, size = 20) => {
    const response = await api.get(`/api/transactions/user/${userId}?page=${page}&size=${size}`);
    return response.data;
  },

  searchTransactions: async (params) => {
    const query = new URLSearchParams(params).toString();
    const response = await api.get(`/api/transactions/search?${query}`);
    return response.data;
  },

  getDashboardSummary: async () => {
    const response = await api.get('/api/dashboard/summary');
    return response.data;
  },

  getTransactionStats: async () => {
    const response = await api.get('/api/dashboard/transaction-stats');
    return response.data;
  },
};
