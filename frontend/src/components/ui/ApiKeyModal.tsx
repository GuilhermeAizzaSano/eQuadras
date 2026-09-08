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

  const curlExemplo = `curl -X GET "http://localhost:8080/api/quadras" \\
  -H "Authorization: Bearer ${token || 'SEU_TOKEN'}"`;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-md overflow-y-auto animate-in fade-in duration-200"
    >
      <div className="bg-surface-900 border border-surface-800 rounded-3xl max-w-lg w-full p-6 sm:p-7 space-y-5 shadow-2xl shadow-black/80 relative my-auto max-h-[90vh] overflow-y-auto animate-in zoom-in-95 duration-200">
        <button
          type="button"
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-surface-400 hover:text-white hover:bg-surface-800 transition cursor-pointer"
          aria-label="Fechar"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="space-y-1">
          <div className="inline-flex items-center justify-center w-11 h-11 rounded-2xl bg-surface-850 border border-surface-750 text-white mb-1 shadow-md">
            <KeyRound className="w-5 h-5 text-brand-400" />
          </div>
          <h3 className="text-lg font-bold text-white tracking-tight">Sua API-KEY de Integração</h3>
          <p className="text-xs text-surface-400">
            Token fixo e permanente vinculado à sua conta ({user?.email_usuario}).
          </p>
        </div>

        {/* Nível de Acesso e Permissões */}
        <div
          className={`p-4 rounded-2xl border ${
            isAdmin
              ? 'bg-emerald-950/30 border-emerald-500/30 text-emerald-300'
              : 'bg-sky-950/30 border-sky-500/30 text-sky-300'
          } flex items-start gap-3.5 text-xs`}
        >
          {isAdmin ? (
            <ShieldCheck className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
          ) : (
            <UserCheck className="w-5 h-5 text-sky-400 shrink-0 mt-0.5" />
          )}
          <div className="space-y-1">
            <div className="font-semibold text-white flex items-center gap-2">
              <span>Nível de Acesso: {isAdmin ? 'ADMINISTRADOR' : 'USUÁRIO COMUM'}</span>
              <span
                className={`px-2 py-0.5 rounded-full text-[10px] font-mono font-bold ${
                  isAdmin ? 'bg-emerald-500/20 text-emerald-400' : 'bg-sky-500/20 text-sky-400'
                }`}
              >
                {isAdmin ? 'Leitura & Escrita' : 'Somente Leitura'}
              </span>
            </div>
            <p className="text-[11px] text-surface-300 leading-relaxed">
              {isAdmin
                ? 'Esta chave possui privilégio total de gerenciamento: consultas, cadastros, alterações e exclusões nas rotas /api.'
                : 'Esta chave possui permissão restrita para consultas e listagens de dados (GET) nas rotas /api.'}
            </p>
          </div>
        </div>

        {/* Bloco de Token */}
        <div className="space-y-2">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-surface-300">Token de Autenticação (Bearer)</span>
            <span className="text-[11px] text-brand-400 font-mono font-medium">Não expira</span>
          </div>

          <div className="relative">
            <div className="w-full bg-surface-950 border border-surface-800 rounded-xl p-3.5 pr-28 font-mono text-[11px] text-surface-200 break-all select-all max-h-24 overflow-y-auto leading-relaxed">
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
          <div className="flex items-center gap-1.5 text-xs text-surface-400 font-medium">
            <Terminal className="w-3.5 h-3.5 text-brand-400" />
            <span>Como utilizar nas suas requisições HTTP:</span>
          </div>
          <div className="bg-surface-950/80 border border-surface-800 rounded-xl p-3 font-mono text-[11px] text-surface-300 overflow-x-auto select-all">
            <pre className="whitespace-pre-wrap leading-relaxed">{curlExemplo}</pre>
          </div>
        </div>

        <div className="pt-3 border-t border-surface-800/80 flex items-center justify-between">
          <div className="flex items-center gap-1.5 text-[11px] text-surface-500">
            <Info className="w-3.5 h-3.5 text-surface-400" />
            <span>Mantenha sua chave em sigilo.</span>
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
