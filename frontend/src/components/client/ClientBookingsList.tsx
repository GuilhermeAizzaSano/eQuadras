import React, { useState, useEffect, useCallback } from 'react';
import { Agendamento } from '../../types';
import { EmptyState, Badge } from '../ui';
import { Calendar as CalendarIcon, Clock, QrCode, AlertCircle, RefreshCw } from 'lucide-react';
import { getAgoraBrasilia, parseDataHoraLocal } from '../../utils/dateUtils';
import { agendamentoApi, AbaAgendamento } from '../../api/apiClient';
import { usePaginatedQuery, PageFetcher } from '../../shared/pagination/usePaginatedQuery';
import { Pagination } from '../../shared/pagination/Pagination';

interface ClientBookingsListProps {
  onPayPix: (agendamento: Agendamento) => void;
  onCancelBooking: (idAgendamento: number, onSuccess?: () => void) => void;
}

const ITENS_POR_PAGINA_RESERVAS = 5;

const fetchAgendamentosPage: PageFetcher<Agendamento, { aba: AbaAgendamento }> = ({
  page,
  size,
  filters,
  signal,
}) => {
  return agendamentoApi.listarPaginado({
    page,
    size,
    aba: filters.aba,
    signal,
  });
};

export const ClientBookingsList: React.FC<ClientBookingsListProps> = ({
  onPayPix,
  onCancelBooking,
}) => {
  const [filtroStatusReservas, setFiltroStatusReservas] = useState<AbaAgendamento>('ATIVOS');
  const [contadores, setContadores] = useState<Record<AbaAgendamento, number>>({
    ATIVOS: 0,
    REALIZADOS: 0,
    CANCELADOS: 0,
  });

  const carregarContadores = useCallback(async (signal?: AbortSignal) => {
    try {
      const dados = await agendamentoApi.obterContadores(signal);
      setContadores(dados);
    } catch {
      // Falha silenciosa para contadores se abortado
    }
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    carregarContadores(controller.signal);
    return () => controller.abort();
  }, [carregarContadores]);

  const {
    items: agendamentos,
    page,
    totalPages,
    totalElements,
    status,
    isEmpty,
    setPage,
    reload,
  } = usePaginatedQuery(
    fetchAgendamentosPage,
    { aba: filtroStatusReservas },
    { size: ITENS_POR_PAGINA_RESERVAS }
  );

  // Intervalo de tick para atualizar contadores de tempo real Pix
  const [agora, setAgora] = useState(() => Date.now());
  useEffect(() => {
    const hasPendente = agendamentos.some((a) => a.status === 'PENDENTE');
    if (!hasPendente) return;
    const timer = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [agendamentos]);

  const getTempoRestantePix = (criadoEm: string) => {
    const criadoMs = new Date(criadoEm).getTime();
    const expiraMs = criadoMs + 15 * 60 * 1000;
    const diff = Math.floor((expiraMs - agora) / 1000);
    if (diff <= 0) return null;
    const min = Math.floor(diff / 60);
    const seg = diff % 60;
    return `${String(min).padStart(2, '0')}:${String(seg).padStart(2, '0')}`;
  };

  const handleCancel = (id: number) => {
    onCancelBooking(id, () => {
      reload();
      carregarContadores();
    });
  };

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
            {contadores.ATIVOS}
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
            {contadores.REALIZADOS}
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
            {contadores.CANCELADOS}
          </span>
        </button>
      </div>

      {/* Estados de Interface: Erro, Carregando Inicial, Vazio ou Lista */}
      {status === 'error' && agendamentos.length === 0 ? (
        <div className="py-12 px-6 flex flex-col items-center justify-center text-center bg-white/[0.02] border border-white/[0.06] rounded-2xl space-y-4">
          <div className="w-12 h-12 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center text-red-400">
            <AlertCircle className="w-6 h-6" />
          </div>
          <div className="space-y-1 max-w-sm">
            <h4 className="text-sm font-semibold text-white">Falha ao carregar reservas</h4>
            <p className="text-xs text-white/50">Não foi possível carregar as reservas desta aba.</p>
          </div>
          <button
            type="button"
            onClick={reload}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-white hover:bg-white/90 text-black font-semibold text-xs transition active:scale-95 cursor-pointer"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Tentar novamente</span>
          </button>
        </div>
      ) : isEmpty ? (
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
          <div className={`space-y-3.5 min-h-[480px] transition-opacity duration-200 ${status === 'loading' ? 'opacity-50 pointer-events-none' : 'opacity-100'}`}>
            {agendamentos.map((ag) => {
              const agoraBr = getAgoraBrasilia().agora;
              const isCancelado = ag.status === 'CANCELADO';
              const dataInicio = parseDataHoraLocal(ag.dataHoraInicio);
              const isRetroativoOuEmAndamento = dataInicio <= agoraBr;
              const isPassado = parseDataHoraLocal(ag.dataHoraFim) < agoraBr;
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

                      {ag.status === 'PENDENTE' && (
                        <div className="mt-2 flex items-center gap-2">
                          <span className="inline-flex items-center gap-1 text-[11px] font-mono font-medium text-[#FF9F0A] bg-[#FF9F0A]/10 border border-[#FF9F0A]/20 px-2 py-0.5 rounded-md">
                            <Clock className="w-3 h-3 animate-spin" />
                            {getTempoRestantePix(ag.criadoEm) ? (
                              <span>Expira em {getTempoRestantePix(ag.criadoEm)}</span>
                            ) : (
                              <span>Expirando...</span>
                            )}
                          </span>
                        </div>
                      )}
                    </div>

                    <Badge
                      variant={
                        isCancelado
                          ? 'danger'
                          : ag.status === 'PENDENTE'
                          ? 'warning'
                          : isPassado
                          ? 'neutral'
                          : 'success'
                      }
                      className="self-start sm:self-auto uppercase tracking-wider text-[10px] font-mono px-2.5 py-0.5"
                    >
                      {isCancelado
                        ? 'Cancelado'
                        : ag.status === 'PENDENTE'
                        ? 'Pendente Pix'
                        : isPassado
                        ? 'Realizado'
                        : 'Confirmado'}
                    </Badge>
                  </div>

                  <div className="flex flex-col sm:flex-row sm:items-center justify-between pt-3 border-t border-white/[0.08] text-xs gap-2">
                    <span className="text-white font-mono font-bold text-sm tracking-tight">
                      R$ {ag.valorTotal.toFixed(2)}
                    </span>

                    <div className="flex items-center gap-3">
                      {ag.status === 'PENDENTE' && (
                        <button
                          type="button"
                          onClick={() => onPayPix(ag)}
                          className="text-xs bg-white hover:bg-white/90 text-black font-semibold px-3.5 py-2 rounded-xl transition flex items-center gap-1.5 shadow-sm active:scale-[0.98] cursor-pointer tracking-tight"
                        >
                          <QrCode className="w-3.5 h-3.5" />
                          <span>Pagar com Pix</span>
                        </button>
                      )}

                      {!isCancelado && (
                        isRetroativoOuEmAndamento ? (
                          <span className="text-[11px] text-white/40 italic">
                            Não é possível cancelar um agendamento que está em andamento ou retroativo.
                          </span>
                        ) : (
                          <button
                            type="button"
                            onClick={() => handleCancel(ag.id_agendamento)}
                            className="text-xs text-[#FF453A] hover:text-[#FF453A]/80 font-medium transition active:scale-95 cursor-pointer tracking-tight"
                          >
                            Cancelar
                          </button>
                        )
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>

          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            onPageChange={setPage}
            isLoading={status === 'loading'}
          />
        </div>
      )}
    </div>
  );
};
