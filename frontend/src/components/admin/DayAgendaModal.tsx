import React from 'react';
import { Quadra, Agendamento, HorarioDisponivel, BloqueioHorario } from '../../types';
import { Badge, EmptyState } from '../ui';
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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-zinc-950 border border-zinc-800 rounded-2xl w-full max-w-3xl max-h-[90vh] flex flex-col shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200">
        {/* Header do Modal */}
        <div className="p-5 sm:p-6 border-b border-zinc-850 flex items-center justify-between">
          <div>
            <h3 className="text-lg font-bold text-white flex items-center gap-2">
              <Clock className="w-5 h-5 text-emerald-400" />
              Agenda do Dia
            </h3>
            <p className="text-xs text-zinc-400 mt-0.5">
              Visualizando reservas e disponibilidade para <strong className="text-white font-mono">{dataSelecionada.split('-').reverse().join('/')}</strong>
            </p>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-full bg-zinc-900 hover:bg-zinc-850 text-zinc-400 hover:text-white border border-zinc-800 transition active:scale-95"
            title="Fechar janela"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Sub-header com Seletor de Quadra, Filtros e Abas de Visualização */}
        <div className="px-5 sm:px-6 py-3 border-b border-zinc-850/80 bg-zinc-900/30 flex flex-col md:flex-row md:items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2 shrink-0">
              <span className="text-xs text-zinc-400 font-medium">Quadra:</span>
              <select
                value={quadraSelecionadaAgendaId}
                onChange={(e) => {
                  const val = e.target.value;
                  onQuadraChange(val === 'TODAS' ? 'TODAS' : Number(val));
                }}
                className="bg-zinc-900 border border-zinc-800 text-xs text-white rounded-xl px-3 py-1.5 focus:outline-none focus:border-emerald-400 transition"
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
              <div className="flex items-center gap-1 bg-zinc-900 border border-zinc-800 p-1 rounded-xl text-xs shrink-0">
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('TODOS')}
                  className={`px-2.5 py-1 rounded-lg font-medium transition ${
                    statusFiltroModal === 'TODOS'
                      ? 'bg-zinc-750 text-white shadow-sm font-semibold'
                      : 'text-zinc-400 hover:text-white'
                  }`}
                >
                  Todos
                </button>
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('LIVRES')}
                  className={`px-2 py-1 rounded-lg font-medium transition flex items-center gap-1.5 ${
                    statusFiltroModal === 'LIVRES'
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 font-semibold'
                      : 'text-zinc-400 hover:text-emerald-400'
                  }`}
                  title="Exibir apenas quadras com horários disponíveis"
                >
                  <span className="w-2 h-2 rounded-full bg-emerald-400" />
                  <span>Disponíveis</span>
                </button>
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('AGENDADOS')}
                  className={`px-2 py-1 rounded-lg font-medium transition flex items-center gap-1.5 ${
                    statusFiltroModal === 'AGENDADOS'
                      ? 'bg-blue-500/20 text-blue-300 border border-blue-500/40 font-semibold'
                      : 'text-zinc-400 hover:text-blue-400'
                  }`}
                  title="Exibir apenas quadras com horários agendados"
                >
                  <span className="w-2 h-2 rounded-full bg-blue-400" />
                  <span>Agendados</span>
                </button>
                <button
                  type="button"
                  onClick={() => onStatusFiltroChange('BLOQUEADOS')}
                  className={`px-2 py-1 rounded-lg font-medium transition flex items-center gap-1.5 ${
                    statusFiltroModal === 'BLOQUEADOS'
                      ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40 font-semibold'
                      : 'text-zinc-400 hover:text-amber-400'
                  }`}
                  title="Exibir apenas quadras com horários bloqueados"
                >
                  <Ban className="w-3 h-3 text-amber-400" />
                  <span>Bloqueados</span>
                </button>
              </div>
            )}
          </div>

          {/* Toggle de Abas: Grade de Horários vs Reservas */}
          <div className="flex items-center bg-zinc-900 p-1 rounded-xl border border-zinc-800 text-xs shrink-0 self-start md:self-center">
            <button
              type="button"
              onClick={() => onVisualizacaoAbaChange('GRADE_HORARIOS')}
              className={`px-3 py-1.5 font-semibold rounded-lg transition-all flex items-center gap-1.5 whitespace-nowrap active:scale-95 ${
                visualizacaoAgendaAba === 'GRADE_HORARIOS'
                  ? 'bg-white text-zinc-950 shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              <CalendarIcon className="w-3.5 h-3.5" />
              <span>Grade</span>
            </button>
            <button
              type="button"
              onClick={() => onVisualizacaoAbaChange('LISTA_RESERVAS')}
              className={`px-3 py-1.5 font-semibold rounded-lg transition-all flex items-center gap-1.5 whitespace-nowrap active:scale-95 ${
                visualizacaoAgendaAba === 'LISTA_RESERVAS'
                  ? 'bg-white text-zinc-950 shadow-sm'
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
              <div className="flex flex-wrap items-center justify-between gap-3 text-xs bg-zinc-900/60 border border-zinc-800 p-3 rounded-xl">
                <div className="flex flex-wrap items-center gap-4">
                  <div className="flex items-center gap-1.5 font-medium text-emerald-400">
                    <span className="w-2.5 h-2.5 rounded-full bg-emerald-400" />
                    <span>Livre (Disponível)</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-medium text-blue-400">
                    <span className="w-2.5 h-2.5 rounded-full bg-blue-400" />
                    <span>Agendado / Ocupado</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-medium text-amber-400">
                    <span className="w-2.5 h-2.5 rounded-full bg-amber-400" />
                    <span>Bloqueado pelo Admin</span>
                  </div>
                  <div className="flex items-center gap-1.5 font-medium text-zinc-400">
                    <span className="w-2.5 h-2.5 rounded-full bg-zinc-500" />
                    <span>Data / Horário Passado</span>
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
                      <div className="py-12 text-center text-zinc-500 text-xs font-mono bg-zinc-900/30 border border-zinc-850 rounded-2xl p-6">
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
                      <div key={quadra.id_quadra} className="p-4 rounded-2xl bg-zinc-900/40 border border-zinc-850 space-y-3">
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-zinc-850/60 pb-2.5">
                          <div>
                            <h4 className="text-sm font-bold text-white flex items-center gap-2">
                              <span>{quadra.nome}</span>
                              <Badge variant={quadra.ativa ? 'success' : 'outline'} className="text-[9px]">
                                {quadra.ativa ? 'ATIVA' : 'INATIVA'}
                              </Badge>
                            </h4>
                            <p className="text-[11px] text-zinc-400 font-mono">
                              {quadra.tipoEsporte.replace('_', ' ')} • R$ {quadra.valorHora.toFixed(2)}/h
                            </p>
                          </div>

                          <div className="flex flex-wrap items-center gap-2 text-xs font-mono">
                            <span className="text-emerald-400 bg-emerald-950/40 border border-emerald-500/30 px-2 py-0.5 rounded-lg">
                              {countLivres} livres
                            </span>
                            <span className="text-blue-400 bg-blue-950/40 border border-blue-500/30 px-2 py-0.5 rounded-lg">
                              {countOcupados} agendados
                            </span>
                            {countBloqueados > 0 && (
                              <span className="text-amber-400 bg-amber-950/40 border border-amber-500/30 px-2 py-0.5 rounded-lg">
                                {countBloqueados} bloqueados
                              </span>
                            )}
                            {countPassados > 0 && (
                              <span className="text-zinc-400 bg-zinc-800/40 border border-zinc-700/40 px-2 py-0.5 rounded-lg">
                                {countPassados} passados
                              </span>
                            )}
                          </div>
                        </div>

                        {bloqueiosDestaQuadra.length > 0 && (
                          <div className="p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs flex items-center gap-2">
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
                                  className={`p-2 rounded-xl border flex flex-col items-center justify-center text-center transition ${
                                    isLivre
                                      ? 'bg-emerald-950/30 border-emerald-500/30 text-emerald-400 hover:border-emerald-500/60'
                                      : isOcupado
                                      ? 'bg-blue-950/40 border-blue-500/40 text-blue-300 hover:border-blue-400 hover:bg-blue-900/40 cursor-pointer shadow-sm active:scale-95 group/slot'
                                      : isPassado
                                      ? 'bg-zinc-900/40 border-zinc-800 text-zinc-500 opacity-60'
                                      : isBloqueado
                                      ? 'bg-amber-950/30 border-amber-500/30 text-amber-300'
                                      : 'bg-zinc-900 border-zinc-800 text-zinc-400'
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
                                  <span className={`text-[9px] font-semibold uppercase mt-0.5 flex items-center gap-1 ${
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
              <div className="flex bg-zinc-900/90 p-1 rounded-xl border border-zinc-800 text-xs">
                <button
                  type="button"
                  onClick={() => onFiltroAgendaAdminChange('ATIVOS')}
                  className={`flex-1 py-1.5 px-2 font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 active:scale-[0.98] ${
                    filtroAgendaAdmin === 'ATIVOS'
                      ? 'bg-white text-zinc-950 shadow-sm'
                      : 'text-zinc-400 hover:text-white'
                  }`}
                >
                  <span>Ativos</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold ${
                    filtroAgendaAdmin === 'ATIVOS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-400'
                  }`}>
                    {contadoresAtivos}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => onFiltroAgendaAdminChange('REALIZADOS')}
                  className={`flex-1 py-1.5 px-2 font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 active:scale-[0.98] ${
                    filtroAgendaAdmin === 'REALIZADOS'
                      ? 'bg-white text-zinc-950 shadow-sm'
                      : 'text-zinc-400 hover:text-white'
                  }`}
                >
                  <span>Realizados</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold ${
                    filtroAgendaAdmin === 'REALIZADOS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-400'
                  }`}>
                    {contadoresRealizados}
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => onFiltroAgendaAdminChange('CANCELADOS')}
                  className={`flex-1 py-1.5 px-2 font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 active:scale-[0.98] ${
                    filtroAgendaAdmin === 'CANCELADOS'
                      ? 'bg-white text-zinc-950 shadow-sm'
                      : 'text-zinc-400 hover:text-white'
                  }`}
                >
                  <span>Cancelados</span>
                  <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold ${
                    filtroAgendaAdmin === 'CANCELADOS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-400'
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
                  <div className="py-12 px-4 flex flex-col items-center justify-center text-center border border-zinc-850 rounded-2xl bg-zinc-900/30">
                    <div className="w-12 h-12 rounded-full bg-purple-950/40 border border-purple-800/40 flex items-center justify-center text-purple-400 mb-3 shadow-inner">
                      <History className="w-6 h-6" />
                    </div>
                    <h4 className="text-sm font-semibold text-white">Jogos Finalizados</h4>
                    <p className="text-xs text-zinc-400 max-w-sm mt-1 mb-4">
                      Existem {contadoresRealizados} {contadoresRealizados === 1 ? 'reserva concluída' : 'reservas concluídas'} neste dia. Clique abaixo para visualizar os detalhes.
                    </p>
                    <button
                      type="button"
                      onClick={() => setRealizadosCarregados(true)}
                      className="px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl text-xs font-semibold flex items-center gap-2 border border-zinc-700 hover:border-zinc-600 transition active:scale-95 shadow-sm"
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
                  <div className="py-12 px-4 flex flex-col items-center justify-center text-center border border-zinc-850 rounded-2xl bg-zinc-900/30">
                    <div className="w-12 h-12 rounded-full bg-rose-950/40 border border-rose-800/40 flex items-center justify-center text-rose-400 mb-3 shadow-inner">
                      <Ban className="w-6 h-6" />
                    </div>
                    <h4 className="text-sm font-semibold text-white">Reservas Canceladas</h4>
                    <p className="text-xs text-zinc-400 max-w-sm mt-1 mb-4">
                      Existem {contadoresCancelados} {contadoresCancelados === 1 ? 'reserva cancelada' : 'reservas canceladas'} neste dia. Clique abaixo para visualizar os detalhes.
                    </p>
                    <button
                      type="button"
                      onClick={() => setCanceladosCarregados(true)}
                      className="px-4 py-2 bg-zinc-900 hover:bg-zinc-800 text-white rounded-xl text-xs font-semibold flex items-center gap-2 border border-zinc-700 hover:border-zinc-600 transition active:scale-95 shadow-sm"
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
                        className={`p-4 rounded-xl border space-y-2.5 transition-all duration-300 ${
                          isHighlighted
                            ? 'bg-emerald-950/60 border-emerald-400 ring-2 ring-emerald-400 shadow-xl shadow-emerald-950/80 scale-[1.01]'
                            : 'bg-zinc-900/60 border-zinc-850 hover:border-zinc-700'
                        }`}
                      >
                        <div className="flex items-start justify-between gap-3">
                          <div className="space-y-1">
                            <div className="text-sm font-bold text-white flex items-center gap-1.5">
                              <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
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
                                <div className="text-[11px] text-red-400 font-mono flex items-center gap-1.5 mt-1 font-semibold">
                                  <Clock className="w-3 h-3 text-red-400" />
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
                                className="p-1 rounded-lg bg-zinc-900 hover:bg-zinc-800 text-zinc-300 hover:text-white border border-zinc-800 hover:border-zinc-700 transition active:scale-95 flex items-center gap-1 text-[11px] font-semibold px-2.5 py-1"
                              >
                                <Info className="w-3.5 h-3.5 text-emerald-400" />
                                <span>Ver Quadra</span>
                              </button>
                            )}

                            <Badge variant={isCancelado ? 'outline' : isPassado ? 'neutral' : ag.status === 'PENDENTE' ? 'warning' : 'success'}>
                              {isCancelado ? 'CANCELADO' : isPassado ? 'REALIZADO' : ag.status}
                            </Badge>
                          </div>
                        </div>

                        <div className="flex items-center gap-2 text-xs text-zinc-300 font-mono">
                          <Clock className="w-3.5 h-3.5 text-zinc-400" />
                          <span>{horaInicio} às {horaFim}</span>
                        </div>

                        <div className="flex items-center justify-between pt-2 border-t border-zinc-850 text-xs">
                          <span className="text-zinc-300 font-mono font-semibold">R$ {ag.valorTotal.toFixed(2)}</span>
                          {!isCancelado && !isPassado && (
                            <button
                              type="button"
                              onClick={() => onCancelarAgendamento(ag.id_agendamento)}
                              className="text-xs text-red-400 hover:text-red-300 font-semibold transition underline underline-offset-2 active:scale-95"
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
        <div className="p-4 border-t border-zinc-850 bg-zinc-950 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-zinc-900 hover:bg-zinc-850 text-zinc-300 hover:text-white border border-zinc-800 transition active:scale-95"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
};
