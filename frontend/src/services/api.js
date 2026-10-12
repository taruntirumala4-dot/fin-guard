import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('finguard_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for token expiration / unauthorized / forbidden
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      const isStandby = localStorage.getItem('finguard_standby') === 'true';
      if (!isStandby) {
        localStorage.removeItem('finguard_token');
        localStorage.removeItem('finguard_user');
        if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);

export const checkBackendHealth = async () => {
  try {
    const res = await axios.get(`${API_BASE_URL}/api/health`, { timeout: 2500 });
    return res.data && res.data.status === 'UP';
  } catch (e) {
    return false;
  }
};

export default api;
