import React, { useState, useEffect } from 'react';
import { Agendamento } from '../../types';
import { pagamentoApi, agendamentoApi } from '../../api/apiClient';
import { Check, Copy, QrCode, X, Clock, AlertTriangle, AlertCircle, Loader2 } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { Button } from './Button';

interface ModalPixProps {
  isOpen: boolean;
  agendamento: Agendamento | null;
  onClose: () => void;
  onSuccess: (agendamentoAtualizado: Agendamento) => void;
  onExpired?: () => void;
}

export const ModalPix: React.FC<ModalPixProps> = ({
  isOpen,
  agendamento,
  onClose,
  onSuccess,
  onExpired,
}) => {
  const [dadosLocais, setDadosLocais] = useState<Agendamento | null>(agendamento);
  const [carregandoDados, setCarregandoDados] = useState(false);
  const [copiado, setCopiado] = useState(false);
  const [simulando, setSimulando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [segundosRestantes, setSegundosRestantes] = useState<number>(900);

  // Sincroniza estado local e busca dados detalhados se vier sem Pix
  useEffect(() => {
    if (!isOpen || !agendamento) {
      setDadosLocais(null);
      return;
    }

    setDadosLocais(agendamento);

    // Se o agendamento fornecido não tem os dados de Pix, busca do backend
    if (!agendamento.pixCopiaECola && !agendamento.qrCodeBase64) {
      setCarregandoDados(true);
      agendamentoApi
        .buscarPorId(agendamento.id_agendamento)
        .then((completo) => {
          if (completo) {
            setDadosLocais(completo);
          }
        })
        .catch(() => {
          // Mantém o atual em caso de falha
        })
        .finally(() => {
          setCarregandoDados(false);
        });
    }
  }, [isOpen, agendamento]);

  useEffect(() => {
    if (!isOpen || !agendamento) return;

    const calcularTempoRestante = () => {
      const criadoEmMs = new Date(agendamento.criadoEm).getTime();
      const expiraEmMs = criadoEmMs + 15 * 60 * 1000;
      const agoraMs = new Date().getTime();
      const diffSegundos = Math.floor((expiraEmMs - agoraMs) / 1000);
      return Math.max(0, diffSegundos);
    };

    setSegundosRestantes(calcularTempoRestante());

    const interval = setInterval(() => {
      const restantes = calcularTempoRestante();
      setSegundosRestantes(restantes);
      if (restantes <= 0) {
        clearInterval(interval);
        if (onExpired) onExpired();
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [isOpen, agendamento, onExpired]);

  useEffect(() => {
    if (!isOpen || !agendamento || agendamento.status !== 'PENDENTE') return;

    let ativo = true;

    const checarStatus = async () => {
      try {
        const agendamentoAtual = await pagamentoApi.consultarStatus(agendamento.id_agendamento);
        if (ativo && agendamentoAtual && agendamentoAtual.status === 'CONFIRMADO') {
          onSuccess(agendamentoAtual);
        }
      } catch {
        // Ignora falhas temporárias no polling
      }
    };

    const pollInterval = setInterval(checarStatus, 3500);

    return () => {
      ativo = false;
      clearInterval(pollInterval);
    };
  }, [isOpen, agendamento, onSuccess]);

  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !simulando) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, simulando, onClose]);

  if (!isOpen || !agendamento) return null;

  const minutos = Math.floor(segundosRestantes / 60);
  const segundos = segundosRestantes % 60;
  const tempoFormatado = `${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')}`;
  const expirado = segundosRestantes <= 0;

  const agendamentoAtivo = dadosLocais || agendamento;

  const copiarPix = () => {
    if (agendamentoAtivo.pixCopiaECola) {
      navigator.clipboard.writeText(agendamentoAtivo.pixCopiaECola);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 3000);
    }
  };

  const handleSimularPagamento = async () => {
    setSimulando(true);
    setErro(null);
    try {
      const atualizado = await pagamentoApi.simularAprovacao(agendamentoAtivo.id_agendamento);
      onSuccess(atualizado);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Falha ao confirmar pagamento.';
      setErro(msg);
    } finally {
      setSimulando(false);
    }
  };

  const [data, tempoInicio] = agendamentoAtivo.dataHoraInicio.split('T');
  const [, tempoFim] = agendamentoAtivo.dataHoraFim.split('T');
  const horaInicio = tempoInicio ? tempoInicio.substring(0, 5) : '';
  const horaFim = tempoFim ? tempoFim.substring(0, 5) : '';

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xl animate-in fade-in duration-200"
    >
      <div className="bg-surface-2 border border-fg/[0.1] rounded-3xl max-w-md w-full p-6 sm:p-7 space-y-5 shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.08)] relative animate-in zoom-in-95 duration-200">
        {/* Botão Fechar */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-full bg-fg/[0.06] hover:bg-fg/[0.15] text-fg/60 hover:text-fg border border-fg/[0.1] transition cursor-pointer"
          title="Fechar"
          aria-label="Fechar modal Pix"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Cabeçalho */}
        <div className="space-y-2 text-center flex flex-col items-center">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-fg/[0.06] border border-fg/[0.1] text-fg mb-0.5 shadow-sm">
            <QrCode className="w-6 h-6 text-fg/80" />
          </div>
          <h3 className="text-xl font-semibold text-fg tracking-tight">Pagamento via Pix</h3>

          {/* Contador de 15 Minutos */}
          <div
            className={`inline-flex items-center gap-2 px-3 py-1 rounded-full border text-xs font-mono font-medium ${
              expirado
                ? 'bg-danger/10 border-danger/25 text-danger'
                : 'bg-warning/10 border-warning/25 text-warning'
            }`}
          >
            <Clock className={`w-3.5 h-3.5 ${expirado ? 'text-danger' : 'text-warning'}`} />
            <span>{expirado ? 'Tempo limite expirado' : `Expira em: ${tempoFormatado}`}</span>
          </div>
        </div>

        {/* Detalhes da Reserva */}
        <div className="p-4 rounded-2xl bg-fg/[0.03] border border-fg/[0.1] flex items-center justify-between text-xs">
          <div>
            <div className="font-semibold text-fg text-sm tracking-tight">{agendamentoAtivo.nomeQuadra}</div>
            <div className="text-fg/60 mt-0.5 tracking-tight">
              {data.split('-').reverse().join('/')} • {horaInicio} às {horaFim}
            </div>
          </div>
          <div className="text-right">
            <div className="text-fg/60 uppercase tracking-wider text-xs font-semibold">Total</div>
            <div className="text-lg font-bold text-fg font-mono tracking-tight">
              R$ {agendamentoAtivo.valorTotal.toFixed(2)}
            </div>
          </div>
        </div>

        {/* QR Code Container */}
        <div className="flex flex-col items-center justify-center p-5 bg-fg rounded-2xl border border-fg/20 shadow-sm min-h-[220px]">
          {carregandoDados ? (
            <div className="w-44 h-44 flex flex-col items-center justify-center bg-zinc-100 rounded-xl text-zinc-600 gap-2">
              <Loader2 className="w-6 h-6 text-warning animate-spin" />
              <span className="text-xs font-medium">Carregando Pix...</span>
            </div>
          ) : expirado ? (
            <div className="w-44 h-44 flex flex-col items-center justify-center bg-zinc-100 rounded-xl text-center p-4 text-zinc-600 gap-2">
              <AlertTriangle className="w-8 h-8 text-warning" />
              <span className="text-xs font-semibold">Código Pix expirado</span>
              <span className="text-xs text-zinc-500">Gere uma nova reserva para pagar.</span>
            </div>
          ) : agendamentoAtivo.pixCopiaECola ? (
            <div className="w-44 h-44 flex items-center justify-center bg-fg rounded-xl select-none p-1">
              <QRCodeSVG
                value={agendamentoAtivo.pixCopiaECola}
                size={168}
                level="M"
                includeMargin={false}
              />
            </div>
          ) : agendamentoAtivo.qrCodeBase64 ? (
            agendamentoAtivo.qrCodeBase64.startsWith('data:') ? (
              <img
                src={agendamentoAtivo.qrCodeBase64}
                alt="QR Code Pix"
                className="w-44 h-44 object-contain rounded-xl select-none"
              />
            ) : (
              <img
                src={`data:image/png;base64,${agendamentoAtivo.qrCodeBase64}`}
                alt="QR Code Pix"
                className="w-44 h-44 object-contain rounded-xl select-none"
              />
            )
          ) : (
            <div className="w-44 h-44 flex items-center justify-center bg-zinc-100 rounded-xl text-zinc-400 text-xs">
              QR Code indisponível
            </div>
          )}
          {!expirado && !carregandoDados && (
            <span className="text-xs text-zinc-600 font-sans mt-2.5 font-medium tracking-tight">
              Abra o app do seu banco e aponte a câmera
            </span>
          )}
        </div>

        {/* Pix Copia e Cola */}
        {!expirado && agendamentoAtivo.pixCopiaECola && (
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-fg/60 uppercase tracking-wider">
              Chave Pix Copia e Cola
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={agendamentoAtivo.pixCopiaECola}
                className="w-full bg-bg/70 border border-fg/[0.1] rounded-xl px-3 py-2 text-xs text-fg/80 font-mono focus:outline-none"
              />
              <Button
                type="button"
                variant={copiado ? 'secondary' : 'primary'}
                size="sm"
                onClick={copiarPix}
                className="shrink-0"
                leftIcon={copiado ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
              >
                {copiado ? 'Copiado!' : 'Copiar'}
              </Button>
            </div>
          </div>
        )}

        {erro && (
          <div className="p-3.5 rounded-xl bg-danger/10 border border-danger/25 text-danger text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-danger shrink-0" />
            <span>{erro}</span>
          </div>
        )}

        {/* Ações / Simulador de Aprovação */}
        <div className="pt-2 border-t border-fg/[0.1] flex flex-col gap-2.5">
          <Button
            type="button"
            variant="secondary"
            size="md"
            onClick={handleSimularPagamento}
            isLoading={simulando}
            disabled={expirado}
            className="w-full"
          >
            Simular Aprovação (Ambiente de Testes)
          </Button>

          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={onClose}
            className="w-full text-fg/60 hover:text-fg"
          >
            Fechar janela
          </Button>
        </div>
      </div>
    </div>
  );
};
