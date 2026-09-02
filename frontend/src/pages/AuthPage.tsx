import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { usuarioApi } from '../api/apiClient';
import { Role } from '../types';
import { FeedbackBanner, Logo } from '../components/ui';
import { ArrowRight, Lock, Mail, Phone, User, ShieldCheck } from 'lucide-react';

export const AuthPage: React.FC = () => {
  const { login } = useAuth();
  const [tab, setTab] = useState<'login' | 'register'>('login');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form states
  const [loginEmail, setLoginEmail] = useState('');
  const [loginSenha, setLoginSenha] = useState('');

  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [phone, setPhone] = useState('');
  const [role, setRole] = useState<Role>('CLIENT');

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

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      const resposta = await usuarioApi.cadastrar({
        nome_usuario: nome,
        email_usuario: email,
        senha_usuario: senha,
        phone_usuario: phone,
      });
      login(resposta.usuario, resposta.token);
    } catch (err: any) {
      setError(err.message || 'Falha ao cadastrar');
    } finally {
      setLoading(false);
    }
  };

  const handlePhoneChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let val = e.target.value.replace(/\D/g, ''); // apenas números
    if (val.length > 11) val = val.substring(0, 11); // limite 11 dígitos

    // formatação: (XX) XXXXX-XXXX ou (XX) XXXX-XXXX
    let formatted = val;
    if (val.length > 2) {
      formatted = `(${val.substring(0, 2)}) `;
      if (val.length > 7) {
        formatted += `${val.substring(2, 7)}-${val.substring(7)}`;
      } else {
        formatted += val.substring(2);
      }
    }
    setPhone(formatted);
  };

  return (
    <div className="min-h-screen bg-black flex flex-col justify-center items-center px-4 sm:px-6 py-12">
      <div className="w-full max-w-md space-y-6">
        {/* Logo & Headline */}
        <div className="text-center space-y-2 flex flex-col items-center">
          <Logo size={48} className="mb-1" />
          <p className="text-xs text-zinc-400">
            Plataforma intuitiva de agendamento e locação de quadras
          </p>
        </div>

        {/* Auth Box */}
        <div className="bg-zinc-950 border border-zinc-800/80 rounded-2xl p-6 sm:p-8 shadow-2xl backdrop-blur-xl">
          {/* Tabs */}
          <div className="flex bg-zinc-900 p-1 rounded-xl mb-6 border border-zinc-800">
            <button
              onClick={() => { setTab('login'); setError(null); }}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${
                tab === 'login'
                  ? 'bg-white text-black shadow-md'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              Entrar
            </button>
            <button
              onClick={() => { setTab('register'); setError(null); }}
              className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-all ${
                tab === 'register'
                  ? 'bg-white text-black shadow-md'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              Criar Conta
            </button>
          </div>

          <div className="mb-4">
            <FeedbackBanner
              feedback={error ? { type: 'error', message: error } : null}
              onClose={() => setError(null)}
            />
          </div>

          {tab === 'login' ? (
            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1.5">
                  E-mail
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    required
                    value={loginEmail}
                    onChange={(e) => setLoginEmail(e.target.value)}
                    placeholder="exemplo@dominio.com"
                    className="w-full bg-zinc-900/80 border border-zinc-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-white focus:ring-1 focus:ring-white transition"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1.5">
                  Senha
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="password"
                    required
                    value={loginSenha}
                    onChange={(e) => setLoginSenha(e.target.value)}
                    placeholder="••••••••"
                    className="w-full bg-zinc-900/80 border border-zinc-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-white focus:ring-1 focus:ring-white transition"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full mt-2 bg-white hover:bg-zinc-200 text-black font-medium py-2.5 px-4 rounded-lg text-sm transition flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {loading ? 'Entrando...' : 'Acessar Conta'}
                <ArrowRight className="w-4 h-4" />
              </button>
            </form>
          ) : (
            <form onSubmit={handleRegister} className="space-y-3.5">
              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1.5">
                  Nome Completo
                </label>
                <div className="relative">
                  <User className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    required
                    value={nome}
                    onChange={(e) => setNome(e.target.value)}
                    placeholder="Nome completo"
                    className="w-full bg-zinc-900/80 border border-zinc-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-white focus:ring-1 focus:ring-white transition"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1.5">
                  E-mail
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="exemplo@dominio.com"
                    className="w-full bg-zinc-900/80 border border-zinc-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-white focus:ring-1 focus:ring-white transition"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-zinc-300 mb-1.5">
                    Telefone
                  </label>
                  <div className="relative">
                    <Phone className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      required
                      value={phone}
                      onChange={handlePhoneChange}
                      placeholder="(11) 99999-9999"
                      className="w-full bg-zinc-900/80 border border-zinc-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-white focus:ring-1 focus:ring-white transition"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-medium text-zinc-300 mb-1.5">
                    Perfil
                  </label>
                  <div className="relative">
                    <ShieldCheck className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                    <select
                      value={role}
                      onChange={(e) => setRole(e.target.value as Role)}
                      className="w-full bg-zinc-900/80 border border-zinc-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white focus:outline-none focus:border-white focus:ring-1 focus:ring-white transition"
                    >
                      <option value="CLIENT">Cliente</option>
                      <option value="ADMIN">Administrador</option>
                    </select>
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-300 mb-1.5">
                  Senha
                </label>
                <div className="relative">
                  <Lock className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="password"
                    required
                    value={senha}
                    onChange={(e) => setSenha(e.target.value)}
                    placeholder="••••••••"
                    className="w-full bg-zinc-900/80 border border-zinc-800 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-zinc-500 focus:outline-none focus:border-white focus:ring-1 focus:ring-white transition"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full mt-3 bg-white hover:bg-zinc-200 text-black font-medium py-2.5 px-4 rounded-lg text-sm transition flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {loading ? 'Criando conta...' : 'Concluir Cadastro'}
                <ArrowRight className="w-4 h-4" />
              </button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};
