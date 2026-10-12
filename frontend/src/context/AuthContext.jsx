import React, { createContext, useState, useEffect } from 'react';
import { authService } from '../services/authService';
import { checkBackendHealth } from '../services/api';

export const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isBackendConnected, setIsBackendConnected] = useState(false);
  const [isCheckingBackend, setIsCheckingBackend] = useState(true);

  const verifyHealth = async () => {
    const isLive = await checkBackendHealth();
    setIsBackendConnected(isLive);
    setIsCheckingBackend(false);
    return isLive;
  };

  useEffect(() => {
    const savedUser = authService.getCurrentUser();
    if (savedUser) {
      setUser(savedUser);
    }
    setLoading(false);

    verifyHealth();
    const interval = setInterval(verifyHealth, 4000);
    return () => clearInterval(interval);
  }, []);

  const login = async (email, password) => {
    localStorage.removeItem('finguard_standby');
    const data = await authService.login(email, password);
    setUser(data);
    return data;
  };

  const loginStandby = (role = 'ADMIN', email = 'admin@finguard.io', name = 'Demo Admin') => {
    const data = authService.loginStandby(role, email, name);
    setUser(data);
    return data;
  };

  const register = async (name, email, password, confirmPassword) => {
    localStorage.removeItem('finguard_standby');
    const data = await authService.register(name, email, password, confirmPassword);
    setUser(data);
    return data;
  };

  const logout = () => {
    authService.logout();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{
      user,
      login,
      loginStandby,
      register,
      logout,
      loading,
      isBackendConnected,
      isCheckingBackend,
      recheckBackend: verifyHealth,
    }}>
      {children}
    </AuthContext.Provider>
  );
};
