import React, { useMemo, useCallback, useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { Quadra, Agendamento, HorarioDisponivel, BloqueioHorario } from '../../types';
import { Badge, EmptyState, Button } from '../ui';
import {
  Clock,
  Calendar as CalendarIcon,
  Users,
  X,
  Ban,
  ShieldCheck,
  Phone,
  Info,
  Loader2
} from 'lucide-react';
import { parseDataHoraLocal, formatarDataHoraBr, getAgoraBrasilia } from '../../utils/dateUtils';
import { agendamentoApi, AbaAgendamento } from '../../api/apiClient';
import { usePaginatedQuery, PageFetcher, Pagination } from '../../shared/pagination';

interface DayAgendaModalProps {
  isOpen: boolean;
  dataSelecionada: string;
  minhasQuadras: Quadra[];
  agendamentosAdmin?: Agendamento[];
  mapaBloqueiosPorQuadra: Record<number, BloqueioHorario[]>;
  horariosDisponiveisPorQuadra: Record<number, HorarioDisponivel[]>;
  loadingHorariosModal: boolean;
  quadraSelecionadaAgendaId: number | 'TODAS';
  statusFiltroModal: 'TODOS' | 'LIVRES' | 'AGENDADOS' | 'BLOQUEADOS';
  visualizacaoAgendaAba: 'GRADE_HORARIOS' | 'LISTA_RESERVAS';
  filtroAgendaAdmin: 'ATIVOS' | 'REALIZADOS' | 'CANCELADOS';
  highlightedAgendamentoId: number | null;
  onClose: () => void;
  onQuadraChange: (id: number | 'TODAS') => void;
  onStatusFiltroChange: (status: 'TODOS' | 'LIVRES' | 'AGENDADOS' | 'BLOQUEADOS') => void;
  onVisualizacaoAbaChange: (aba: 'GRADE_HORARIOS' | 'LISTA_RESERVAS') => void;
  onFiltroAgendaAdminChange: (filtro: 'ATIVOS' | 'REALIZADOS' | 'CANCELADOS') => void;
  onSelectHighlightedAgendamento: (id: number | null) => void;
  onCancelarAgendamento: (id: number, onSuccess?: () => void) => void;
  onVerQuadra: (quadra: Quadra) => void;
  getTempoRestantePix: (criadoEm: string) => string | null;
}

const ITENS_POR_PAGINA = 5;

const fetchAgendaPage: PageFetcher<Agendamento, { data: string; quadraId?: number; aba: AbaAgendamento }> = ({
  page,
  size,
  filters,
  signal,
}) => {
  return agendamentoApi.listarAgendaDoDiaPaginado({
    data: filters.data,
    page,
    size,
    quadraId: filters.quadraId,
    aba: filters.aba,
    signal,
  });
};

export const DayAgendaModal: React.FC<DayAgendaModalProps> = ({
  isOpen,
  dataSelecionada,
  minhasQuadras,
  agendamentosAdmin = [],
  mapaBloqueiosPorQuadra,
  horariosDisponiveisPorQuadra,
  loadingHorariosModal,
  quadraSelecionadaAgendaId,
  statusFiltroModal,
  visualizacaoAgendaAba,
  filtroAgendaAdmin,
  highlightedAgendamentoId,
  onClose,
  onQuadraChange,
  onStatusFiltroChange,
  onVisualizacaoAbaChange,
  onFiltroAgendaAdminChange,
  onSelectHighlightedAgendamento,
  onCancelarAgendamento,
  onVerQuadra,
  getTempoRestantePix,
}) => {
  const filters = useMemo(() => ({
    data: dataSelecionada,
    quadraId: quadraSelecionadaAgendaId === 'TODAS' ? undefined : quadraSelecionadaAgendaId,
    aba: filtroAgendaAdmin as AbaAgendamento,
  }), [dataSelecionada, quadraSelecionadaAgendaId, filtroAgendaAdmin]);

  const {
    items: agendamentosPaginados,
    page,
    setPage,
    totalPages,
    totalElements,
    status,
    error,
    isEmpty,
    reload,
  } = usePaginatedQuery(fetchAgendaPage, filters, { size: ITENS_POR_PAGINA, enabled: isOpen });

  const [contadores, setContadores] = useState<Record<AbaAgendamento, number>>({
    ATIVOS: 0,
    REALIZADOS: 0,
    CANCELADOS: 0,
  });

  const totalReservasDia = (contadores.ATIVOS || 0) + (contadores.REALIZADOS || 0) + (contadores.CANCELADOS || 0);

  const carregarContadores = useCallback(async (signal?: AbortSignal) => {
    try {
      const res = await agendamentoApi.obterContadoresAgendaDoDia({
        data: dataSelecionada,
        quadraId: quadraSelecionadaAgendaId === 'TODAS' ? undefined : quadraSelecionadaAgendaId,
        signal,
      });
      setContadores(res);
    } catch {
      // Falha silenciosa para contadores
    }
  }, [dataSelecionada, quadraSelecionadaAgendaId]);

  useEffect(() => {
    if (isOpen) {
      const controller = new AbortController();
      carregarContadores(controller.signal);
      return () => controller.abort();
    }
  }, [isOpen, carregarContadores]);

  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = originalOverflow;
      };
    }
  }, [isOpen]);

  useEffect(() => {
    if (highlightedAgendamentoId && isOpen) {
      const timer = setTimeout(() => {
        const el = document.getElementById(`agendamento-card-${highlightedAgendamentoId}`);
        const container = el?.closest('.overflow-y-auto');
        if (el && container) {
          const elTop = el.offsetTop;
          const containerHeight = container.clientHeight;
          container.scrollTo({ top: Math.max(0, elTop - containerHeight / 3), behavior: 'smooth' });
        }
      }, 200);
      return () => clearTimeout(timer);
    }
  }, [highlightedAgendamentoId, isOpen, visualizacaoAgendaAba]);

  if (!isOpen) return null;

  return createPortal(
    <div role="dialog" aria-modal="true" className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/70 backdrop-blur-2xl animate-in fade-in duration-200">
      <div className="bg-surface-2 border border-fg/[0.1] rounded-2xl sm:rounded-3xl w-full max-w-3xl h-[720px] max-h-[90vh] flex flex-col shadow-2xl shadow-black/80 overflow-hidden animate-in zoom-in-95 duration-200">
        {/* Header do Modal */}
        <div className="p-5 sm:p-6 border-b border-fg/[0.06] flex items-center justify-between bg-fg/[0.03] shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-fg/[0.06] border border-fg/[0.1] text-fg flex items-center justify-center">
              <Clock className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-semibold text-fg tracking-tight">
                Agenda do Dia
              </h3>
              <p className="text-xs text-fg/50 mt-0.5">
                Visualizando disponibilidade para <strong className="text-fg font-mono">{dataSelecionada.split('-').reverse().join('/')}</strong>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-fg/[0.03] hover:bg-fg/[0.1] text-fg/50 hover:text-fg border border-fg/[0.1] transition cursor-pointer"
            title="Fechar janela"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Sub-header com Seletor de Quadra, Filtros e Abas de Visualização */}
        <div className="px-5 sm:px-6 py-3 border-b border-fg/[0.06] bg-fg/[0.03] flex flex-col md:flex-row md:items-center justify-between gap-3 shrink-0">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 shrink-0">
              <span className="text-xs text-fg/50 font-medium font-mono">Quadra:</span>
              <select
                value={quadraSelecionadaAgendaId}
                onChange={(e) => {
                  const val = e.target.value;
                  onQuadraChange(val === 'TODAS' ? 'TODAS' : Number(val));
                }}
                className="bg-surface-3 border border-fg/[0.1] text-xs text-fg rounded-xl px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-fg/20 focus:border-fg/30 transition font-mono"
              >
                <option value="TODAS">Todas as Quadras ({minhasQuadras.length})</option>
                {minhasQuadras.map((q) => (
                  <option key={q.id_quadra} value={q.id_quadra}>
                    {q.nome} {!q.ativa ? '(Inativa)' : ''}
                  </option>
                ))}
              </select>
            </div>

            {/* Filtro por Status dos Horários no Modal */}
            {visualizacaoAgendaAba === 'GRADE_HORARIOS' && (
              <div className="flex items-center gap-1 bg-fg/[0.03] border border-fg/[0.1] p-1 rounded-xl text-xs shrink-0">
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('TODOS')}
                  className={`px-2.5 py-1 rounded-lg font-medium transition cursor-pointer ${
                    statusFiltroModal === 'TODOS'
                      ? 'bg-fg text-on-accent font-semibold shadow-sm'
                      : 'text-fg/60 hover:text-fg'
                  }`}
                >
                  Todos
                </button>
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('LIVRES')}
                  className={`px-2.5 py-1 rounded-lg font-medium transition flex items-center gap-1.5 cursor-pointer ${
                    statusFiltroModal === 'LIVRES'
                      ? 'bg-success/20 text-success border border-success/30 font-semibold'
                      : 'text-fg/60 hover:text-fg'
                  }`}
                  title="Exibir apenas quadras com horários disponíveis"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-success" />
                  <span>Livres</span>
                </button>
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('AGENDADOS')}
                  className={`px-2.5 py-1 rounded-lg font-medium transition flex items-center gap-1.5 cursor-pointer ${
                    statusFiltroModal === 'AGENDADOS'
                      ? 'bg-info/20 text-info border border-info/30 font-semibold'
                      : 'text-fg/60 hover:text-fg'
                  }`}
                  title="Exibir apenas quadras com horários agendados"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-info" />
                  <span>Agendados</span>
                </button>
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('BLOQUEADOS')}
                  className={`px-2.5 py-1 rounded-lg font-medium transition flex items-center gap-1.5 cursor-pointer ${
                    statusFiltroModal === 'BLOQUEADOS'
                      ? 'bg-danger/20 text-danger border border-danger/30 font-semibold'
                      : 'text-fg/60 hover:text-danger'
                  }`}
                  title="Exibir apenas quadras com horários bloqueados"
                >
                  <Ban className="w-3 h-3 text-danger" />
                  <span>Bloqueados</span>
                </button>
              </div>
            )}
          </div>

          {/* Toggle de Abas: Grade de Horários vs Reservas */}
          <div className="flex items-center bg-fg/[0.03] p-1 rounded-xl border border-fg/[0.1] text-xs shrink-0 self-start md:self-center">
            <button
              type="button"
              onClick={() => onVisualizacaoAbaChange('GRADE_HORARIOS')}
              className={`px-3 py-1.5 font-medium rounded-lg transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                visualizacaoAgendaAba === 'GRADE_HORARIOS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg'
              }`}
            >
              <CalendarIcon className="w-3.5 h-3.5" />
              <span>Grade</span>
            </button>
            <button
              type="button"
              onClick={() => onVisualizacaoAbaChange('LISTA_RESERVAS')}
              className={`px-3 py-1.5 font-medium rounded-lg transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                visualizacaoAgendaAba === 'LISTA_RESERVAS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg'
              }`}
            >
              <Users className="w-3.5 h-3.5" />
              <span>Reservas ({totalReservasDia})</span>
            </button>
          </div>
        </div>

        {/* Conteúdo do Modal */}
        <div className="flex-1 min-h-0 p-5 sm:p-6 overflow-y-auto space-y-6 scrollbar-thin">
          {visualizacaoAgendaAba === 'GRADE_HORARIOS' ? (
            <div className="space-y-6">
              {/* Legenda Resumida */}
              <div className="flex flex-wrap items-center justify-between gap-3 text-xs bg-fg/[0.03] border border-fg/[0.06] p-3 rounded-2xl">
                <div className="flex flex-wrap items-center gap-4">
                  <div className="flex items-center gap-1.5 font-medium text-success font-mono">
                    <span className="w-2 h-2 rounded-full bg-success" />
                    <span>Livre</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-medium text-info font-mono">
                    <span className="w-2 h-2 rounded-full bg-info" />
                    <span>Agendado</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-medium text-warning font-mono">
                    <span className="w-2 h-2 rounded-full bg-warning" />
                    <span>Bloqueado</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-medium text-fg/40 font-mono">
                    <span className="w-2 h-2 rounded-full bg-fg/30" />
                    <span>Passado</span>
                  </div>
                </div>
              </div>

              {loadingHorariosModal ? (
                <div className="py-16 text-center text-fg/40 text-xs font-mono">
                  Carregando horários das quadras...
                </div>
              ) : (
                (() => {
                  const quadrasBase =
                    quadraSelecionadaAgendaId === 'TODAS'
                      ? minhasQuadras
                      : minhasQuadras.filter((q) => q.id_quadra === quadraSelecionadaAgendaId);

                  const quadrasFiltradas = quadrasBase.filter((quadra) => {
                    if (statusFiltroModal === 'TODOS') return true;
                    const slots = horariosDisponiveisPorQuadra[quadra.id_quadra] || [];
                    const countLivres = slots.filter((s) => s.status === 'DISPONIVEL' || s.disponivel).length;
                    const countOcupados = slots.filter((s) => s.status === 'AGENDADO' || (!s.disponivel && s.motivo?.toLowerCase().includes('ocupado'))).length;
                    const countBloqueados = slots.filter((s) => s.status === 'BLOQUEADO' || (!s.disponivel && (s.motivo?.toLowerCase().includes('bloque') || s.motivo?.toLowerCase().includes('limite')))).length;

                    if (statusFiltroModal === 'LIVRES') return countLivres > 0;
                    if (statusFiltroModal === 'AGENDADOS') return countOcupados > 0;
                    if (statusFiltroModal === 'BLOQUEADOS') return countBloqueados > 0;
                    return true;
                  });

                  if (quadrasFiltradas.length === 0) {
                    return (
                      <div className="py-12 text-center text-fg/40 text-xs font-mono bg-fg/[0.03] border border-fg/[0.06] rounded-2xl p-6">
                        Nenhuma quadra encontrada com horários{' '}
                        {statusFiltroModal === 'LIVRES' ? 'disponíveis' : statusFiltroModal === 'AGENDADOS' ? 'agendados' : 'bloqueados'}{' '}
                        para este dia.
                      </div>
                    );
                  }

                  return quadrasFiltradas.map((quadra) => {
                    const slots = horariosDisponiveisPorQuadra[quadra.id_quadra] || [];
                    const bloqueiosDestaQuadra = (mapaBloqueiosPorQuadra[quadra.id_quadra] || []).filter(
                      (b) => b.data === dataSelecionada
                    );

                    const countLivres = slots.filter((s) => s.status === 'DISPONIVEL' || s.disponivel).length;
                    const countOcupados = slots.filter((s) => s.status === 'AGENDADO' || (!s.disponivel && s.motivo?.toLowerCase().includes('ocupado'))).length;
                    const countBloqueados = slots.filter((s) => s.status === 'BLOQUEADO' || (!s.disponivel && (s.motivo?.toLowerCase().includes('bloque') || s.motivo?.toLowerCase().includes('limite')))).length;
                    const countPassados = slots.filter((s) => s.status === 'INDISPONIVEL' || (!s.disponivel && s.motivo?.toLowerCase().includes('passado'))).length;

                    return (
                      <div key={quadra.id_quadra} className="p-5 rounded-2xl bg-fg/[0.03] border border-fg/[0.06] space-y-3.5">
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-fg/[0.06] pb-3">
                          <div>
                            <h4 className="text-sm font-semibold text-fg flex items-center gap-2">
                              <span>{quadra.nome}</span>
                              <Badge variant={quadra.ativa ? 'success' : 'neutral'} withDot className="text-[9px]">
                                {quadra.ativa ? 'ATIVA' : 'INATIVA'}
                              </Badge>
                            </h4>
                            <p className="text-[11px] text-fg/50 font-mono">
                              {quadra.tipoEsporte.replace('_', ' ')} • R$ {quadra.valorHora.toFixed(2)}/h
                            </p>
                          </div>

                          <div className="flex flex-wrap items-center gap-2 text-xs font-mono">
                            <span className="text-success bg-success/10 border border-success/20 px-2 py-0.5 rounded-lg font-medium">
                              {countLivres} livres
                            </span>
                            <span className="text-info bg-info/10 border border-info/20 px-2 py-0.5 rounded-lg font-medium">
                              {countOcupados} agendados
                            </span>
                            {countBloqueados > 0 && (
                              <span className="text-warning bg-warning/10 border border-warning/20 px-2 py-0.5 rounded-lg font-medium">
                                {countBloqueados} bloqueados
                              </span>
                            )}
                            {countPassados > 0 && (
                              <span className="text-fg/40 bg-fg/[0.03] border border-fg/[0.1] px-2 py-0.5 rounded-lg">
                                {countPassados} passados
                              </span>
                            )}
                          </div>
                        </div>

                        {bloqueiosDestaQuadra.length > 0 && (
                          <div className="p-3 rounded-xl bg-warning/10 border border-warning/20 text-warning text-xs flex items-center gap-2">
                            <Ban className="w-4 h-4 text-warning shrink-0" />
                            <span className="text-[11px]">
                              <strong>Bloqueio ativo:</strong>{' '}
                              {bloqueiosDestaQuadra.map((b) => {
                                const horario = !b.horaInicio || !b.horaFim ? 'Dia todo' : `${b.horaInicio.slice(0, 5)} às ${b.horaFim.slice(0, 5)}`;
                                const mot = b.motivo ? ` (${b.motivo})` : '';
                                return `${horario}${mot}`;
                              }).join(', ')}
                            </span>
                          </div>
                        )}

                        {slots.length === 0 ? (
                          <div className="py-4 text-center text-xs text-fg/40 font-mono">
                            Sem funcionamento configurado para este dia da semana.
                          </div>
                        ) : (
                          <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-6 gap-2">
                            {slots.map((slot, sIdx) => {
                              const isLivre = slot.status === 'DISPONIVEL' || slot.disponivel;
                              const isOcupado = slot.status === 'AGENDADO' || (!slot.disponivel && slot.motivo?.toLowerCase().includes('ocupado'));
                              const isPassado = slot.status === 'INDISPONIVEL' || (!slot.disponivel && slot.motivo?.toLowerCase().includes('passado'));
                              const isBloqueado = !isLivre && !isOcupado && !isPassado;

                              const horaInicioSlot = slot.inicio.slice(0, 5);
                              const agendamentoCorrespondente = isOcupado
                                ? agendamentosAdmin.find((a) => {
                                    const matchQuadra = a.quadraId === quadra.id_quadra;
                                    const matchData = a.dataHoraInicio.startsWith(dataSelecionada);
                                    const aHoraInicio = a.dataHoraInicio.split('T')[1]?.substring(0, 5);
                                    const aHoraFim = a.dataHoraFim.split('T')[1]?.substring(0, 5);
                                    return matchQuadra && matchData && a.status !== 'CANCELADO' && horaInicioSlot >= aHoraInicio && horaInicioSlot < aHoraFim;
                                  })
                                : null;

                              const handleClickSlot = () => {
                                if (isOcupado && agendamentoCorrespondente) {
                                  onQuadraChange(quadra.id_quadra);
                                  onSelectHighlightedAgendamento(agendamentoCorrespondente.id_agendamento);
                                  
                                  const isAgPassado = parseDataHoraLocal(agendamentoCorrespondente.dataHoraFim) < new Date();
                                  onFiltroAgendaAdminChange(isAgPassado ? 'REALIZADOS' : 'ATIVOS');
                                  onVisualizacaoAbaChange('LISTA_RESERVAS');
                                }
                              };

                              return (
                                <div
                                  key={sIdx}
                                  onClick={handleClickSlot}
                                  className={`p-2.5 rounded-xl border flex flex-col items-center justify-center text-center transition ${
                                    isLivre
                                      ? 'bg-success/10 border-success/20 text-success hover:border-success/40'
                                      : isOcupado
                                      ? 'bg-info/15 border-info/30 text-info hover:border-info/50 hover:bg-info/25 cursor-pointer active:scale-95 group/slot'
                                      : isPassado
                                      ? 'bg-fg/[0.03] border-fg/[0.03] text-fg/30 opacity-40'
                                      : isBloqueado
                                      ? 'bg-warning/10 border-warning/20 text-warning'
                                      : 'bg-fg/[0.03] border-fg/[0.06] text-fg/40'
                                  }`}
                                  title={
                                    isOcupado
                                      ? `Agendado por ${agendamentoCorrespondente?.nomeUsuario || 'Atleta'}. Clique para ver a reserva.`
                                      : slot.motivo || (isLivre ? 'Livre para agendamento' : isPassado ? 'Horário já transcorrido (passado)' : 'Bloqueado pelo administrador')
                                  }
                                >
                                  <span className="text-xs font-semibold font-mono">
                                    {slot.inicio.slice(0, 5)} - {slot.fim.slice(0, 5)}
                                  </span>
                                  <span className={`text-[9px] font-medium uppercase mt-0.5 flex items-center gap-1 font-mono ${
                                    isOcupado ? 'group-hover/slot:underline' : ''
                                  }`}>
                                    <span>{isLivre ? 'Livre' : isOcupado ? 'Agendado' : isPassado ? 'Passado' : isBloqueado ? 'Bloqueado' : 'Indisponível'}</span>
                                    {isOcupado && <span className="text-[10px]">↗</span>}
                                  </span>
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>
                    );
                  });
                })()
              )}
            </div>
          ) : (
            <div className="space-y-5">
              {/* Abas de Filtro: Ativos / Realizados / Cancelados */}
              <div className="flex bg-fg/[0.03] p-1 rounded-xl border border-fg/[0.1] text-xs">
                <button
                  type="button"
                  onClick={() => onFiltroAgendaAdminChange('ATIVOS')}
                  className={`flex-1 py-1.5 px-2 font-medium rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                    filtroAgendaAdmin === 'ATIVOS'
                      ? 'bg-fg text-on-accent font-semibold shadow-sm'
                      : 'text-fg/60 hover:text-fg'
                  }`}
                >
                  <span>Ativos</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-medium ${
                    filtroAgendaAdmin === 'ATIVOS' ? 'bg-on-accent/10 text-on-accent' : 'bg-fg/[0.06] text-fg/60'
                  }`}>
                    {contadores.ATIVOS}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => onFiltroAgendaAdminChange('REALIZADOS')}
                  className={`flex-1 py-1.5 px-2 font-medium rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                    filtroAgendaAdmin === 'REALIZADOS'
                      ? 'bg-fg text-on-accent font-semibold shadow-sm'
                      : 'text-fg/60 hover:text-fg'
                  }`}
                >
                  <span>Realizados</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-medium ${
                    filtroAgendaAdmin === 'REALIZADOS' ? 'bg-on-accent/10 text-on-accent' : 'bg-fg/[0.06] text-fg/60'
                  }`}>
                    {contadores.REALIZADOS}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => onFiltroAgendaAdminChange('CANCELADOS')}
                  className={`flex-1 py-1.5 px-2 font-medium rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                    filtroAgendaAdmin === 'CANCELADOS'
                      ? 'bg-fg text-on-accent font-semibold shadow-sm'
                      : 'text-fg/60 hover:text-fg'
                  }`}
                >
                  <span>Cancelados</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-medium ${
                    filtroAgendaAdmin === 'CANCELADOS' ? 'bg-on-accent/10 text-on-accent' : 'bg-fg/[0.06] text-fg/60'
                  }`}>
                    {contadores.CANCELADOS}
                  </span>
                </button>
              </div>

              {status === 'loading' && agendamentosPaginados.length === 0 ? (
                <div className="py-16 flex flex-col items-center justify-center gap-3 text-fg/50 text-xs font-mono">
                  <Loader2 className="w-6 h-6 animate-spin text-fg/60" />
                  <span>Carregando reservas do dia...</span>
                </div>
              ) : status === 'error' && agendamentosPaginados.length === 0 ? (
                <div className="py-12 px-4 flex flex-col items-center justify-center text-center border border-danger/20 rounded-2xl bg-danger/5">
                  <p className="text-xs text-danger font-medium">
                    {error instanceof Error ? error.message : 'Falha ao carregar reservas.'}
                  </p>
                </div>
              ) : isEmpty ? (
                <EmptyState
                  icon={CalendarIcon}
                  title={
                    filtroAgendaAdmin === 'ATIVOS'
                      ? 'Nenhum jogo ativo agendado para este dia'
                      : filtroAgendaAdmin === 'REALIZADOS'
                      ? 'Nenhum jogo finalizado para este dia'
                      : 'Nenhum jogo cancelado para este dia'
                  }
                  description="Nenhuma reserva localizada com este status para o dia selecionado."
                  className="py-12"
                />
              ) : (
                <div className="space-y-3">
                  <div className="space-y-3">
                    {agendamentosPaginados.map((ag) => {
                      const agora = getAgoraBrasilia().agora;
                      const isCancelado = ag.status === 'CANCELADO';
                      const dataInicio = parseDataHoraLocal(ag.dataHoraInicio);
                      const isRetroativoOuEmAndamento = dataInicio <= agora;
                      const isPassado = parseDataHoraLocal(ag.dataHoraFim) < agora;
                      const horaInicio = ag.dataHoraInicio.split('T')[1]?.substring(0, 5);
                      const horaFim = ag.dataHoraFim.split('T')[1]?.substring(0, 5);
                      const quadraCorrespondente = minhasQuadras.find((q) => q.id_quadra === ag.quadraId);
                      const isHighlighted = ag.id_agendamento === highlightedAgendamentoId;

                      return (
                        <div
                          key={ag.id_agendamento}
                          id={`agendamento-card-${ag.id_agendamento}`}
                          className={`p-4 rounded-2xl border space-y-2.5 transition-colors duration-200 ${
                            isHighlighted
                              ? 'bg-fg/[0.06] border-info/60 shadow-lg shadow-info/5'
                              : 'bg-fg/[0.03] border-fg/[0.06] hover:border-fg/20'
                          }`}
                        >
                          <div className="flex items-start justify-between gap-3">
                            <div className="space-y-1">
                              <div className="text-sm font-semibold text-fg flex items-center gap-1.5">
                                <ShieldCheck className="w-3.5 h-3.5 text-fg/70" />
                                {ag.nomeQuadra}
                              </div>

                              <div className="text-xs text-fg/50">
                                Atleta: <strong className="text-fg/80">{ag.nomeUsuario}</strong>
                              </div>

                              {ag.telefoneUsuario && (
                                <div className="text-xs text-fg/40 flex items-center gap-1.5 font-mono">
                                  <Phone className="w-3 h-3 text-fg/40" />
                                  <span>{ag.telefoneUsuario}</span>
                                </div>
                              )}

                              {ag.status === 'PENDENTE' && !isPassado && (() => {
                                const tempo = getTempoRestantePix(ag.criadoEm);
                                return tempo ? (
                                  <div className="text-[11px] text-warning font-mono flex items-center gap-1.5 mt-1 font-medium">
                                    <Clock className="w-3 h-3 animate-pulse text-warning" />
                                    <span>Aguardando Pix ({tempo})</span>
                                  </div>
                                ) : (
                                  <div className="text-[11px] text-danger font-mono flex items-center gap-1.5 mt-1 font-medium">
                                    <Clock className="w-3 h-3 text-danger" />
                                    <span>Pix expirado</span>
                                  </div>
                                );
                              })()}
                            </div>

                            <div className="flex items-center gap-2 shrink-0">
                              {quadraCorrespondente && (
                                <button
                                  type="button"
                                  onClick={() => onVerQuadra(quadraCorrespondente)}
                                  title="Ver fotos e informações completas desta quadra"
                                  className="p-1 rounded-xl bg-fg/[0.03] hover:bg-fg/[0.1] text-fg/70 hover:text-fg border border-fg/[0.1] transition active:scale-95 flex items-center gap-1 text-[11px] font-medium px-2.5 py-1 cursor-pointer font-mono"
                                >
                                  <Info className="w-3.5 h-3.5 text-fg/50" />
                                  <span>Ver Quadra</span>
                                </button>
                              )}

                              <Badge variant={isCancelado ? 'outline' : isPassado ? 'neutral' : ag.status === 'PENDENTE' ? 'warning' : 'success'} withDot>
                                {isCancelado ? 'CANCELADO' : isPassado ? 'REALIZADO' : ag.status}
                              </Badge>
                            </div>
                          </div>

                          <div className="flex items-center gap-2 text-xs text-fg/60 font-mono">
                            <Clock className="w-3.5 h-3.5 text-fg/40" />
                            <span>{horaInicio} às {horaFim}</span>
                          </div>

                          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-fg/40 font-mono">
                            {ag.criadoEm && (
                              <span>Agendado em: <strong className="text-fg/60 font-normal">{formatarDataHoraBr(ag.criadoEm)}</strong></span>
                            )}
                            {isCancelado && (
                              <span className="text-danger/90">
                                Cancelado em: <strong className="text-danger font-medium">{ag.canceladoEm ? formatarDataHoraBr(ag.canceladoEm) : '—'}</strong>
                              </span>
                            )}
                          </div>

                          <div className="flex flex-col sm:flex-row sm:items-center justify-between pt-2 border-t border-fg/[0.06] text-xs gap-2">
                            <span className="text-fg font-mono font-semibold">R$ {ag.valorTotal.toFixed(2)}</span>
                            {!isCancelado && (
                              isRetroativoOuEmAndamento ? (
                                <span className="text-[11px] text-fg/40 italic">
                                  Não é possível cancelar um agendamento que está em andamento ou retroativo.
                                </span>
                              ) : (
                                <button
                                  type="button"
                                  onClick={() =>
                                    onCancelarAgendamento(ag.id_agendamento, () => {
                                      reload();
                                      carregarContadores();
                                    })
                                  }
                                  className="text-xs text-danger hover:text-danger/80 font-medium transition underline underline-offset-2 cursor-pointer active:scale-95"
                                >
                                  Cancelar Agendamento
                                </button>
                              )
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  <Pagination
                    page={page}
                    totalPages={totalPages}
                    totalElements={totalElements}
                    isLoading={status === 'loading'}
                    onPageChange={setPage}
                  />
                </div>
              )}
            </div>
          )}
        </div>

        {/* Rodapé do Modal */}
        <div className="mt-auto shrink-0 p-4 border-t border-fg/[0.06] bg-fg/[0.03] flex justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            className="cursor-pointer"
          >
            Fechar
          </Button>
        </div>
      </div>
    </div>,
    document.body
  );
};
