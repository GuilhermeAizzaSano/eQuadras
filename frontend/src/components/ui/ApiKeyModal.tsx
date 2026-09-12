import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { X, KeyRound, Copy, Check, ShieldCheck, UserCheck, Terminal, Info } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { Button } from './Button';

interface ApiKeyModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ApiKeyModal: React.FC<ApiKeyModalProps> = ({ isOpen, onClose }) => {
  const { user, token, isAdmin } = useAuth();
  const [copiado, setCopiado] = useState(false);

  if (!isOpen) return null;

  const handleCopiar = async () => {
    if (!token) return;
    try {
      await navigator.clipboard.writeText(token);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    } catch {
      const textArea = document.createElement('textarea');
      textArea.value = token;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    }
  };

  const curlExemplo = `curl -X GET "${window.location.origin}/api/quadras" \\
  -H "Authorization: Bearer ${token || 'SEU_TOKEN'}"`;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xl overflow-y-auto animate-in fade-in duration-200"
    >
      <div className="bg-[#121214] border border-white/[0.1] rounded-3xl max-w-lg w-full p-6 sm:p-7 space-y-5 shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.08)] relative my-auto max-h-[90vh] overflow-y-auto animate-in zoom-in-95 duration-200">
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
            <KeyRound className="w-5 h-5 text-white/80" />
          </div>
          <h3 className="text-lg font-semibold text-white tracking-tight">Sua Chave de API (Token JWT)</h3>
          <p className="text-xs text-white/50 tracking-tight">
            Token de autenticação vinculado à sua sessão ativa ({user?.email_usuario}).
          </p>
        </div>

        {/* Nível de Acesso e Permissões */}
        <div
          className={`p-4 rounded-2xl border ${
            isAdmin
              ? 'bg-[#30D158]/5 border-[#30D158]/20 text-[#30D158]'
              : 'bg-[#0A84FF]/5 border-[#0A84FF]/20 text-[#0A84FF]'
          } flex items-start gap-3.5 text-xs`}
        >
          {isAdmin ? (
            <ShieldCheck className="w-5 h-5 text-[#30D158] shrink-0 mt-0.5" />
          ) : (
            <UserCheck className="w-5 h-5 text-[#0A84FF] shrink-0 mt-0.5" />
          )}
          <div className="space-y-1">
            <div className="font-semibold text-white flex items-center gap-2">
              <span>Nível de Acesso: {isAdmin ? 'ADMINISTRADOR' : 'USUÁRIO COMUM'}</span>
              <span
                className={`px-2.5 py-0.5 rounded-full text-[10px] font-medium ${
                  isAdmin ? 'bg-[#30D158]/15 text-[#30D158]' : 'bg-[#0A84FF]/15 text-[#0A84FF]'
                }`}
              >
                {isAdmin ? 'Leitura & Escrita' : 'Somente Leitura'}
              </span>
            </div>
            <p className="text-[11px] text-white/60 leading-relaxed tracking-tight">
              {isAdmin
                ? 'Esta chave possui privilégio total de gerenciamento: consultas, cadastros, alterações e exclusões nas rotas /api.'
                : 'Esta chave possui permissão restrita para consultas e listagens de dados (GET) nas rotas /api.'}
            </p>
          </div>
        </div>

        {/* Bloco de Token */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-medium text-white/70 tracking-tight">Token de Autenticação (Bearer)</span>
            <span className="text-[11px] text-white/40 font-mono">Sessão JWT</span>
          </div>

          <div className="relative">
            <div className="w-full bg-black/70 border border-white/[0.08] rounded-xl p-3.5 pr-28 font-mono text-[11px] text-white/80 break-all select-all max-h-24 overflow-y-auto leading-relaxed">
              {token || 'Nenhum token encontrado. Efetue login novamente.'}
            </div>
            <Button
              type="button"
              variant={copiado ? 'secondary' : 'primary'}
              size="sm"
              onClick={handleCopiar}
              className="absolute top-2.5 right-2.5"
              leftIcon={copiado ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
            >
              {copiado ? 'Copiado!' : 'Copiar'}
            </Button>
          </div>
        </div>

        {/* Exemplo cURL */}
        <div className="space-y-2">
          <div className="flex items-center gap-1.5 text-xs text-white/50 font-medium tracking-tight">
            <Terminal className="w-3.5 h-3.5 text-white/70" />
            <span>Como utilizar nas suas requisições HTTP:</span>
          </div>
          <div className="bg-black/70 border border-white/[0.08] rounded-xl p-3 font-mono text-[11px] text-white/70 overflow-x-auto select-all">
            <pre className="whitespace-pre-wrap leading-relaxed">{curlExemplo}</pre>
          </div>
        </div>

        <div className="pt-3 border-t border-white/[0.08] flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-[11px] text-white/40 tracking-tight">
            <Info className="w-3.5 h-3.5 text-white/40" />
            <span>Mantenha seu token em sigilo.</span>
          </div>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={onClose}
          >
            Fechar
          </Button>
        </div>
      </div>
    </div>,
    document.body
  );
};
