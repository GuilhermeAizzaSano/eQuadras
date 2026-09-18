import React, { useState, useEffect, useMemo } from 'react';
import { Agendamento } from '../../types';
import { EmptyState, Badge } from '../ui';
import { Calendar as CalendarIcon, Clock, QrCode, History, Loader2, ChevronLeft, ChevronRight } from 'lucide-react';
import { formatarDataHoraBr } from '../../utils/dateUtils';

interface ClientBookingsListProps {
  meusAgendamentos: Agendamento[];
  historicoCarregado: boolean;
  carregandoHistorico: boolean;
  onCarregarHistorico: () => void;
  onPayPix: (agendamento: Agendamento) => void;
  onCancelBooking: (idAgendamento: number) => void;
}

const ITENS_POR_PAGINA_RESERVAS = 5;

export const ClientBookingsList: React.FC<ClientBookingsListProps> = ({
  meusAgendamentos,
  historicoCarregado,
  carregandoHistorico,
  onCarregarHistorico,
  onPayPix,
  onCancelBooking,
}) => {
  const [filtroStatusReservas, setFiltroStatusReservas] = useState<'ATIVOS' | 'CANCELADOS' | 'REALIZADOS'>('ATIVOS');
  const [paginaAtualReservas, setPaginaAtualReservas] = useState(1);

  // Resetar página ao mudar filtro de status
  useEffect(() => {
    setPaginaAtualReservas(1);
  }, [filtroStatusReservas]);

  // Intervalo de tick para atualizar contadores de tempo real
  const [agora, setAgora] = useState(() => Date.now());
  useEffect(() => {
    const hasPendente = meusAgendamentos.some((a) => a.status === 'PENDENTE');
    if (!hasPendente) return;
    const timer = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [meusAgendamentos]);

  const getTempoRestantePix = (criadoEm: string) => {
    const criadoMs = new Date(criadoEm).getTime();
    const expiraMs = criadoMs + 15 * 60 * 1000;
    const diff = Math.floor((expiraMs - agora) / 1000);
    if (diff <= 0) return null;
    const min = Math.floor(diff / 60);
    const seg = diff % 60;
    return `${String(min).padStart(2, '0')}:${String(seg).padStart(2, '0')}`;
  };

  // Filtragem dinâmica de agendamentos do cliente
  const agendamentosFiltrados = useMemo(() => {
    const agoraLocal = new Date();
    return meusAgendamentos.filter((ag) => {
      const dataFim = new Date(ag.dataHoraFim);
      const isCancelado = ag.status === 'CANCELADO';
      const isPassado = dataFim < agoraLocal;

      if (filtroStatusReservas === 'CANCELADOS') {
        return isCancelado;
      }
      if (filtroStatusReservas === 'REALIZADOS') {
        return !isCancelado && isPassado;
      }
      // 'ATIVOS' (Futuros/Em andamento que não foram cancelados)
      return !isCancelado && !isPassado;
    });
  }, [meusAgendamentos, filtroStatusReservas]);

  const totalItensReservas = agendamentosFiltrados.length;
  const totalPaginasReservas = Math.max(1, Math.ceil(totalItensReservas / ITENS_POR_PAGINA_RESERVAS));
  const paginaValidaReservas = Math.min(Math.max(1, paginaAtualReservas), totalPaginasReservas);
  const indiceInicioReservas = (paginaValidaReservas - 1) * ITENS_POR_PAGINA_RESERVAS;
  const agendamentosPaginados = agendamentosFiltrados.slice(
    indiceInicioReservas,
    indiceInicioReservas + ITENS_POR_PAGINA_RESERVAS
  );

  // Contadores para as abas
  const contadoresReservas = useMemo(() => {
    const agoraLocal = new Date();
    let ativos = 0;
    let cancelados = 0;
    let realizados = 0;

    meusAgendamentos.forEach((ag) => {
      const dataFim = new Date(ag.dataHoraFim);
      const isCancelado = ag.status === 'CANCELADO';
      const isPassado = dataFim < agoraLocal;

      if (isCancelado) {
        cancelados++;
      } else if (isPassado) {
        realizados++;
      } else {
        ativos++;
      }
    });

    return { ativos, cancelados, realizados };
  }, [meusAgendamentos]);

  return (
    <div className="bg-white/[0.02] border border-white/[0.06] rounded-3xl p-5 sm:p-7 space-y-6">
      {/* Sub-abas de Filtro de Reservas */}
      <div className="flex bg-white/[0.04] p-1 rounded-2xl border border-white/[0.08] text-xs">
        <button
          onClick={() => setFiltroStatusReservas('ATIVOS')}
          className={`flex-1 py-2 px-3 font-semibold rounded-xl transition-all flex items-center justify-center gap-2 active:scale-[0.98] cursor-pointer tracking-tight ${
            filtroStatusReservas === 'ATIVOS'
              ? 'bg-white text-black shadow-sm'
              : 'text-white/60 hover:text-white'
          }`}
        >
          <span>Ativos</span>
          <span
            className={`text-[10px] px-2 py-0.5 rounded-full font-mono font-medium ${
              filtroStatusReservas === 'ATIVOS'
                ? 'bg-black/10 text-black'
                : 'bg-white/10 text-white/50'
            }`}
          >
            {contadoresReservas.ativos}
          </span>
        </button>

        <button
          onClick={() => setFiltroStatusReservas('REALIZADOS')}
          className={`flex-1 py-2 px-3 font-semibold rounded-xl transition-all flex items-center justify-center gap-2 active:scale-[0.98] cursor-pointer tracking-tight ${
            filtroStatusReservas === 'REALIZADOS'
              ? 'bg-white text-black shadow-sm'
              : 'text-white/60 hover:text-white'
          }`}
        >
          <span>Realizados</span>
          <span
            className={`text-[10px] px-2 py-0.5 rounded-full font-mono font-medium ${
              filtroStatusReservas === 'REALIZADOS'
                ? 'bg-black/10 text-black'
                : 'bg-white/10 text-white/50'
            }`}
          >
            {historicoCarregado ? contadoresReservas.realizados : '—'}
          </span>
        </button>

        <button
          onClick={() => setFiltroStatusReservas('CANCELADOS')}
          className={`flex-1 py-2 px-3 font-semibold rounded-xl transition-all flex items-center justify-center gap-2 active:scale-[0.98] cursor-pointer tracking-tight ${
            filtroStatusReservas === 'CANCELADOS'
              ? 'bg-white text-black shadow-sm'
              : 'text-white/60 hover:text-white'
          }`}
        >
          <span>Cancelados</span>
          <span
            className={`text-[10px] px-2 py-0.5 rounded-full font-mono font-medium ${
              filtroStatusReservas === 'CANCELADOS'
                ? 'bg-black/10 text-black'
                : 'bg-white/10 text-white/50'
            }`}
          >
            {historicoCarregado ? contadoresReservas.cancelados : '—'}
          </span>
        </button>
      </div>

      {/* Listagem de Reservas ou Botão de Carregar Histórico */}
      {filtroStatusReservas !== 'ATIVOS' && !historicoCarregado ? (
        <div className="py-12 px-6 flex flex-col items-center justify-center text-center bg-white/[0.02] border border-white/[0.06] rounded-2xl space-y-4">
          <div className="w-13 h-13 rounded-2xl bg-white/[0.04] border border-white/[0.08] flex items-center justify-center text-white/60 shadow-sm">
            <History className="w-6 h-6 text-white/80" />
          </div>
          <div className="space-y-1.5 max-w-md">
            <h4 className="text-sm font-semibold text-white tracking-tight">
              Histórico não carregado
            </h4>
            <p className="text-xs text-white/50 leading-relaxed tracking-tight">
              Por padrão carregamos apenas suas reservas ativas para maior rapidez. Clique abaixo para carregar todo o seu histórico.
            </p>
          </div>
          <button
            onClick={onCarregarHistorico}
            disabled={carregandoHistorico}
            className="mt-2 inline-flex items-center gap-2 px-5 py-2 rounded-xl bg-white hover:bg-white/90 disabled:opacity-40 text-black font-semibold text-xs shadow-sm transition active:scale-[0.98] cursor-pointer tracking-tight"
          >
            {carregandoHistorico ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin text-black" />
                <span>Carregando histórico...</span>
              </>
            ) : (
              <>
                <History className="w-4 h-4" />
                <span>Carregar histórico de reservas</span>
              </>
            )}
          </button>
        </div>
      ) : agendamentosFiltrados.length === 0 ? (
        <EmptyState
          icon={Clock}
          title={
            filtroStatusReservas === 'ATIVOS'
              ? 'Nenhuma reserva ativa no momento'
              : filtroStatusReservas === 'REALIZADOS'
              ? 'Nenhuma reserva realizada no histórico'
              : 'Nenhuma reserva cancelada'
          }
          description={
            filtroStatusReservas === 'ATIVOS'
              ? 'Clique na aba "Quadras" para encontrar uma quadra e agendar seu jogo.'
              : 'Seus registros aparecerão aqui conforme as partidas forem finalizadas ou canceladas.'
          }
          className="py-14"
        />
      ) : (
        <div className="space-y-4">
          <div className="space-y-3.5">
            {agendamentosPaginados.map((ag) => {
              const isCancelado = ag.status === 'CANCELADO';
              const isPassado = new Date(ag.dataHoraFim) < new Date();
              const [data, tempoInicio] = ag.dataHoraInicio.split('T');
              const [, tempoFim] = ag.dataHoraFim.split('T');
              const horaInicio = tempoInicio ? tempoInicio.substring(0, 5) : '';
              const horaFim = tempoFim ? tempoFim.substring(0, 5) : '';

              return (
                <div
                  key={ag.id_agendamento}
                  className="p-4 sm:p-5 rounded-2xl bg-white/[0.03] border border-white/[0.08] space-y-3.5 transition hover:border-white/[0.16] shadow-sm"
                >
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                    <div>
                      <div className="text-base font-semibold text-white tracking-tight">
                        {ag.nomeQuadra}
                      </div>
                      <div className="text-xs text-white/50 flex items-center gap-1.5 mt-1 tracking-tight">
                        <CalendarIcon className="w-3.5 h-3.5 text-white/40" />
                        <span>{data.split('-').reverse().join('/')}</span>
                        <span className="text-white/30">•</span>
                        <span>{horaInicio} às {horaFim}</span>
                      </div>

                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-2 text-[11px] text-white/40 font-mono">
                        {ag.criadoEm && (
                          <span>Agendado em: <strong className="text-white/60 font-normal">{formatarDataHoraBr(ag.criadoEm)}</strong></span>
                        )}
                        {isCancelado && (
                          <span className="text-[#FF453A]/90">
                            Cancelado em: <strong className="text-[#FF453A] font-medium">{ag.canceladoEm ? formatarDataHoraBr(ag.canceladoEm) : '—'}</strong>
                          </span>
                        )}
                      </div>
                      {ag.status === 'PENDENTE' && !isPassado && (() => {
                        const tempo = getTempoRestantePix(ag.criadoEm);
                        return tempo ? (
                          <div className="text-xs text-[#FF9F0A] font-mono flex items-center gap-1.5 mt-2 font-medium bg-[#FF9F0A]/10 border border-[#FF9F0A]/20 px-2.5 py-1 rounded-full w-fit">
                            <Clock className="w-3.5 h-3.5 animate-pulse text-[#FF9F0A]" />
                            <span>Pague via Pix em até {tempo}</span>
                          </div>
                        ) : (
                          <div className="text-xs text-[#FF453A] font-mono flex items-center gap-1.5 mt-2 font-medium bg-[#FF453A]/10 border border-[#FF453A]/20 px-2.5 py-1 rounded-full w-fit">
                            <Clock className="w-3.5 h-3.5 text-[#FF453A]" />
                            <span>Tempo de pagamento expirado</span>
                          </div>
                        );
                      })()}
                    </div>

                    <Badge
                      variant={
                        isCancelado
                          ? 'outline'
                          : isPassado
                          ? 'neutral'
                          : ag.status === 'PENDENTE'
                          ? 'warning'
                          : 'success'
                      }
                      withDot
                    >
                      {isCancelado ? 'CANCELADO' : isPassado ? 'REALIZADO' : ag.status}
                    </Badge>
                  </div>

                  <div className="flex items-center justify-between pt-3 border-t border-white/[0.08] text-xs">
                    <span className="text-white font-mono font-bold text-sm tracking-tight">
                      R$ {ag.valorTotal.toFixed(2)}
                    </span>

                    <div className="flex items-center gap-3">
                      {ag.status === 'PENDENTE' && (
                        <button
                          onClick={() => onPayPix(ag)}
                          className="text-xs bg-white hover:bg-white/90 text-black font-semibold px-3.5 py-2 rounded-xl transition flex items-center gap-1.5 shadow-sm active:scale-[0.98] cursor-pointer tracking-tight"
                        >
                          <QrCode className="w-3.5 h-3.5" />
                          <span>Pagar com Pix</span>
                        </button>
                      )}

                      {!isCancelado && (
                        <button
                          onClick={() => onCancelBooking(ag.id_agendamento)}
                          className="text-xs text-[#FF453A] hover:text-[#FF453A]/80 font-medium transition active:scale-95 cursor-pointer tracking-tight"
                        >
                          Cancelar
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          {totalPaginasReservas > 1 && (
            <div className="pt-4 border-t border-white/[0.08] flex items-center justify-between text-xs">
              <button
                type="button"
                disabled={paginaValidaReservas <= 1}
                onClick={() => setPaginaAtualReservas((p) => Math.max(1, p - 1))}
                className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-white/80 disabled:opacity-30 disabled:cursor-not-allowed transition border border-white/[0.06] cursor-pointer active:scale-95"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
                <span>Anterior</span>
              </button>
              <span className="text-[11px] text-white/50 font-mono">
                Página {paginaValidaReservas} de {totalPaginasReservas} ({totalItensReservas} {totalItensReservas === 1 ? 'reserva' : 'reservas'})
              </span>
              <button
                type="button"
                disabled={paginaValidaReservas >= totalPaginasReservas}
                onClick={() => setPaginaAtualReservas((p) => Math.min(totalPaginasReservas, p + 1))}
                className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-white/80 disabled:opacity-30 disabled:cursor-not-allowed transition border border-white/[0.06] cursor-pointer active:scale-95"
              >
                <span>Próxima</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
