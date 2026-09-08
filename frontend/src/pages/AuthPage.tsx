import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { usuarioApi } from '../api/apiClient';
import { FeedbackBanner, Button, Input } from '../components/ui';
import {
  ArrowRight,
  Lock,
  Mail,
  Eye,
  EyeOff,
} from 'lucide-react';
import loginBg from '../assets/login-bg.jpg';

export const AuthPage: React.FC = () => {
  const { login } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form states
  const [loginEmail, setLoginEmail] = useState('');
  const [loginSenha, setLoginSenha] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const resposta = await usuarioApi.login(loginEmail, loginSenha);
      login(resposta.usuario, resposta.token);
    } catch (err: any) {
      setError(err.message || 'Falha ao autenticar');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative min-h-screen w-full flex flex-col md:flex-row overflow-x-hidden bg-zinc-950 select-none">
      {/* Imagem de Fundo Panorâmica */}
      <div className="absolute inset-0 z-0 overflow-hidden">
        <img
          src={loginBg}
          alt="Vista aérea de quadras esportivas"
          fetchPriority="high"
          loading="eager"
          className="w-full h-full object-cover object-center pointer-events-none select-none"
        />
        {/* Contraste suave e natural */}
        <div className="absolute inset-0 bg-black/60 md:hidden pointer-events-none" />
        <div className="hidden md:block absolute inset-0 bg-gradient-to-r from-black/20 via-black/40 to-black/90 pointer-events-none" />
      </div>

      {/* Lado Esquerdo: Espaço livre para a foto das quadras respirar */}
      <div className="flex-1 hidden md:block" />

      {/* Lado Direito: Formulário Direto de Login */}
      <div className="relative z-20 w-full md:w-[440px] lg:w-[480px] min-h-screen flex flex-col justify-between bg-zinc-950/95 md:bg-zinc-950/90 md:backdrop-blur-xl border-t md:border-t-0 md:border-l border-zinc-800 p-8 sm:p-12">
        <FeedbackBanner
          feedback={error ? { type: 'error', message: error } : null}
          onClose={() => setError(null)}
        />

        <div className="my-auto w-full max-w-sm mx-auto space-y-6">
          <div className="space-y-1 text-left">
            <h1 className="text-2xl font-bold text-white tracking-tight">
              Entrar
            </h1>
            <p className="text-sm text-zinc-400">
              Informe seu e-mail e senha para acessar a plataforma
            </p>
          </div>

          <form onSubmit={handleLogin} className="space-y-4">
            <Input
              label="E-mail"
              type="email"
              autoComplete="email"
              required
              value={loginEmail}
              onChange={(e) => setLoginEmail(e.target.value)}
              placeholder="exemplo@dominio.com"
              leftIcon={<Mail className="w-4 h-4" />}
            />

            <Input
              label="Senha"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              required
              value={loginSenha}
              onChange={(e) => setLoginSenha(e.target.value)}
              placeholder="••••••••••••"
              leftIcon={<Lock className="w-4 h-4" />}
              rightElement={
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="p-1 text-zinc-400 hover:text-white transition-colors focus:outline-none"
                  title={showPassword ? 'Ocultar senha' : 'Exibir senha'}
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              }
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={loading}
              className="w-full mt-2"
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Entrar
            </Button>
          </form>
        </div>

        <div className="pt-4 text-xs text-zinc-500">
          eQuadras
        </div>
      </div>
    </div>
  );
};

