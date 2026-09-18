import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { usuarioApi } from '../api/apiClient';
import { FeedbackBanner } from '../components/ui/FeedbackBanner';
import { SignInPage } from '../components/ui/sign-in';
import loginBg from '../assets/login-bg.jpg';

export const AuthPage: React.FC = () => {
  const { login } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [loginEmail, setLoginEmail] = useState('');
  const [loginSenha, setLoginSenha] = useState('');

  const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const resposta = await usuarioApi.login(loginEmail, loginSenha);
      login(resposta);
    } catch (err: any) {
      setError(err.message || 'Falha ao autenticar');
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <FeedbackBanner
        feedback={error ? { type: 'error', message: error } : null}
        onClose={() => setError(null)}
      />
      <SignInPage
        title={<span className="font-semibold text-white tracking-tight">Entrar</span>}
        description="Informe suas credenciais para acessar sua conta no eQuadras"
        heroImageSrc={loginBg}
        email={loginEmail}
        password={loginSenha}
        onEmailChange={setLoginEmail}
        onPasswordChange={setLoginSenha}
        onSubmit={handleLogin}
        isLoading={loading}
      />
    </>
  );
};
