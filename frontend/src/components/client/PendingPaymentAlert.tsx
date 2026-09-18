import React, { useState, useEffect } from 'react';
import { Agendamento } from '../../types';
import { Clock, ChevronLeft, ChevronRight, QrCode } from 'lucide-react';

interface PendingPaymentAlertProps {
  agendamentos: Agendamento[];
  onPayPix: (agendamento: Agendamento) => void;
}

export const PendingPaymentAlert: React.FC<PendingPaymentAlertProps> = ({
  agendamentos,
  onPayPix,
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [agora, setAgora] = useState(() => Date.now());

  // Filtra apenas agendamentos pendentes válidos (dentro dos 15 minutos)
  const pendentesValidos = agendamentos.filter((a) => {
    if (a.status !== 'PENDENTE') return false;
    const criadoMs = new Date(a.criadoEm).getTime();
    const expiraMs = criadoMs + 15 * 60 * 1000;
    return expiraMs > agora;
  });

  // Atualiza relógio a cada segundo enquanto houver agendamentos pendentes
  useEffect(() => {
    if (pendentesValidos.length === 0) return;
    const timer = setInterval(() => {
      setAgora(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, [pendentesValidos.length]);

  // Se o índice atual estiver fora do range após expiração, ajusta
  useEffect(() => {
    if (currentIndex >= pendentesValidos.length && pendentesValidos.length > 0) {
      setCurrentIndex(pendentesValidos.length - 1);
    }
  }, [currentIndex, pendentesValidos.length]);

  if (pendentesValidos.length === 0) {
    return null;
  }

  const agendamentoAtual = pendentesValidos[currentIndex] || pendentesValidos[0];
  const criadoMs = new Date(agendamentoAtual.criadoEm).getTime();
  const expiraMs = criadoMs + 15 * 60 * 1000;
  const diffSegundos = Math.max(0, Math.floor((expiraMs - agora) / 1000));
  const minutos = Math.floor(diffSegundos / 60);
  const segundos = diffSegundos % 60;
  const tempoFormatado = `${String(minutos).padStart(2, '0')}:${String(segundos).padStart(2, '0')}`;

  const handlePrev = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev > 0 ? prev - 1 : pendentesValidos.length - 1));
  };

  const handleNext = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev < pendentesValidos.length - 1 ? prev + 1 : 0));
  };

  return (
    <div className="w-full max-w-xl animate-in fade-in slide-in-from-top-2 duration-300">
      <div className="relative overflow-hidden rounded-2xl border border-[#FF9F0A]/30 bg-[#FF9F0A]/[0.08] backdrop-blur-xl p-3.5 sm:p-4 shadow-apple-card">
        {/* Glow de fundo sutil */}
        <div className="absolute -right-8 -top-8 w-28 h-28 bg-[#FF9F0A]/10 rounded-full blur-2xl pointer-events-none" />

        <div className="flex items-center justify-between gap-3 relative z-10">
          <div className="flex items-start gap-3 min-w-0">
            <div className="p-2 rounded-xl bg-[#FF9F0A]/15 border border-[#FF9F0A]/30 text-[#FF9F0A] shrink-0 mt-0.5">
              <Clock className="w-4 h-4 animate-pulse" />
            </div>

            <div className="min-w-0 space-y-1">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-[10px] uppercase font-bold tracking-wider font-mono text-[#FF9F0A]">
                  Pagamento Pendente
                </span>
                <span className="inline-flex items-center gap-1 text-[11px] font-mono font-semibold px-2 py-0.5 rounded-full bg-[#FF9F0A]/20 border border-[#FF9F0A]/30 text-[#FF9F0A]">
                  Expira em {tempoFormatado}
                </span>
              </div>

              <p className="text-xs sm:text-sm text-white/90 leading-snug break-words">
                Confirme o pagamento da sua reserva na{' '}
                <span className="font-semibold text-white">{agendamentoAtual.nomeQuadra}</span> em até{' '}
                <span className="font-mono font-bold text-[#FF9F0A]">{tempoFormatado}</span> ou ela será cancelada automaticamente.
              </p>
            </div>
          </div>

          <div className="flex flex-col sm:flex-row items-end sm:items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={() => onPayPix(agendamentoAtual)}
              className="px-3.5 py-1.5 rounded-xl bg-[#FF9F0A] hover:bg-[#FF9F0A]/90 text-black font-semibold text-xs transition active:scale-95 flex items-center gap-1.5 shadow-sm shadow-[#FF9F0A]/20 cursor-pointer"
            >
              <QrCode className="w-3.5 h-3.5" />
              <span>Pagar Pix</span>
            </button>

            {pendentesValidos.length > 1 && (
              <div className="flex items-center gap-1 bg-black/40 border border-white/10 rounded-xl p-0.5">
                <button
                  type="button"
                  onClick={handlePrev}
                  className="p-1 text-white/60 hover:text-white rounded-lg hover:bg-white/10 transition cursor-pointer"
                  title="Agendamento pendente anterior"
                  aria-label="Anterior"
                >
                  <ChevronLeft className="w-3.5 h-3.5" />
                </button>
                <span className="text-[10px] font-mono px-1 text-white/70">
                  {currentIndex + 1}/{pendentesValidos.length}
                </span>
                <button
                  type="button"
                  onClick={handleNext}
                  className="p-1 text-white/60 hover:text-white rounded-lg hover:bg-white/10 transition cursor-pointer"
                  title="Próximo agendamento pendente"
                  aria-label="Próximo"
                >
                  <ChevronRight className="w-3.5 h-3.5" />
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
