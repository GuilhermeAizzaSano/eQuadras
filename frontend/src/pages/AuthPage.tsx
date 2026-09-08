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
  CalendarCheck2,
  Trophy,
  Zap,
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
          alt="Vista aérea de quadras poliesportivas, tênis e futebol"
          fetchPriority="high"
          loading="eager"
          className="w-full h-full object-cover object-center pointer-events-none select-none scale-[1.01]"
        />
        {/* Camadas de contraste cinematográfico */}
        <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/70 to-zinc-950/40 md:hidden pointer-events-none" />
        <div className="hidden md:block absolute inset-0 bg-gradient-to-r from-zinc-950/65 via-zinc-950/35 to-zinc-950/95 pointer-events-none" />
        <div className="hidden md:block absolute inset-0 bg-[radial-gradient(ellipse_at_top_left,rgba(16,185,129,0.1),transparent_50%)] pointer-events-none" />
      </div>

      {/* Lado Esquerdo: Identidade Visual e Ambientação Esportiva (Desktop) */}
      <div className="relative z-10 flex-1 hidden md:flex flex-col justify-center p-10 lg:p-14 xl:p-16 text-white min-h-screen">
        {/* Centro: Chamada de Impacto */}
        <div className="max-w-xl space-y-6">
          <h1 className="text-3xl sm:text-4xl lg:text-5xl font-extrabold tracking-tight leading-[1.15] text-white drop-shadow-md">
            Alta performance para <br />
            <span className="bg-gradient-to-r from-brand-400 via-emerald-300 to-teal-200 bg-clip-text text-transparent">
              suas partidas e quadras.
            </span>
          </h1>

          <p className="text-sm sm:text-base text-surface-300 max-w-lg leading-relaxed drop-shadow">
            Agende horários em segundos, controle a ocupação em tempo real e gerencie sua arena com máxima eficiência.
          </p>

          {/* Destaques Rápidos */}
          <div className="grid grid-cols-3 gap-3 pt-2">
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-surface-950/70 border border-white/10 backdrop-blur-md shadow-md">
              <CalendarCheck2 className="w-4 h-4 text-brand-400 shrink-0" />
              <span className="text-xs font-medium text-surface-200">Reserva Ágil</span>
            </div>
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-surface-950/70 border border-white/10 backdrop-blur-md shadow-md">
              <Trophy className="w-4 h-4 text-amber-400 shrink-0" />
              <span className="text-xs font-medium text-surface-200">Multiesportes</span>
            </div>
            <div className="flex items-center gap-2.5 p-3 rounded-xl bg-surface-950/70 border border-white/10 backdrop-blur-md shadow-md">
              <Zap className="w-4 h-4 text-sky-400 shrink-0" />
              <span className="text-xs font-medium text-surface-200">PIX Imediato</span>
            </div>
          </div>
        </div>
      </div>

      {/* Lado Direito: Formulário Exclusivo de Login */}
      <div className="relative z-20 w-full md:w-[460px] lg:w-[500px] xl:w-[540px] min-h-screen flex flex-col justify-between bg-surface-950/90 md:bg-surface-950/85 backdrop-blur-xl md:backdrop-blur-2xl border-t md:border-t-0 md:border-l border-white/10 p-6 sm:p-10 lg:p-12 shadow-[-20px_0_60px_rgba(0,0,0,0.85)]">
        <FeedbackBanner
          feedback={error ? { type: 'error', message: error } : null}
          onClose={() => setError(null)}
        />

        <div className="my-auto w-full max-w-md mx-auto space-y-7 py-6">
          <div className="space-y-1 text-left">
            <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              Entrar na Conta
            </h2>
            <p className="text-xs sm:text-sm text-surface-400 mt-1">
              Informe seu e-mail e senha para acessar o painel
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
                  className="p-1 text-surface-400 hover:text-white transition-colors focus:outline-none"
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
              className="w-full mt-2 shadow-lg shadow-brand-500/20"
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Entrar na Conta
            </Button>
          </form>
        </div>

        {/* Copyright */}
        <div className="pt-4 text-center md:text-left text-[11px] text-surface-500">
          © {new Date().getFullYear()} eQuadras. Todos os direitos reservados.
        </div>
      </div>
    </div>
  );
};

