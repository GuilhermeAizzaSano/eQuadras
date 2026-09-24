import React, { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';
import {
  X,
  KeyRound,
  Copy,
  Check,
  ShieldCheck,
  UserCheck,
  Terminal,
  AlertTriangle,
  RotateCw,
  Trash2,
  Sparkles,
  Loader2,
  Clock,
  Calendar,
  ShieldAlert,
  CheckCircle2,
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { usuarioApi } from '../../api/apiClient';
import { ApiKeyInfo, ApiKeyCriada } from '../../types';
import { Button } from './Button';
import { ConfirmModal } from './ConfirmModal';
import { FeedbackBanner } from './FeedbackBanner';

interface ApiKeyModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ApiKeyModal: React.FC<ApiKeyModalProps> = ({ isOpen, onClose }) => {
  const { user, isAdmin } = useAuth();

  // Estados de dados
  const [info, setInfo] = useState<ApiKeyInfo | null>(null);
  const [chaveRecemGerada, setChaveRecemGerada] = useState<ApiKeyCriada | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingAcao, setLoadingAcao] = useState(false);
  const [copiado, setCopiado] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Confirmações
  const [confirmTipo, setConfirmTipo] = useState<'regenerar' | 'revogar' | null>(null);
  const [modalRevogadoSucesso, setModalRevogadoSucesso] = useState(false);

  const carregarInfo = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setFeedback(null);
    try {
      const dados = await usuarioApi.obterApiKeyInfo(signal);
      setInfo(dados);
    } catch (err: any) {
      if (err.name !== 'AbortError') {
        setFeedback({ type: 'error', message: err.message || 'Erro ao consultar chave de API.' });
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isOpen) {
      // Limpeza de segurança: descartar imediatamente qualquer texto plano da memória
      setChaveRecemGerada(null);
      setConfirmTipo(null);
      setModalRevogadoSucesso(false);
      setFeedback(null);
      setCopiado(false);
      return;
    }

    const controller = new AbortController();
    carregarInfo(controller.signal);

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !confirmTipo && !loadingAcao && !modalRevogadoSucesso) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      controller.abort();
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, carregarInfo, confirmTipo, loadingAcao, modalRevogadoSucesso, onClose]);

  if (!isOpen) return null;

  const handleCopiar = async (texto: string) => {
    try {
      await navigator.clipboard.writeText(texto);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    } catch {
      const textArea = document.createElement('textarea');
      textArea.value = texto;
      document.body.appendChild(textArea);
      textArea.select();
      document.execCommand('copy');
      document.body.removeChild(textArea);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    }
  };

  const handleGerarOuRegenerar = async () => {
    setLoadingAcao(true);
    setFeedback(null);
    try {
      const novaChave = await usuarioApi.regenerarApiKey();
      setChaveRecemGerada(novaChave);
      setInfo({
        possuiChave: true,
        last4: novaChave.last4,
        criadaEm: novaChave.criadaEm,
        ultimoUsoEm: null,
      });
      setConfirmTipo(null);
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Falha ao gerar nova Chave de API.' });
    } finally {
      setLoadingAcao(false);
    }
  };

  const handleRevogar = async () => {
    setLoadingAcao(true);
    setFeedback(null);
    try {
      await usuarioApi.revogarApiKey();
      setInfo({ possuiChave: false, last4: null, criadaEm: null, ultimoUsoEm: null });
      setChaveRecemGerada(null);
      setConfirmTipo(null);
      setModalRevogadoSucesso(true);
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Falha ao revogar Chave de API.' });
    } finally {
      setLoadingAcao(false);
    }
  };

  const formatarDataHora = (dataIso?: string | null) => {
    if (!dataIso) return '';
    const d = new Date(dataIso);
    if (isNaN(d.getTime())) return dataIso;
    return d.toLocaleString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getCurlExemplo = (apiKeySnippet: string) =>
    `curl -X GET "${window.location.origin}/api/quadras" \\\n  -H "X-API-KEY: ${apiKeySnippet}"`;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xl overflow-y-auto animate-in fade-in duration-200"
    >
      <div className="bg-surface-2 border border-fg/[0.1] rounded-3xl max-w-xl w-full p-6 sm:p-7 space-y-5 shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.08)] relative my-auto max-h-[90vh] overflow-y-auto animate-in zoom-in-95 duration-200">
        {/* Botão Fechar */}
        <Button
          type="button"
          onClick={onClose}
          variant="ghost" size="icon" className="absolute top-5 right-5"
          aria-label="Fechar"
          disabled={loadingAcao}
        >
          <X className="w-5 h-5" />
        </Button>

        {/* Cabeçalho */}
        <div className="space-y-1">
          <div className="inline-flex items-center justify-center w-11 h-11 rounded-2xl bg-fg/[0.06] border border-fg/[0.1] text-fg mb-1 shadow-sm">
            <KeyRound className="w-5 h-5 text-fg/80" />
          </div>
          <h3 className="text-lg font-semibold text-fg tracking-tight">Chave de API (Integração)</h3>
          <p className="text-xs text-fg/60 tracking-tight">
            Credencial opaca de alta entropia para automações, scripts e integração externa ({user?.email_usuario}).
          </p>
        </div>

        {/* Feedback Banner */}
        <FeedbackBanner feedback={feedback} onClose={() => setFeedback(null)} />

        {/* Nível de Acesso da Chave */}
        <div
          className={`p-3.5 rounded-2xl border ${
            isAdmin
              ? 'bg-success/5 border-success/20 text-success'
              : 'bg-info/5 border-info/20 text-info'
          } flex items-start gap-3 text-xs`}
        >
          {isAdmin ? (
            <ShieldCheck className="w-4 h-4 text-success shrink-0 mt-0.5" />
          ) : (
            <UserCheck className="w-4 h-4 text-info shrink-0 mt-0.5" />
          )}
          <div className="space-y-0.5">
            <div className="font-semibold text-fg flex items-center gap-2">
              <span>Nível de Acesso: {isAdmin ? 'ADMINISTRADOR' : 'CLIENTE'}</span>
              <span
                className={`px-2 py-0.5 rounded-full text-xs font-medium ${
                  isAdmin ? 'bg-success/15 text-success' : 'bg-info/15 text-info'
                }`}
              >
                {isAdmin ? 'Total nas rotas de negócio' : 'Consultas nas rotas de negócio'}
              </span>
            </div>
            <p className="text-xs text-fg/60 leading-relaxed tracking-tight">
              A API-KEY acessa apenas rotas de negócio permitidas. Gerenciamento de conta, senhas e chaves exigem obrigatoriamente Sessão Web.
            </p>
          </div>
        </div>

        {/* Conteúdo Principal */}
        {loading ? (
          <div className="py-12 flex flex-col items-center justify-center gap-3">
            <Loader2 className="w-6 h-6 text-fg/60 animate-spin" />
            <span className="text-xs text-fg/60 tracking-tight">Consultando estado da chave...</span>
          </div>
        ) : chaveRecemGerada ? (
          /* ESTADO 4: Chave Recém-Gerada (Exibição Única) */
          <div className="space-y-4 animate-in fade-in duration-200">
            {/* Alerta de Cópia Única */}
            <div className="p-4 rounded-2xl bg-warning/10 border border-warning/30 text-warning flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-warning shrink-0 mt-0.5" />
              <div className="space-y-1 text-xs">
                <span className="font-semibold text-warning">Copie sua Chave de API agora</span>
                <p className="text-warning/80 leading-relaxed">
                  Por motivos de segurança, esta chave <strong>não será exibida novamente</strong>. Se você fechar esta tela sem copiá-la, será necessário regenerar uma nova chave.
                </p>
              </div>
            </div>

            {/* Input com Token em Texto Plano */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs text-fg/70">
                <span className="font-medium">Sua nova Chave de API:</span>
                <span className="text-xs text-success font-mono flex items-center gap-1">
                  <Sparkles className="w-3 h-3" /> Gerada com sucesso
                </span>
              </div>
              <div className="relative">
                <div className="w-full bg-bg/80 border border-warning/40 rounded-xl p-3.5 pr-28 font-mono text-xs text-fg font-medium break-all select-all shadow-inner">
                  {chaveRecemGerada.apiKey}
                </div>
                <Button
                  type="button"
                  variant={copiado ? 'secondary' : 'primary'}
                  size="sm"
                  onClick={() => handleCopiar(chaveRecemGerada.apiKey)}
                  className="absolute top-2.5 right-2.5"
                  leftIcon={copiado ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                >
                  {copiado ? 'Copiado!' : 'Copiar'}
                </Button>
              </div>
            </div>

            {/* Exemplo de uso cURL */}
            <div className="space-y-1.5">
              <div className="flex items-center gap-1.5 text-xs text-fg/60 font-medium tracking-tight">
                <Terminal className="w-3.5 h-3.5 text-fg/70" />
                <span>Exemplo de requisição externa:</span>
              </div>
              <div className="bg-bg/70 border border-fg/[0.1] rounded-xl p-3 font-mono text-xs text-fg/70 overflow-x-auto select-all">
                <pre className="whitespace-pre-wrap leading-relaxed">
                  {getCurlExemplo(chaveRecemGerada.apiKey)}
                </pre>
              </div>
            </div>

            <div className="pt-2 flex justify-end">
              <Button
                type="button"
                variant="primary"
                size="md"
                onClick={() => setChaveRecemGerada(null)}
              >
                Concluir e ocultar chave
              </Button>
            </div>
          </div>
        ) : info?.possuiChave ? (
          /* ESTADO 3: Chave Ativa Mascarada */
          <div className="space-y-4">
            {/* Bloco de Chave Mascarada */}
            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs">
                <span className="font-medium text-fg/70">Chave Ativa (Mascarada)</span>
                <span className="text-xs text-success font-medium">● Ativa</span>
              </div>
              <div className="w-full bg-bg/60 border border-fg/[0.1] rounded-xl p-3.5 font-mono text-xs text-fg/70 flex items-center justify-between">
                <span>eq_••••••••••••••••••••{info.last4}</span>
                <span className="text-xs text-fg/60 uppercase tracking-wider font-sans font-semibold">Oculta</span>
              </div>
              <p className="text-xs text-fg/60 leading-relaxed">
                O token completo foi exibido apenas no momento da criação por segurança.
              </p>
            </div>

            {/* Metadados e Auditoria */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs">
              <div className="p-3 rounded-xl bg-fg/[0.03] border border-fg/[0.06] flex items-center gap-2.5">
                <Calendar className="w-4 h-4 text-fg/60 shrink-0" />
                <div className="min-w-0">
                  <span className="text-xs text-fg/60 block uppercase tracking-wider">Criada em</span>
                  <span className="text-fg/80 font-medium truncate block">{formatarDataHora(info.criadaEm)}</span>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-fg/[0.03] border border-fg/[0.06] flex items-center gap-2.5">
                <Clock className="w-4 h-4 text-fg/60 shrink-0" />
                <div className="min-w-0">
                  <span className="text-xs text-fg/60 block uppercase tracking-wider">Último uso</span>
                  <span className="text-fg/80 font-medium truncate block">
                    {info.ultimoUsoEm ? formatarDataHora(info.ultimoUsoEm) : 'Nunca utilizada'}
                  </span>
                </div>
              </div>
            </div>

            {/* Exemplo cURL */}
            <div className="space-y-1.5">
              <div className="flex items-center gap-1.5 text-xs text-fg/60 font-medium tracking-tight">
                <Terminal className="w-3.5 h-3.5 text-fg/70" />
                <span>Como utilizar via Header:</span>
              </div>
              <div className="bg-bg/70 border border-fg/[0.1] rounded-xl p-3 font-mono text-xs text-fg/70 overflow-x-auto select-all">
                <pre className="whitespace-pre-wrap leading-relaxed">
                  {getCurlExemplo(`eq_••••••••${info.last4}`)}
                </pre>
              </div>
            </div>

            {/* Botões de Ação */}
            <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-2.5">
              <Button
                type="button"
                variant="destructive"
                size="sm"
                onClick={() => setConfirmTipo('revogar')}
                disabled={loadingAcao}
                leftIcon={<Trash2 className="w-3.5 h-3.5" />}
              >
                Revogar Chave
              </Button>

              <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setConfirmTipo('regenerar')}
                  disabled={loadingAcao}
                  leftIcon={<RotateCw className="w-3.5 h-3.5" />}
                >
                  Regenerar Chave
                </Button>
                <Button
                  type="button"
                  variant="primary"
                  size="sm"
                  onClick={onClose}
                  disabled={loadingAcao}
                >
                  Fechar
                </Button>
              </div>
            </div>
          </div>
        ) : (
          /* ESTADO 2: Sem Chave de API Ativa */
          <div className="py-6 flex flex-col items-center text-center space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-fg/[0.03] border border-fg/[0.1] flex items-center justify-center text-fg/60">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div className="space-y-1 max-w-sm">
              <h4 className="text-sm font-semibold text-fg">Nenhuma Chave de API Ativa</h4>
              <p className="text-xs text-fg/60 leading-relaxed">
                Você ainda não possui uma Chave de API gerada. Gere uma chave para conectar automações, scripts ou acessar as rotas de negócio.
              </p>
            </div>
            <Button
              type="button"
              variant="primary"
              size="md"
              onClick={handleGerarOuRegenerar}
              isLoading={loadingAcao}
              leftIcon={<Sparkles className="w-4 h-4" />}
            >
              Gerar Chave de API
            </Button>
          </div>
        )}

        {/* Rodapé Informativo */}
        <div className="pt-3 border-t border-fg/[0.1] flex items-center justify-between text-xs text-fg/60">
          <span>Autenticação segregada</span>
          <span>Header: X-API-KEY ou Bearer</span>
        </div>
      </div>

      {/* Modal de Confirmação para Regenerar */}
      <ConfirmModal
        isOpen={confirmTipo === 'regenerar'}
        title="Regenerar Chave de API?"
        description="Ao regenerar esta chave, a chave atual será INVALIDADA IMEDIATAMENTE no mesmo milissegundo. Qualquer integração ou script utilizando a chave anterior parará de funcionar."
        confirmLabel="Sim, Regenerar Chave"
        cancelLabel="Cancelar"
        isDestructive={true}
        onConfirm={handleGerarOuRegenerar}
        onCancel={() => setConfirmTipo(null)}
      />

      {/* Modal de Confirmação para Revogar */}
      <ConfirmModal
        isOpen={confirmTipo === 'revogar'}
        title="Revogar Chave de API?"
        description="Tem certeza que deseja revogar permanentemente sua Chave de API? Ela deixará de funcionar imediatamente em todas as automações."
        confirmLabel="Sim, Revogar Chave"
        cancelLabel="Cancelar"
        isDestructive={true}
        onConfirm={handleRevogar}
        onCancel={() => setConfirmTipo(null)}
      />

      {/* Modal de Aviso de Chave Revogada com Sucesso */}
      {modalRevogadoSucesso && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-[110] flex items-center justify-center p-4 bg-black/70 backdrop-blur-2xl animate-in fade-in duration-200"
        >
          <div className="bg-surface-2 border border-fg/[0.1] rounded-3xl w-full max-w-md p-6 sm:p-7 shadow-apple-elevated space-y-6 relative my-auto animate-in zoom-in-95 duration-200">
            <div className="flex items-start justify-between gap-3">
              <div className="flex items-center gap-3">
                <div className="p-3 rounded-2xl border shrink-0 bg-success/10 border-success/25 text-success">
                  <CheckCircle2 className="w-5 h-5" />
                </div>
                <h3 className="text-lg font-semibold text-fg tracking-tight leading-tight">
                  Chave Revogada com Sucesso
                </h3>
              </div>
              <button
                type="button"
                onClick={() => {
                  setModalRevogadoSucesso(false);
                  onClose();
                }}
                className="p-1.5 rounded-xl text-fg/60 hover:text-fg hover:bg-fg/[0.1] transition shrink-0 cursor-pointer"
                aria-label="Fechar"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="p-4 rounded-2xl bg-fg/[0.03] border border-fg/[0.1] text-xs sm:text-sm text-fg/80 leading-relaxed font-sans">
              Sua Chave de API foi revogada permanentemente. A partir deste instante, nenhuma chave está ativa e quaisquer integrações foram desabilitadas com segurança.
            </div>

            <div className="pt-2">
              <Button
                type="button"
                variant="primary"
                onClick={() => {
                  setModalRevogadoSucesso(false);
                  onClose();
                }}
                className="w-full"
              >
                Fechar
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>,
    document.body
  );
};
