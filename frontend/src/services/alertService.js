import api from './api';

export const alertService = {
  getAlerts: async (page = 0, size = 20) => {
    const response = await api.get(`/api/fraud/alerts?page=${page}&size=${size}`);
    return response.data;
  },

  getAlertById: async (id) => {
    const response = await api.get(`/api/fraud/alerts/${id}`);
    return response.data;
  },

  getStatistics: async () => {
    const response = await api.get('/api/fraud/statistics');
    return response.data;
  },

  getRecentAlerts: async () => {
    const response = await api.get('/api/fraud/alerts/recent');
    return response.data;
  },

  analyzeTransaction: async (transactionId) => {
    const response = await api.post(`/api/fraud/analyze/${transactionId}`);
    return response.data;
  },
};
