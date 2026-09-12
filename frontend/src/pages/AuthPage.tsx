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
    <div className="relative min-h-screen w-full flex flex-col md:flex-row overflow-x-hidden bg-black select-none">
      {/* Imagem de Fundo Panorâmica */}
      <div className="absolute inset-0 z-0 overflow-hidden">
        <img
          src={loginBg}
          alt="Vista aérea de quadras esportivas"
          fetchPriority="high"
          loading="eager"
          className="w-full h-full object-cover object-center pointer-events-none select-none"
        />
        {/* Contraste suave e natural - Apple Dark Vignette */}
        <div className="absolute inset-0 bg-black/60 md:hidden pointer-events-none" />
        <div className="hidden md:block absolute inset-0 bg-gradient-to-r from-black/20 via-black/50 to-black pointer-events-none" />
      </div>

      {/* Lado Esquerdo: Espaço livre para a foto respirar */}
      <div className="flex-1 hidden md:flex flex-col justify-end p-12 relative z-10">
        <div className="max-w-md space-y-2">
          <span className="text-xs font-semibold tracking-wider text-white/50 uppercase">Plataforma de Reservas</span>
          <h2 className="text-3xl font-bold text-white tracking-[-0.03em]">
            Agilidade e precisão em cada partida.
          </h2>
        </div>
      </div>

      {/* Lado Direito: Painel Frosted Glass Apple */}
      <div className="relative z-20 w-full md:w-[460px] lg:w-[480px] min-h-screen flex flex-col justify-between bg-black/85 md:backdrop-blur-2xl border-t md:border-t-0 md:border-l border-white/[0.08] p-8 sm:p-12 shadow-2xl">
        <FeedbackBanner
          feedback={error ? { type: 'error', message: error } : null}
          onClose={() => setError(null)}
        />

        <div className="my-auto w-full max-w-sm mx-auto space-y-7">
          <div className="space-y-1.5 text-left">
            <h1 className="text-2xl sm:text-3xl font-bold text-white tracking-[-0.03em]">
              Entrar
            </h1>
            <p className="text-sm text-white/50 tracking-tight">
              Informe suas credenciais para acessar sua conta
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
                  className="p-1 text-white/40 hover:text-white transition-colors focus:outline-none cursor-pointer"
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
              Continuar
            </Button>
          </form>
        </div>

        <div className="pt-4 text-xs text-white/30 tracking-tight flex items-center justify-between">
          <span>eQuadras</span>
          <span>Ambiente seguro</span>
        </div>
      </div>
    </div>
  );
};

