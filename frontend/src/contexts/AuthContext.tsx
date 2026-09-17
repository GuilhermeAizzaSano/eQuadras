import React, { createContext, useContext, useState, useEffect } from 'react';
import { Usuario } from '../types';
import { usuarioApi, setUnauthorizedCallback } from '../api/apiClient';

interface AuthContextType {
  user: Usuario | null;
  loadingAuth: boolean;
  login: (user: Usuario) => void;
  logout: () => Promise<void>;
  isAdmin: boolean;
  isMasterAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<Usuario | null>(() => {
    const saved = localStorage.getItem('equadras_auth_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [loadingAuth, setLoadingAuth] = useState(true);

  useEffect(() => {
    // Limpeza obrigatória de resíduos de tokens legados no localStorage
    localStorage.removeItem('equadras_auth_token');

    // Callback invocado automaticamente quando qualquer requisição receber 401
    setUnauthorizedCallback(() => {
      setUser(null);
      localStorage.removeItem('equadras_auth_user');
    });

    // Se não há usuário previamente autenticado salvo, não dispara chamada desnecessária que geraria 401
    const savedUser = localStorage.getItem('equadras_auth_user');
    if (!savedUser) {
      setLoadingAuth(false);
      return;
    }

    const controller = new AbortController();

    // Valida sessão ativa com o backend via cookie HttpOnly
    usuarioApi
      .me(controller.signal)
      .then((usuarioAtual) => {
        setUser(usuarioAtual);
        localStorage.setItem('equadras_auth_user', JSON.stringify(usuarioAtual));
      })
      .catch((err) => {
        if (err.name !== 'AbortError') {
          setUser(null);
          localStorage.removeItem('equadras_auth_user');
        }
      })
      .finally(() => {
        setLoadingAuth(false);
      });

    return () => {
      controller.abort();
    };
  }, []);

  const login = (newUser: Usuario) => {
    setUser(newUser);
    localStorage.setItem('equadras_auth_user', JSON.stringify(newUser));
  };

  const logout = async () => {
    try {
      await usuarioApi.logout();
    } catch {
      // Ignora erro de rede no logout
    } finally {
      setUser(null);
      localStorage.removeItem('equadras_auth_user');
      localStorage.removeItem('equadras_auth_token');
    }
  };

  const isAdmin = user?.role === 'ADMIN';
  const isMasterAdmin = user?.role === 'ADMIN' && (user?.masterAdmin === true || user?.email_usuario?.toLowerCase() === 'gui@gmail.com');

  return (
    <AuthContext.Provider value={{ user, loadingAuth, login, logout, isAdmin, isMasterAdmin }}>
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
