import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { usuarioApi } from '../api/apiClient';
import { FeedbackBanner, Logo, Button, Input, Card } from '../components/ui';
import { ArrowRight, Lock, Mail, ShieldCheck, Sparkles } from 'lucide-react';

export const AuthPage: React.FC = () => {
  const { login } = useAuth();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form states
  const [loginEmail, setLoginEmail] = useState('');
  const [loginSenha, setLoginSenha] = useState('');

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
    <div className="min-h-screen bg-surface-950 flex flex-col justify-center items-center px-4 sm:px-6 py-12 relative overflow-hidden">
      {/* Luzes de ambientação decorativas sutis */}
      <div className="absolute -top-40 -left-40 w-96 h-96 bg-brand-500/10 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute -bottom-40 -right-40 w-96 h-96 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />

      <FeedbackBanner
        feedback={error ? { type: 'error', message: error } : null}
        onClose={() => setError(null)}
      />

      <div className="w-full max-w-md space-y-7 relative z-10">
        {/* Header com Logo e Tagline */}
        <div className="text-center space-y-3 flex flex-col items-center">
          <Logo size={52} className="mb-1" />
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-surface-900/90 border border-surface-800 text-[11px] font-medium text-surface-300">
            <Sparkles className="w-3.5 h-3.5 text-brand-400" />
            <span>Sistema Operacional de Arenas & Quadras</span>
          </div>
        </div>

        {/* Card de Autenticação */}
        <Card variant="glass" className="p-7 sm:p-9 space-y-6">
          <div className="text-center space-y-1">
            <h2 className="text-xl font-bold text-white tracking-tight">
              Acesso à Plataforma
            </h2>
            <p className="text-xs text-surface-400">
              Digite seu e-mail e senha cadastrados para entrar
            </p>
          </div>

          <form onSubmit={handleLogin} className="space-y-4">
            <Input
              label="E-mail"
              type="email"
              required
              value={loginEmail}
              onChange={(e) => setLoginEmail(e.target.value)}
              placeholder="exemplo@dominio.com"
              leftIcon={<Mail className="w-4 h-4" />}
            />

            <Input
              label="Senha"
              type="password"
              required
              value={loginSenha}
              onChange={(e) => setLoginSenha(e.target.value)}
              placeholder="••••••••••••"
              leftIcon={<Lock className="w-4 h-4" />}
            />

            <Button
              type="submit"
              variant="primary"
              size="lg"
              isLoading={loading}
              className="w-full mt-2"
              rightIcon={<ArrowRight className="w-4 h-4" />}
            >
              Entrar na Conta
            </Button>
          </form>

          <div className="pt-4 border-t border-surface-800/80 text-center">
            <div className="flex items-center justify-center gap-1.5 text-xs text-surface-400">
              <ShieldCheck className="w-4 h-4 text-brand-400 shrink-0" />
              <span>Novas contas são geridas exclusivamente pelo Administrador</span>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
};

