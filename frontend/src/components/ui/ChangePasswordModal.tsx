import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { X, Lock, Check, AlertCircle, Eye, EyeOff } from 'lucide-react';
import { usuarioApi } from '../../api/apiClient';
import { Button } from './Button';

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

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xl overflow-y-auto animate-in fade-in duration-200"
    >
      <div className="bg-[#121214] border border-white/[0.1] rounded-3xl max-w-md w-full p-6 sm:p-7 space-y-5 shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.08)] relative my-auto max-h-[90vh] overflow-y-auto animate-in zoom-in-95 duration-200">
        <button
          type="button"
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-white/40 hover:text-white hover:bg-white/[0.08] transition cursor-pointer"
          aria-label="Fechar"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="space-y-1">
          <div className="inline-flex items-center justify-center w-11 h-11 rounded-2xl bg-white/[0.06] border border-white/[0.1] text-white mb-1 shadow-sm">
            <Lock className="w-5 h-5 text-white/80" />
          </div>
          <h3 className="text-lg font-semibold text-white tracking-tight">Alterar Senha</h3>
          <p className="text-xs text-white/50 tracking-tight">
            Atualize sua senha de acesso definindo uma combinação segura.
          </p>
        </div>

        {erro && (
          <div className="p-3.5 rounded-xl bg-[#FF453A]/10 border border-[#FF453A]/25 text-[#FF453A] text-xs flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-[#FF453A] shrink-0" />
            <span>{erro}</span>
          </div>
        )}

        {sucesso && (
          <div className="p-3.5 rounded-xl bg-[#30D158]/10 border border-[#30D158]/25 text-[#30D158] text-xs flex items-center gap-2.5">
            <Check className="w-4 h-4 text-[#30D158] shrink-0" />
            <span>Senha alterada com sucesso!</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-white/70 tracking-tight">
              Senha Atual
            </label>
            <div className="relative">
              <input
                type={mostrarSenhaAtual ? 'text' : 'password'}
                required
                value={senhaAtual}
                onChange={(e) => setSenhaAtual(e.target.value)}
                placeholder="••••••••••••"
                className="w-full bg-white/[0.04] border border-white/[0.08] rounded-xl pl-3.5 pr-11 py-2.5 text-xs sm:text-sm text-white placeholder-white/30 focus:outline-none focus:border-white/30 focus:ring-2 focus:ring-white/10 transition"
              />
              <button
                type="button"
                onClick={() => setMostrarSenhaAtual(!mostrarSenhaAtual)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white p-1 cursor-pointer"
                aria-label="Alternar visualização da senha"
              >
                {mostrarSenhaAtual ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-white/70 tracking-tight">
              Nova Senha
            </label>
            <div className="relative">
              <input
                type={mostrarNovaSenha ? 'text' : 'password'}
                required
                value={novaSenha}
                onChange={(e) => setNovaSenha(e.target.value)}
                placeholder="••••••••••••"
                className="w-full bg-white/[0.04] border border-white/[0.08] rounded-xl pl-3.5 pr-11 py-2.5 text-xs sm:text-sm text-white placeholder-white/30 focus:outline-none focus:border-white/30 focus:ring-2 focus:ring-white/10 transition"
              />
              <button
                type="button"
                onClick={() => setMostrarNovaSenha(!mostrarNovaSenha)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white p-1 cursor-pointer"
                aria-label="Alternar visualização da senha"
              >
                {mostrarNovaSenha ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="block text-xs font-medium text-white/70 tracking-tight">
              Confirmar Nova Senha
            </label>
            <input
              type="password"
              required
              value={confirmarSenha}
              onChange={(e) => setConfirmarSenha(e.target.value)}
              placeholder="••••••••••••"
              className="w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-3.5 py-2.5 text-xs sm:text-sm text-white placeholder-white/30 focus:outline-none focus:border-white/30 focus:ring-2 focus:ring-white/10 transition"
            />
          </div>

          {/* Checklist de Requisitos de Senha */}
          <div className="p-3.5 rounded-2xl bg-white/[0.03] border border-white/[0.06] space-y-2 text-[11px]">
            <span className="font-medium text-white/50 block">Requisitos de segurança:</span>
            <div className="grid grid-cols-2 gap-x-2.5 gap-y-1.5 text-white/60">
              <div className={`flex items-center gap-1.5 ${temTamanhoMinimo ? 'text-[#30D158]' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temTamanhoMinimo ? 'text-[#30D158]' : 'text-white/20'}`} />
                <span>Mínimo 6 caracteres</span>
              </div>
              <div className={`flex items-center gap-1.5 ${temMaiuscula ? 'text-[#30D158]' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temMaiuscula ? 'text-[#30D158]' : 'text-white/20'}`} />
                <span>1 letra maiúscula</span>
              </div>
              <div className={`flex items-center gap-1.5 ${temMinuscula ? 'text-[#30D158]' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temMinuscula ? 'text-[#30D158]' : 'text-white/20'}`} />
                <span>1 letra minúscula</span>
              </div>
              <div className={`flex items-center gap-1.5 ${temNumero ? 'text-[#30D158]' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temNumero ? 'text-[#30D158]' : 'text-white/20'}`} />
                <span>1 número</span>
              </div>
              <div className={`flex items-center gap-1.5 ${temSimbolo ? 'text-[#30D158]' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${temSimbolo ? 'text-[#30D158]' : 'text-white/20'}`} />
                <span>1 símbolo especial</span>
              </div>
              <div className={`flex items-center gap-1.5 ${senhasConferem ? 'text-[#30D158]' : ''}`}>
                <Check className={`w-3.5 h-3.5 ${senhasConferem ? 'text-[#30D158]' : 'text-white/20'}`} />
                <span>Senhas coincidem</span>
              </div>
            </div>
          </div>

          <div className="flex items-center justify-end gap-3 pt-3 border-t border-white/[0.08]">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={onClose}
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={loading}
              disabled={!formValido}
            >
              Atualizar Senha
            </Button>
          </div>
        </form>
      </div>
    </div>,
    document.body
  );
};
