import React, { useState, useEffect } from 'react';
import { Agendamento } from '../../types';
import { pagamentoApi } from '../../api/apiClient';
import { Check, Copy, QrCode, Sparkles, X, Loader2, AlertCircle, Clock, AlertTriangle } from 'lucide-react';

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
  const [segundosRestantes, setSegundosRestantes] = useState<number>(900); // 15 min default

  useEffect(() => {
    if (!isOpen || !agendamento) return;

    const calcularTempoRestante = () => {
      // criadoEm ISO
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

  // Polling automático para detecção instantânea do pagamento Pix no gateway / webhook
  useEffect(() => {
    if (!isOpen || !agendamento || agendamento.status !== 'PENDENTE') return;

    let ativo = true;

    const checarStatus = async () => {
      try {
        const agendamentoAtual = await pagamentoApi.consultarStatus(agendamento.id_agendamento);
        if (ativo && agendamentoAtual && agendamentoAtual.status === 'CONFIRMADO') {
          onSuccess(agendamentoAtual);
        }
      } catch (e) {
        // Erros transitórios de rede em polling não devem quebrar o modal
      }
    };

    // Consulta a cada 3.5 segundos
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-zinc-950 border border-zinc-800 rounded-3xl max-w-md w-full p-6 space-y-6 shadow-2xl relative">
        {/* Botão Fechar */}
        <button
          onClick={onClose}
          className="absolute top-5 right-5 p-2 rounded-xl text-zinc-400 hover:text-white hover:bg-zinc-900 transition"
          title="Fechar"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Cabeçalho */}
        <div className="space-y-1 text-center">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-zinc-900 border border-zinc-800 text-white mb-2">
            <QrCode className="w-6 h-6 text-white" />
          </div>
          <h3 className="text-xl font-bold text-white tracking-tight">Pagar Reserva com Pix</h3>
          
          {/* Contador de 15 Minutos */}
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-zinc-900 border border-zinc-800 text-xs font-mono">
            <Clock className={`w-3.5 h-3.5 ${expirado ? 'text-red-400' : 'text-amber-400 animate-pulse'}`} />
            <span className={expirado ? 'text-red-400 font-bold' : 'text-zinc-300 font-semibold'}>
              {expirado ? 'Tempo limite expirado' : `Expira em: ${tempoFormatado}`}
            </span>
          </div>
        </div>

        {/* Detalhes da Reserva */}
        <div className="p-3.5 rounded-2xl bg-zinc-900/60 border border-zinc-800/80 flex items-center justify-between text-xs">
          <div>
            <div className="font-bold text-white text-sm">{agendamento.nomeQuadra}</div>
            <div className="text-zinc-400 mt-0.5">
              {data.split('-').reverse().join('/')} • {horaInicio} às {horaFim}
            </div>
          </div>
          <div className="text-right">
            <div className="text-zinc-500 uppercase tracking-wider text-[10px] font-semibold">Valor</div>
            <div className="text-base font-extrabold text-white font-mono">
              R$ {agendamento.valorTotal.toFixed(2)}
            </div>
          </div>
        </div>

        {/* QR Code Container */}
        <div className="flex flex-col items-center justify-center p-5 bg-white rounded-2xl border border-zinc-200 shadow-inner">
          {expirado ? (
            <div className="w-44 h-44 flex flex-col items-center justify-center bg-zinc-100 rounded-lg text-center p-4 text-zinc-600 gap-2">
              <AlertTriangle className="w-8 h-8 text-amber-600" />
              <span className="text-xs font-semibold">Código Pix expirado</span>
              <span className="text-[10px] text-zinc-500">Gere uma nova reserva para pagar.</span>
            </div>
          ) : agendamento.qrCodeBase64 ? (
            agendamento.qrCodeBase64.startsWith('data:') ? (
              <img
                src={agendamento.qrCodeBase64}
                alt="QR Code Pix"
                className="w-44 h-44 object-contain rounded-lg"
              />
            ) : (
              <img
                src={`data:image/png;base64,${agendamento.qrCodeBase64}`}
                alt="QR Code Pix"
                className="w-44 h-44 object-contain rounded-lg"
              />
            )
          ) : (
            <div className="w-44 h-44 flex items-center justify-center bg-zinc-100 rounded-lg text-zinc-400 text-xs">
              QR Code não disponível
            </div>
          )}
          {!expirado && (
            <span className="text-[11px] text-zinc-600 font-mono mt-2 font-medium">
              Abra seu app de pagamentos e aponte a câmera
            </span>
          )}
        </div>

        {/* Pix Copia e Cola */}
        {!expirado && agendamento.pixCopiaECola && (
          <div className="space-y-1.5">
            <label className="text-[11px] font-semibold text-zinc-400 uppercase tracking-wider">
              Pix Copia e Cola
            </label>
            <div className="flex items-center gap-2">
              <input
                type="text"
                readOnly
                value={agendamento.pixCopiaECola}
                className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-zinc-300 font-mono focus:outline-none"
              />
              <button
                onClick={copiarPix}
                className={`px-3.5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-1.5 whitespace-nowrap ${
                  copiado
                    ? 'bg-emerald-500 text-black'
                    : 'bg-white text-black hover:bg-zinc-200'
                }`}
              >
                {copiado ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copiado ? 'Copiado!' : 'Copiar'}</span>
              </button>
            </div>
          </div>
        )}

        {erro && (
          <div className="p-3 rounded-xl bg-red-950/40 border border-red-900/60 text-red-400 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{erro}</span>
          </div>
        )}

        {/* Ações / Ambiente de Teste */}
        <div className="space-y-2 pt-2 border-t border-zinc-850">
          {!expirado && (
            <button
              onClick={handleSimularPagamento}
              disabled={simulando}
              className="w-full bg-emerald-400 hover:bg-emerald-300 text-zinc-950 font-bold py-3 px-4 rounded-xl text-xs transition flex items-center justify-center gap-2 disabled:opacity-50 active:scale-[0.98] shadow-lg shadow-emerald-950/20"
            >
              {simulando ? (
                <Loader2 className="w-4 h-4 animate-spin text-zinc-950" />
              ) : (
                <Sparkles className="w-4 h-4 text-zinc-950" />
              )}
              <span>{simulando ? 'Processando confirmação...' : 'Simular Pagamento Aprovado (Dev)'}</span>
            </button>
          )}

          <button
            onClick={onClose}
            className="w-full bg-zinc-900 hover:bg-zinc-850 text-zinc-300 hover:text-white font-medium py-2.5 rounded-xl text-xs transition border border-zinc-800"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
};
