import React, { useState, useEffect } from 'react';
import { Agendamento } from '../../types';
import { pagamentoApi } from '../../api/apiClient';
import { Check, Copy, QrCode, X, Clock, AlertTriangle, AlertCircle } from 'lucide-react';
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
  const [copiado, setCopiado] = useState(false);
  const [simulando, setSimulando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [segundosRestantes, setSegundosRestantes] = useState<number>(900);

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

  if (!isOpen || !agendamento) return null;

  const minutos = Math.floor(segundosRestantes / 60);
  const segundos = segundosRestantes % 60;
  const tempoFormatado = `${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')}`;
  const expirado = segundosRestantes <= 0;

  const copiarPix = () => {
    if (agendamento.pixCopiaECola) {
      navigator.clipboard.writeText(agendamento.pixCopiaECola);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 3000);
    }
  };

  const handleSimularPagamento = async () => {
    setSimulando(true);
    setErro(null);
    try {
      const atualizado = await pagamentoApi.simularAprovacao(agendamento.id_agendamento);
      onSuccess(atualizado);
    } catch (err: any) {
      setErro(err.message || 'Falha ao confirmar pagamento.');
    } finally {
      setSimulando(false);
    }
  };

  const [data, tempoInicio] = agendamento.dataHoraInicio.split('T');
  const [, tempoFim] = agendamento.dataHoraFim.split('T');
  const horaInicio = tempoInicio ? tempoInicio.substring(0, 5) : '';
  const horaFim = tempoFim ? tempoFim.substring(0, 5) : '';

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200"
    >
      <div className="bg-surface-900 border border-surface-800 rounded-3xl max-w-md w-full p-6 sm:p-7 space-y-5 shadow-2xl shadow-black/80 relative animate-in zoom-in-95 duration-200">
        {/* Botão Fechar */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-surface-400 hover:text-white hover:bg-surface-800 transition cursor-pointer"
          title="Fechar"
          aria-label="Fechar modal Pix"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Cabeçalho */}
        <div className="space-y-2 text-center flex flex-col items-center">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-surface-850 border border-surface-750 text-brand-400 shadow-md">
            <QrCode className="w-6 h-6 text-brand-400" />
          </div>
          <h3 className="text-xl font-bold text-white tracking-tight">Pagamento com Pix</h3>

          {/* Contador de 15 Minutos */}
          <div
            className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full border text-xs font-mono font-semibold ${
              expirado
                ? 'bg-red-950/40 border-red-500/30 text-red-400'
                : 'bg-amber-950/40 border-amber-500/30 text-amber-300'
            }`}
          >
            <Clock className={`w-3.5 h-3.5 ${expirado ? 'text-red-400' : 'text-amber-400 animate-pulse'}`} />
            <span>{expirado ? 'Tempo limite expirado' : `Expira em: ${tempoFormatado}`}</span>
          </div>
        </div>

        {/* Detalhes da Reserva */}
        <div className="p-4 rounded-2xl bg-surface-950/80 border border-surface-800 flex items-center justify-between text-xs">
          <div>
            <div className="font-bold text-white text-sm">{agendamento.nomeQuadra}</div>
            <div className="text-surface-400 mt-0.5">
              {data.split('-').reverse().join('/')} • {horaInicio} às {horaFim}
            </div>
          </div>
          <div className="text-right">
            <div className="text-surface-500 uppercase tracking-wider text-[10px] font-semibold">Total</div>
            <div className="text-lg font-extrabold text-brand-400 font-mono">
              R$ {agendamento.valorTotal.toFixed(2)}
            </div>
          </div>
        </div>

        {/* QR Code Container */}
        <div className="flex flex-col items-center justify-center p-5 bg-white rounded-2xl border border-zinc-200 shadow-inner">
          {expirado ? (
            <div className="w-44 h-44 flex flex-col items-center justify-center bg-zinc-100 rounded-xl text-center p-4 text-zinc-600 gap-2">
              <AlertTriangle className="w-8 h-8 text-amber-600" />
              <span className="text-xs font-semibold">Código Pix expirado</span>
              <span className="text-[10px] text-zinc-500">Gere uma nova reserva para pagar.</span>
            </div>
          ) : agendamento.qrCodeBase64 ? (
            agendamento.qrCodeBase64.startsWith('data:') ? (
              <img
                src={agendamento.qrCodeBase64}
                alt="QR Code Pix"
                className="w-44 h-44 object-contain rounded-xl select-none"
              />
            ) : (
              <img
                src={`data:image/png;base64,${agendamento.qrCodeBase64}`}
                alt="QR Code Pix"
                className="w-44 h-44 object-contain rounded-xl select-none"
              />
            )
          ) : (
            <div className="w-44 h-44 flex items-center justify-center bg-zinc-100 rounded-xl text-zinc-400 text-xs">
              QR Code indisponível
            </div>
          )}
          {!expirado && (
            <span className="text-[11px] text-zinc-600 font-mono mt-2.5 font-semibold">
              Abra o app do seu banco e aponte a câmera
            </span>
          )}
        </div>

        {/* Pix Copia e Cola */}
        {!expirado && agendamento.pixCopiaECola && (
          <div className="space-y-1.5">
            <label className="text-[10px] font-bold text-surface-400 uppercase tracking-wider">
              Chave Pix Copia e Cola
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={agendamento.pixCopiaECola}
                className="w-full bg-surface-950 border border-surface-800 rounded-xl px-3 py-2 text-xs text-surface-200 font-mono focus:outline-none"
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
          <div className="p-3.5 rounded-xl bg-red-950/40 border border-red-500/30 text-red-300 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-red-400 shrink-0" />
            <span>{erro}</span>
          </div>
        )}

        {/* Ações / Simulador de Aprovação */}
        <div className="pt-2 border-t border-surface-800/80 flex flex-col gap-2.5">
          <Button
            type="button"
            variant="outline"
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
            className="w-full"
          >
            Fechar janela
          </Button>
        </div>
      </div>
    </div>
  );
};
