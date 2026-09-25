import React, { useState } from 'react';
import { Sun, Moon } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { usuarioApi } from '../api/apiClient';
import { FeedbackBanner } from '../components/ui/FeedbackBanner';
import { SignInPage } from '../components/ui/SignIn';
import { useTema } from '../shared/theme/useTema';
import loginBg from '../assets/login-bg.jpg';

export const AuthPage: React.FC = () => {
  const { login } = useAuth();
  const { tema, alternar: alternarTema } = useTema();
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
      <button
        type="button"
        onClick={alternarTema}
        aria-label="Alternar tema claro/escuro"
        className="fixed top-4 right-4 z-50 p-2.5 rounded-xl border border-fg/20 bg-bg/80 backdrop-blur-md hover:bg-fg/10 text-fg shadow-lg transition cursor-pointer"
      >
        {tema === 'claro' ? <Moon className="size-4" aria-hidden="true" /> : <Sun className="size-4" aria-hidden="true" />}
      </button>
      <FeedbackBanner
        feedback={error ? { type: 'error', message: error } : null}
        onClose={() => setError(null)}
      />
      <SignInPage
        title={<span className="font-semibold text-fg tracking-tight">Entrar</span>}
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
