import React, { useState } from 'react';
import { X, Lock, Check, AlertCircle, Eye, EyeOff } from 'lucide-react';
import { usuarioApi } from '../../api/apiClient';

interface ChangePasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export const ChangePasswordModal: React.FC<ChangePasswordModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const [senhaAtual, setSenhaAtual] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');

  const [mostrarSenhaAtual, setMostrarSenhaAtual] = useState(false);
  const [mostrarNovaSenha, setMostrarNovaSenha] = useState(false);

  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState(false);

  // Critérios de validação de senha:
  // - mínimo 6 caracteres
  // - pelo menos 1 maiúscula
  // - pelo menos 1 minúscula
  // - pelo menos 1 número
  // - pelo menos 1 símbolo
  const temTamanhoMinimo = novaSenha.length >= 6;
  const temMaiuscula = /[A-Z]/.test(novaSenha);
  const temMinuscula = /[a-z]/.test(novaSenha);
  const temNumero = /\d/.test(novaSenha);
  const temSimbolo = /[^a-zA-Z0-9]/.test(novaSenha);
  const senhasConferem = novaSenha.length > 0 && novaSenha === confirmarSenha;

  const formValido =
    senhaAtual.length > 0 &&
    temTamanhoMinimo &&
    temMaiuscula &&
    temMinuscula &&
    temNumero &&
    temSimbolo &&
    senhasConferem;

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErro(null);

    if (!formValido) {
      setErro('Preencha todos os requisitos de segurança da nova senha.');
      return;
    }

    setLoading(true);
    try {
      await usuarioApi.alterarMinhaSenha({
        senhaAtual,
        novaSenha,
      });

      setSucesso(true);
      setTimeout(() => {
        setSucesso(false);
        setSenhaAtual('');
        setNovaSenha('');
        setConfirmarSenha('');
        onClose();
        if (onSuccess) onSuccess();
      }, 1500);
    } catch (err: any) {
      setErro(err.message || 'Falha ao alterar senha.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-zinc-950 border border-zinc-800 rounded-3xl max-w-md w-full p-6 space-y-5 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-zinc-400 hover:text-white hover:bg-zinc-900 transition"
          title="Fechar"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="space-y-1">
          <div className="inline-flex items-center justify-center w-10 h-10 rounded-2xl bg-zinc-900 border border-zinc-800 text-white mb-1">
            <Lock className="w-5 h-5 text-zinc-300" />
          </div>
          <h3 className="text-lg font-bold text-white tracking-tight">Alterar Senha</h3>
          <p className="text-xs text-zinc-400">
            Atualize sua senha de acesso definindo uma combinação segura.
          </p>
        </div>

        {erro && (
          <div className="p-3 rounded-xl bg-red-950/40 border border-red-900/60 text-red-400 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{erro}</span>
          </div>
        )}

        {sucesso && (
          <div className="p-3 rounded-xl bg-emerald-950/40 border border-emerald-900/60 text-emerald-400 text-xs flex items-center gap-2">
            <Check className="w-4 h-4 shrink-0" />
            <span>Senha alterada com sucesso!</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3.5">
          <div>
            <label className="block text-xs font-medium text-zinc-300 mb-1">
              Senha Atual
            </label>
            <div className="relative">
              <input
                type={mostrarSenhaAtual ? 'text' : 'password'}
                required
                value={senhaAtual}
                onChange={(e) => setSenhaAtual(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-zinc-900/80 border border-zinc-800 rounded-xl pl-3 pr-10 py-2 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-white transition"
              />
              <button
                type="button"
                onClick={() => setMostrarSenhaAtual(!mostrarSenhaAtual)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300"
              >
                {mostrarSenhaAtual ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-zinc-300 mb-1">
              Nova Senha
            </label>
            <div className="relative">
              <input
                type={mostrarNovaSenha ? 'text' : 'password'}
                required
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-zinc-900/80 border border-zinc-800 rounded-xl pl-3 pr-10 py-2 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-white transition"
              />
              <button
                type="button"
                onClick={() => setMostrarNovaSenha(!mostrarNovaSenha)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-zinc-500 hover:text-zinc-300"
              >
                {mostrarNovaSenha ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-zinc-300 mb-1">
              Confirmar Nova Senha
            </label>
            <input
              type="password"
              required
              value={confirmarSenha}
              onChange={(e) => setConfirmarSenha(e.target.value)}
              placeholder="••••••••"
              className="w-full bg-zinc-900/80 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-white transition"
            />
          </div>

          {/* Checklist de Requisitos de Senha */}
          <div className="p-3 rounded-2xl bg-zinc-900/60 border border-zinc-800/80 space-y-1.5 text-[11px]">
            <span className="font-semibold text-zinc-400 block mb-1">Requisitos de segurança:</span>
            <div className="grid grid-cols-2 gap-x-2 gap-y-1 text-zinc-400">
              <div className={`flex items-center gap-1.5 ${temTamanhoMinimo ? 'text-emerald-400' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temTamanhoMinimo ? 'text-emerald-400' : 'text-zinc-600'}`} />
                <span>Mínimo 6 caracteres</span>
              </div>
              <div className={`flex items-center gap-1.5 ${temMaiuscula ? 'text-emerald-400' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temMaiuscula ? 'text-emerald-400' : 'text-zinc-600'}`} />
                <span>1 letra maiúscula</span>
              </div>
              <div className={`flex items-center gap-1.5 ${temMinuscula ? 'text-emerald-400' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temMinuscula ? 'text-emerald-400' : 'text-zinc-600'}`} />
                <span>1 letra minúscula</span>
              </div>
              <div className={`flex items-center gap-1.5 ${temNumero ? 'text-emerald-400' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temNumero ? 'text-emerald-400' : 'text-zinc-600'}`} />
                <span>1 número</span>
              </div>
              <div className={`flex items-center gap-1.5 ${temSimbolo ? 'text-emerald-400' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temSimbolo ? 'text-emerald-400' : 'text-zinc-600'}`} />
                <span>1 símbolo especial</span>
              </div>
              <div className={`flex items-center gap-1.5 ${senhasConferem ? 'text-emerald-400' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${senhasConferem ? 'text-emerald-400' : 'text-zinc-600'}`} />
                <span>Senhas coincidem</span>
              </div>
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-2 border-t border-zinc-850">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-medium text-zinc-400 hover:text-white rounded-xl transition"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading || !formValido}
              className="bg-white hover:bg-zinc-200 text-black font-semibold text-xs py-2 px-4 rounded-xl transition flex items-center justify-center gap-2 disabled:opacity-40 disabled:hover:bg-white"
            >
              {loading ? 'Salvando...' : 'Atualizar Senha'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
