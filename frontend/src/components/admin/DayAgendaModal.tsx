import React from 'react';
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
  History
} from 'lucide-react';
import { parseDataHoraLocal } from '../../utils/dateUtils';

interface DayAgendaModalProps {
  isOpen: boolean;
  dataSelecionada: string;
  minhasQuadras: Quadra[];
  agendamentosAdmin: Agendamento[];
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
  onCancelarAgendamento: (id: number) => void;
  onVerQuadra: (quadra: Quadra) => void;
  getTempoRestantePix: (criadoEm: string) => string | null;
}

export const DayAgendaModal: React.FC<DayAgendaModalProps> = ({
  isOpen,
  dataSelecionada,
  minhasQuadras,
  agendamentosAdmin,
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
  const [realizadosCarregados, setRealizadosCarregados] = React.useState(false);
  const [canceladosCarregados, setCanceladosCarregados] = React.useState(false);

  React.useEffect(() => {
    setRealizadosCarregados(false);
    setCanceladosCarregados(false);
  }, [dataSelecionada]);

  React.useEffect(() => {
    if (highlightedAgendamentoId && isOpen) {
      const ag = agendamentosAdmin.find((a) => a.id_agendamento === highlightedAgendamentoId);
      if (ag) {
        const isCancelado = ag.status === 'CANCELADO';
        const isPassado = parseDataHoraLocal(ag.dataHoraFim) < new Date();
        if (isCancelado) {
          setCanceladosCarregados(true);
        } else if (isPassado) {
          setRealizadosCarregados(true);
        }
      }

      const timer = setTimeout(() => {
        const el = document.getElementById(`agendamento-card-${highlightedAgendamentoId}`);
        if (el) {
          el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
      }, 200);
      return () => clearTimeout(timer);
    }
  }, [highlightedAgendamentoId, isOpen, visualizacaoAgendaAba, filtroAgendaAdmin, agendamentosAdmin]);

  if (!isOpen) return null;

  const agendamentosDoDia = agendamentosAdmin
    .filter((a) => {
      const matchData = a.dataHoraInicio.startsWith(dataSelecionada);
      const matchQuadra = quadraSelecionadaAgendaId === 'TODAS' || a.quadraId === quadraSelecionadaAgendaId;
      return matchData && matchQuadra;
    })
    .sort((a, b) => a.dataHoraInicio.localeCompare(b.dataHoraInicio));

  const agoraLocal = new Date();

  const agendamentosDoDiaFiltrados = agendamentosDoDia.filter((ag) => {
    const dataFim = parseDataHoraLocal(ag.dataHoraFim);
    const isCancelado = ag.status === 'CANCELADO';
    const isPassado = dataFim < agoraLocal;

    if (filtroAgendaAdmin === 'CANCELADOS') return isCancelado;
    if (filtroAgendaAdmin === 'REALIZADOS') return !isCancelado && isPassado;
    return !isCancelado && !isPassado;
  });

  let contadoresAtivos = 0;
  let contadoresCancelados = 0;
  let contadoresRealizados = 0;

  agendamentosDoDia.forEach((ag) => {
    const dataFim = parseDataHoraLocal(ag.dataHoraFim);
    const isCancelado = ag.status === 'CANCELADO';
    const isPassado = dataFim < agoraLocal;
    if (isCancelado) contadoresCancelados++;
    else if (isPassado) contadoresRealizados++;
    else contadoresAtivos++;
  });

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-surface-950/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="bg-surface-900 border border-surface-800 rounded-3xl w-full max-w-3xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200">
        {/* Header do Modal */}
        <div className="p-5 sm:p-6 border-b border-surface-800 flex items-center justify-between bg-surface-950/80">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-brand-500/10 border border-brand-500/20 text-brand-400 flex items-center justify-center">
              <Clock className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-bold text-white tracking-tight">
                Agenda do Dia
              </h3>
              <p className="text-xs text-zinc-400 mt-0.5">
                Visualizando disponibilidade para <strong className="text-white font-mono">{dataSelecionada.split('-').reverse().join('/')}</strong>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-surface-900 hover:bg-surface-800 text-zinc-400 hover:text-white border border-surface-800 transition cursor-pointer"
            title="Fechar janela"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Sub-header com Seletor de Quadra, Filtros e Abas de Visualização */}
        <div className="px-5 sm:px-6 py-3 border-b border-surface-800/80 bg-surface-950/60 flex flex-col md:flex-row md:items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 shrink-0">
              <span className="text-xs text-zinc-400 font-medium font-mono">Quadra:</span>
              <select
                value={quadraSelecionadaAgendaId}
                onChange={(e) => {
                  const val = e.target.value;
                  onQuadraChange(val === 'TODAS' ? 'TODAS' : Number(val));
                }}
                className="bg-surface-900 border border-surface-700 text-xs text-white rounded-xl px-3 py-1.5 focus:outline-none focus:border-brand-400 transition font-mono"
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
              <div className="flex items-center gap-1 bg-surface-900 border border-surface-800 p-1 rounded-xl text-xs shrink-0">
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('TODOS')}
                  className={`px-2.5 py-1 rounded-lg font-medium transition cursor-pointer ${
                    statusFiltroModal === 'TODOS'
                      ? 'bg-surface-800 text-white font-semibold'
                      : 'text-zinc-400 hover:text-white'
                  }`}
                >
                  Todos
                </button>
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('LIVRES')}
                  className={`px-2.5 py-1 rounded-lg font-medium transition flex items-center gap-1.5 cursor-pointer ${
                    statusFiltroModal === 'LIVRES'
                      ? 'bg-brand-500/20 text-brand-300 border border-brand-500/30 font-semibold'
                      : 'text-zinc-400 hover:text-brand-400'
                  }`}
                  title="Exibir apenas quadras com horários disponíveis"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-brand-400" />
                  <span>Livres</span>
                </button>
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('AGENDADOS')}
                  className={`px-2.5 py-1 rounded-lg font-medium transition flex items-center gap-1.5 cursor-pointer ${
                    statusFiltroModal === 'AGENDADOS'
                      ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30 font-semibold'
                      : 'text-zinc-400 hover:text-sky-400'
                  }`}
                  title="Exibir apenas quadras com horários agendados"
                >
                  <span className="w-1.5 h-1.5 rounded-full bg-sky-400" />
                  <span>Agendados</span>
                </button>
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('BLOQUEADOS')}
                  className={`px-2.5 py-1 rounded-lg font-medium transition flex items-center gap-1.5 cursor-pointer ${
                    statusFiltroModal === 'BLOQUEADOS'
                      ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30 font-semibold'
                      : 'text-zinc-400 hover:text-rose-400'
                  }`}
                  title="Exibir apenas quadras com horários bloqueados"
                >
                  <Ban className="w-3 h-3 text-rose-400" />
                  <span>Bloqueados</span>
                </button>
              </div>
            )}
          </div>

          {/* Toggle de Abas: Grade de Horários vs Reservas */}
          <div className="flex items-center bg-surface-900 p-1 rounded-xl border border-surface-800 text-xs shrink-0 self-start md:self-center">
            <button
              type="button"
              onClick={() => onVisualizacaoAbaChange('GRADE_HORARIOS')}
              className={`px-3 py-1.5 font-semibold rounded-lg transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                visualizacaoAgendaAba === 'GRADE_HORARIOS'
                  ? 'bg-brand-400 text-surface-950 font-bold shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              <CalendarIcon className="w-3.5 h-3.5" />
              <span>Grade</span>
            </button>
            <button
              type="button"
              onClick={() => onVisualizacaoAbaChange('LISTA_RESERVAS')}
              className={`px-3 py-1.5 font-semibold rounded-lg transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer ${
                visualizacaoAgendaAba === 'LISTA_RESERVAS'
                  ? 'bg-brand-400 text-surface-950 font-bold shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              <Users className="w-3.5 h-3.5" />
              <span>Reservas ({agendamentosDoDiaFiltrados.length})</span>
            </button>
          </div>
        </div>

        {/* Conteúdo do Modal */}
        <div className="p-5 sm:p-6 overflow-y-auto space-y-6 scrollbar-thin">
          {visualizacaoAgendaAba === 'GRADE_HORARIOS' ? (
            <div className="space-y-6">
              {/* Legenda Resumida */}
              <div className="flex flex-wrap items-center justify-between gap-3 text-xs bg-surface-950/60 border border-surface-800 p-3 rounded-2xl">
                <div className="flex flex-wrap items-center gap-4">
                  <div className="flex items-center gap-1.5 font-medium text-brand-400 font-mono">
                    <span className="w-2 h-2 rounded-full bg-brand-400" />
                    <span>Livre</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-medium text-sky-400 font-mono">
                    <span className="w-2 h-2 rounded-full bg-sky-400" />
                    <span>Agendado</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-medium text-amber-400 font-mono">
                    <span className="w-2 h-2 rounded-full bg-amber-400" />
                    <span>Bloqueado</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-medium text-zinc-500 font-mono">
                    <span className="w-2 h-2 rounded-full bg-zinc-600" />
                    <span>Passado</span>
                  </div>
                </div>
              </div>

              {loadingHorariosModal ? (
                <div className="py-16 text-center text-zinc-500 text-xs font-mono">
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
                      <div className="py-12 text-center text-zinc-500 text-xs font-mono bg-surface-950/40 border border-surface-800 rounded-2xl p-6">
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
                      <div key={quadra.id_quadra} className="p-5 rounded-2xl bg-surface-950/60 border border-surface-800 space-y-3.5">
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-surface-800/60 pb-3">
                          <div>
                            <h4 className="text-sm font-bold text-white flex items-center gap-2">
                              <span>{quadra.nome}</span>
                              <Badge variant={quadra.ativa ? 'success' : 'neutral'} withDot className="text-[9px]">
                                {quadra.ativa ? 'ATIVA' : 'INATIVA'}
                              </Badge>
                            </h4>
                            <p className="text-[11px] text-zinc-400 font-mono">
                              {quadra.tipoEsporte.replace('_', ' ')} • R$ {quadra.valorHora.toFixed(2)}/h
                            </p>
                          </div>

                          <div className="flex flex-wrap items-center gap-2 text-xs font-mono">
                            <span className="text-brand-400 bg-brand-500/10 border border-brand-500/20 px-2 py-0.5 rounded-lg font-semibold">
                              {countLivres} livres
                            </span>
                            <span className="text-sky-400 bg-sky-500/10 border border-sky-500/20 px-2 py-0.5 rounded-lg font-semibold">
                              {countOcupados} agendados
                            </span>
                            {countBloqueados > 0 && (
                              <span className="text-amber-400 bg-amber-500/10 border border-amber-500/20 px-2 py-0.5 rounded-lg font-semibold">
                                {countBloqueados} bloqueados
                              </span>
                            )}
                            {countPassados > 0 && (
                              <span className="text-zinc-400 bg-surface-800 border border-surface-700 px-2 py-0.5 rounded-lg">
                                {countPassados} passados
                              </span>
                            )}
                          </div>
                        </div>

                        {bloqueiosDestaQuadra.length > 0 && (
                          <div className="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs flex items-center gap-2">
                            <Ban className="w-4 h-4 text-amber-400 shrink-0" />
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
                          <div className="py-4 text-center text-xs text-zinc-500 font-mono">
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
                                  if (isAgPassado) {
                                    setRealizadosCarregados(true);
                                  }
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
                                      ? 'bg-brand-500/10 border-brand-500/20 text-brand-400 hover:border-brand-500/40'
                                      : isOcupado
                                      ? 'bg-sky-500/15 border-sky-500/30 text-sky-300 hover:border-sky-400 hover:bg-sky-500/25 cursor-pointer shadow-sm active:scale-95 group/slot'
                                      : isPassado
                                      ? 'bg-surface-900/40 border-surface-800 text-zinc-500 opacity-50'
                                      : isBloqueado
                                      ? 'bg-amber-500/10 border-amber-500/20 text-amber-300'
                                      : 'bg-surface-900 border-surface-800 text-zinc-400'
                                  }`}
                                  title={
                                    isOcupado
                                      ? `Agendado por ${agendamentoCorrespondente?.nomeUsuario || 'Atleta'}. Clique para ver a reserva.`
                                      : slot.motivo || (isLivre ? 'Livre para agendamento' : isPassado ? 'Horário já transcorrido (passado)' : 'Bloqueado pelo administrador')
                                  }
                                >
                                  <span className="text-xs font-bold font-mono">
                                    {slot.inicio.slice(0, 5)} - {slot.fim.slice(0, 5)}
                                  </span>
                                  <span className={`text-[9px] font-semibold uppercase mt-0.5 flex items-center gap-1 font-mono ${
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
              <div className="flex bg-surface-950 p-1 rounded-2xl border border-surface-800 text-xs">
                <button
                  type="button"
                  onClick={() => onFiltroAgendaAdminChange('ATIVOS')}
                  className={`flex-1 py-1.5 px-2 font-semibold rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                    filtroAgendaAdmin === 'ATIVOS'
                      ? 'bg-brand-400 text-surface-950 font-bold shadow-sm'
                      : 'text-zinc-400 hover:text-white'
                  }`}
                >
                  <span>Ativos</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold ${
                    filtroAgendaAdmin === 'ATIVOS' ? 'bg-surface-950/20 text-surface-950' : 'bg-surface-850 text-zinc-400'
                  }`}>
                    {contadoresAtivos}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => onFiltroAgendaAdminChange('REALIZADOS')}
                  className={`flex-1 py-1.5 px-2 font-semibold rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                    filtroAgendaAdmin === 'REALIZADOS'
                      ? 'bg-brand-400 text-surface-950 font-bold shadow-sm'
                      : 'text-zinc-400 hover:text-white'
                  }`}
                >
                  <span>Realizados</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold ${
                    filtroAgendaAdmin === 'REALIZADOS' ? 'bg-surface-950/20 text-surface-950' : 'bg-surface-850 text-zinc-400'
                  }`}>
                    {contadoresRealizados}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => onFiltroAgendaAdminChange('CANCELADOS')}
                  className={`flex-1 py-1.5 px-2 font-semibold rounded-xl transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                    filtroAgendaAdmin === 'CANCELADOS'
                      ? 'bg-brand-400 text-surface-950 font-bold shadow-sm'
                      : 'text-zinc-400 hover:text-white'
                  }`}
                >
                  <span>Cancelados</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold ${
                    filtroAgendaAdmin === 'CANCELADOS' ? 'bg-surface-950/20 text-surface-950' : 'bg-surface-850 text-zinc-400'
                  }`}>
                    {contadoresCancelados}
                  </span>
                </button>
              </div>

              {filtroAgendaAdmin === 'REALIZADOS' && !realizadosCarregados ? (
                contadoresRealizados === 0 ? (
                  <EmptyState
                    icon={CalendarIcon}
                    title="Nenhum jogo finalizado para este dia"
                    description="Nenhuma reserva localizada com este status para o dia selecionado."
                    className="py-12"
                  />
                ) : (
                  <div className="py-12 px-4 flex flex-col items-center justify-center text-center border border-surface-800 rounded-3xl bg-surface-950/40">
                    <div className="w-12 h-12 rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400 mb-3 shadow-inner">
                      <History className="w-6 h-6" />
                    </div>
                    <h4 className="text-sm font-semibold text-white">Jogos Finalizados</h4>
                    <p className="text-xs text-zinc-400 max-w-sm mt-1 mb-4">
                      Existem {contadoresRealizados} {contadoresRealizados === 1 ? 'reserva concluída' : 'reservas concluídas'} neste dia. Clique abaixo para visualizar os detalhes.
                    </p>
                    <button
                      type="button"
                      onClick={() => setRealizadosCarregados(true)}
                      className="px-4 py-2 bg-surface-900 hover:bg-surface-800 text-white rounded-xl text-xs font-semibold flex items-center gap-2 border border-surface-700 transition active:scale-95 cursor-pointer font-mono"
                    >
                      <History className="w-4 h-4 text-purple-400" />
                      <span>Carregar Realizados ({contadoresRealizados})</span>
                    </button>
                  </div>
                )
              ) : filtroAgendaAdmin === 'CANCELADOS' && !canceladosCarregados ? (
                contadoresCancelados === 0 ? (
                  <EmptyState
                    icon={CalendarIcon}
                    title="Nenhum jogo cancelado para este dia"
                    description="Nenhuma reserva localizada com este status para o dia selecionado."
                    className="py-12"
                  />
                ) : (
                  <div className="py-12 px-4 flex flex-col items-center justify-center text-center border border-surface-800 rounded-3xl bg-surface-950/40">
                    <div className="w-12 h-12 rounded-2xl bg-rose-500/10 border border-rose-500/20 flex items-center justify-center text-rose-400 mb-3 shadow-inner">
                      <Ban className="w-6 h-6" />
                    </div>
                    <h4 className="text-sm font-semibold text-white">Reservas Canceladas</h4>
                    <p className="text-xs text-zinc-400 max-w-sm mt-1 mb-4">
                      Existem {contadoresCancelados} {contadoresCancelados === 1 ? 'reserva cancelada' : 'reservas canceladas'} neste dia. Clique abaixo para visualizar os detalhes.
                    </p>
                    <button
                      type="button"
                      onClick={() => setCanceladosCarregados(true)}
                      className="px-4 py-2 bg-surface-900 hover:bg-surface-800 text-white rounded-xl text-xs font-semibold flex items-center gap-2 border border-surface-700 transition active:scale-95 cursor-pointer font-mono"
                    >
                      <Ban className="w-4 h-4 text-rose-400" />
                      <span>Carregar Cancelados ({contadoresCancelados})</span>
                    </button>
                  </div>
                )
              ) : agendamentosDoDiaFiltrados.length === 0 ? (
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
                <div className="space-y-3 max-h-[460px] overflow-y-auto pr-1 scrollbar-thin">
                  {agendamentosDoDiaFiltrados.map((ag) => {
                    const isCancelado = ag.status === 'CANCELADO';
                    const isPassado = parseDataHoraLocal(ag.dataHoraFim) < new Date();
                    const horaInicio = ag.dataHoraInicio.split('T')[1]?.substring(0, 5);
                    const horaFim = ag.dataHoraFim.split('T')[1]?.substring(0, 5);
                    const quadraCorrespondente = minhasQuadras.find((q) => q.id_quadra === ag.quadraId);
                    const isHighlighted = ag.id_agendamento === highlightedAgendamentoId;

                    return (
                      <div
                        key={ag.id_agendamento}
                        id={`agendamento-card-${ag.id_agendamento}`}
                        className={`p-4 rounded-2xl border space-y-2.5 transition-all duration-300 ${
                          isHighlighted
                            ? 'bg-brand-950/40 border-brand-400 ring-2 ring-brand-400/40 shadow-xl scale-[1.01]'
                            : 'bg-surface-950/60 border-surface-800 hover:border-surface-700'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="space-y-1">
                            <div className="text-sm font-bold text-white flex items-center gap-1.5">
                              <ShieldCheck className="w-3.5 h-3.5 text-brand-400" />
                              {ag.nomeQuadra}
                            </div>

                            <div className="text-xs text-zinc-400">
                              Atleta: <strong className="text-zinc-200">{ag.nomeUsuario}</strong>
                            </div>

                            {ag.telefoneUsuario && (
                              <div className="text-xs text-zinc-400 flex items-center gap-1.5 font-mono">
                                <Phone className="w-3 h-3 text-zinc-500" />
                                <span>{ag.telefoneUsuario}</span>
                              </div>
                            )}

                            {ag.status === 'PENDENTE' && !isPassado && (() => {
                              const tempo = getTempoRestantePix(ag.criadoEm);
                              return tempo ? (
                                <div className="text-[11px] text-amber-400 font-mono flex items-center gap-1.5 mt-1 font-semibold">
                                  <Clock className="w-3 h-3 animate-pulse text-amber-400" />
                                  <span>Aguardando Pix ({tempo})</span>
                                </div>
                              ) : (
                                <div className="text-[11px] text-rose-400 font-mono flex items-center gap-1.5 mt-1 font-semibold">
                                  <Clock className="w-3 h-3 text-rose-400" />
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
                                className="p-1 rounded-xl bg-surface-900 hover:bg-surface-800 text-zinc-300 hover:text-white border border-surface-700 transition active:scale-95 flex items-center gap-1 text-[11px] font-semibold px-2.5 py-1 cursor-pointer font-mono"
                              >
                                <Info className="w-3.5 h-3.5 text-brand-400" />
                                <span>Ver Quadra</span>
                              </button>
                            )}

                            <Badge variant={isCancelado ? 'outline' : isPassado ? 'neutral' : ag.status === 'PENDENTE' ? 'warning' : 'success'} withDot>
                              {isCancelado ? 'CANCELADO' : isPassado ? 'REALIZADO' : ag.status}
                            </Badge>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 text-xs text-zinc-300 font-mono">
                          <Clock className="w-3.5 h-3.5 text-zinc-400" />
                          <span>{horaInicio} às {horaFim}</span>
                        </div>

                        <div className="flex items-center justify-between pt-2 border-t border-surface-800/80 text-xs">
                          <span className="text-zinc-300 font-mono font-semibold">R$ {ag.valorTotal.toFixed(2)}</span>
                          {!isCancelado && !isPassado && (
                            <button
                              type="button"
                              onClick={() => onCancelarAgendamento(ag.id_agendamento)}
                              className="text-xs text-rose-400 hover:text-rose-300 font-semibold transition underline underline-offset-2 cursor-pointer active:scale-95"
                            >
                              Cancelar Agendamento
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Rodapé do Modal */}
        <div className="p-4 border-t border-surface-800 bg-surface-950/80 flex justify-end">
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
    </div>
  );
};
