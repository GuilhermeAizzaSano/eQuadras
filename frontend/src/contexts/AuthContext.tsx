import React, { createContext, useContext, useState } from 'react';
import { Usuario } from '../types';

interface AuthContextType {
  user: Usuario | null;
  token: string | null;
  login: (user: Usuario, token: string) => void;
  logout: () => void;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<Usuario | null>(() => {
    const saved = localStorage.getItem('equadras_auth_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem('equadras_auth_token')
  );

  const login = (newUser: Usuario, newToken: string) => {
    setUser(newUser);
    setToken(newToken);
    localStorage.setItem('equadras_auth_user', JSON.stringify(newUser));
    localStorage.setItem('equadras_auth_token', newToken);
  };

  const logout = () => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('equadras_auth_user');
    localStorage.removeItem('equadras_auth_token');
  };

  const isAdmin = user?.role === 'ADMIN';

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAdmin }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth deve ser usado dentro de AuthProvider');
  }
  return context;
};
