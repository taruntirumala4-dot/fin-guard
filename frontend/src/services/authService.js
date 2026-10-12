import api from './api';

export const authService = {
  login: async (email, password) => {
    const response = await api.post('/api/auth/login', { email, password });
    if (response.data.token) {
      localStorage.setItem('finguard_token', response.data.token);
      localStorage.setItem('finguard_user', JSON.stringify(response.data));
    }
    return response.data;
  },

  register: async (name, email, password, confirmPassword) => {
    const response = await api.post('/api/auth/register', {
      name,
      email,
      password,
      confirmPassword,
    });
    if (response.data.token) {
      localStorage.setItem('finguard_token', response.data.token);
      localStorage.setItem('finguard_user', JSON.stringify(response.data));
    }
    return response.data;
  },

  loginStandby: (role = 'ADMIN', email = 'admin@finguard.io', name = 'Demo Admin') => {
    const mockUser = {
      token: 'mock-standby-jwt-token-' + Date.now(),
      id: '1',
      name: name,
      email: email,
      role: role,
      expiresIn: 86400000,
      isStandby: true,
    };
    localStorage.setItem('finguard_token', mockUser.token);
    localStorage.setItem('finguard_user', JSON.stringify(mockUser));
    localStorage.setItem('finguard_standby', 'true');
    return mockUser;
  },

  logout: () => {
    localStorage.removeItem('finguard_token');
    localStorage.removeItem('finguard_user');
    localStorage.removeItem('finguard_standby');
  },

  getCurrentUser: () => {
    const userStr = localStorage.getItem('finguard_user');
    if (userStr) {
      try {
        return JSON.parse(userStr);
      } catch (e) {
        return null;
      }
    }
    return null;
  },

  fetchProfile: async () => {
    if (localStorage.getItem('finguard_standby') === 'true') {
      const user = authService.getCurrentUser();
      return {
        id: user?.id || '1',
        name: user?.name || 'Demo User',
        email: user?.email || 'admin@finguard.io',
        role: user?.role || 'ADMIN',
        createdAt: new Date().toISOString(),
        isStandby: true,
      };
    }
    const response = await api.get('/api/auth/me');
    return response.data;
  },
};
