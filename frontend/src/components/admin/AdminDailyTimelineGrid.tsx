import React from 'react';
import { Quadra, Agendamento, BloqueioHorario } from '../../types';
import { Lock, Clock, Plus, AlertCircle, User, Phone, Ban, X } from 'lucide-react';
import { parseDataHoraLocal, extrairDataIso, getAgoraBrasilia } from '../../utils/dateUtils';

export interface AdminDailyTimelineGridProps {
  dataSelecionada: string;
  minhasQuadras: Quadra[];
  agendamentosAdmin: Agendamento[];
  mapaBloqueiosPorQuadra: Record<number, BloqueioHorario[]>;
  quadraFiltroId: number | 'TODAS';
  statusFiltro: 'TODOS' | 'CONFIRMADOS' | 'REALIZADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES';
  buscaTermo: string;
  onAbrirAgendamento: (ag: Agendamento) => void;
  onBloquearSlot: (quadraId: number, data: string, horaInicio: string, horaFim: string) => void;
  onDesbloquear: (quadraId: number, data: string, horaInicio: string, horaFim: string, bloqueio: BloqueioHorario) => void;
  onQuadraFiltroChange?: (id: number | 'TODAS') => void;
}

const HORARIOS_DIA = Array.from({ length: 18 }, (_, i) => {
  const h = i + 6; // 06 às 23
  return `${String(h).padStart(2, '0')}:00`;
});

export const AdminDailyTimelineGrid: React.FC<AdminDailyTimelineGridProps> = ({
  dataSelecionada,
  minhasQuadras,
  agendamentosAdmin,
  mapaBloqueiosPorQuadra,
  quadraFiltroId,
  statusFiltro,
  buscaTermo,
  onAbrirAgendamento,
  onBloquearSlot,
  onDesbloquear,
  onQuadraFiltroChange,
}) => {
  const [brasiliaTime, setBrasiliaTime] = React.useState(() => getAgoraBrasilia());
  const containerRef = React.useRef<HTMLDivElement>(null);
  const tableRef = React.useRef<HTMLTableElement>(null);
  const [lineTop, setLineTop] = React.useState<number | null>(null);
  const [tableWidth, setTableWidth] = React.useState<number>(0);
  const [scrollLeft, setScrollLeft] = React.useState<number>(0);

  React.useEffect(() => {
    const timer = setInterval(() => {
      setBrasiliaTime(getAgoraBrasilia());
    }, 15000);
    return () => clearInterval(timer);
  }, []);

  const { hojeIso, horaMinutoAtual, agora } = brasiliaTime;
  const isHoje = dataSelecionada === hojeIso;
  const horaInteira = Math.floor(horaMinutoAtual);
  const showCurrentTimeLine = isHoje && horaInteira >= 6 && horaInteira <= 23;

  const quadrasExibidas =
    quadraFiltroId === 'TODAS'
      ? minhasQuadras
      : minhasQuadras.filter((q) => q.id_quadra === quadraFiltroId);

  const calcularPosicaoLinha = React.useCallback(() => {
    if (!containerRef.current || !showCurrentTimeLine) {
      setLineTop(null);
      return;
    }

    const minutoFracao = horaMinutoAtual - horaInteira;
    const rowEl = document.getElementById(`timeline-row-${horaInteira}`);

    if (rowEl && containerRef.current) {
      const containerRect = containerRef.current.getBoundingClientRect();
      const rowRect = rowEl.getBoundingClientRect();
      const topOffset =
        rowRect.top -
        containerRect.top +
        containerRef.current.scrollTop +
        minutoFracao * rowRect.height;
      setLineTop(topOffset);

      if (tableRef.current) {
        setTableWidth(tableRef.current.scrollWidth);
      }
    }
  }, [showCurrentTimeLine, horaMinutoAtual, horaInteira]);

  React.useLayoutEffect(() => {
    calcularPosicaoLinha();
  }, [calcularPosicaoLinha, agendamentosAdmin, quadrasExibidas.length]);

  React.useEffect(() => {
    if (!containerRef.current) return;
    const ro = new ResizeObserver(() => {
      calcularPosicaoLinha();
    });
    ro.observe(containerRef.current);
    if (tableRef.current) {
      ro.observe(tableRef.current);
    }
    return () => ro.disconnect();
  }, [calcularPosicaoLinha]);

  React.useEffect(() => {
    window.addEventListener('resize', calcularPosicaoLinha);
    return () => window.removeEventListener('resize', calcularPosicaoLinha);
  }, [calcularPosicaoLinha]);

  const encontrarAgendamentoNoSlot = (quadraId: number, horaStr: string) => {
    const horaNum = parseInt(horaStr.split(':')[0], 10);
    return agendamentosAdmin.find((ag) => {
      if (ag.quadraId !== quadraId) return false;
      if (ag.status === 'CANCELADO') return false;

      const agData = extrairDataIso(ag.dataHoraInicio);
      if (agData !== dataSelecionada) return false;

      const horaInicioStr = ag.dataHoraInicio.includes('T') ? ag.dataHoraInicio.split('T')[1] : '';
      const horaFimStr = ag.dataHoraFim.includes('T') ? ag.dataHoraFim.split('T')[1] : '';
      const hIni = parseInt(horaInicioStr.split(':')[0], 10);
      const mFim = parseInt(horaFimStr.split(':')[1] || '0', 10);
      const hFimBase = parseInt(horaFimStr.split(':')[0], 10);
      const hFim = mFim > 0 ? hFimBase + 1 : hFimBase;
      return horaNum >= hIni && horaNum < hFim;
    });
  };

  const encontrarBloqueioNoSlot = (quadraId: number, horaStr: string) => {
    const bloqueios = mapaBloqueiosPorQuadra[quadraId] || [];
    const horaNum = parseInt(horaStr.split(':')[0], 10);

    return bloqueios.find((b) => {
      if (b.data !== dataSelecionada) return false;
      if (!b.horaInicio || !b.horaFim) return true;

      const bIni = parseInt(b.horaInicio.slice(0, 2), 10);
      let bFim = parseInt(b.horaFim.slice(0, 2), 10);
      const bFimMin = parseInt(b.horaFim.slice(3, 5) || '0', 10);
      const bFimSec = parseInt(b.horaFim.slice(6, 8) || '0', 10);

      // Se o bloqueio termina às 23:59, 23:59:59 ou 00:00, cobre até a meia-noite (hora 24), cobrindo o slot das 23h
      if (bFim === 0 || (bFim === 23 && (bFimMin > 0 || bFimSec > 0))) {
        bFim = 24;
      }

      return horaNum >= bIni && horaNum < bFim;
    });
  };



  if (quadrasExibidas.length === 0) {
    return (
      <div className="bg-surface-2 border border-fg/[0.1] rounded-2xl sm:rounded-3xl p-12 text-center space-y-3 shadow-[inset_0_1px_0_0_rgba(255,255,255,0.06)]">
        <AlertCircle className="w-10 h-10 text-fg/60 mx-auto" />
        <h3 className="text-base font-semibold text-fg tracking-tight">Nenhuma quadra selecionada</h3>
        <p className="text-sm text-fg/60">Selecione outra opção no filtro acima.</p>
        {quadraFiltroId !== 'TODAS' && onQuadraFiltroChange && (
          <button
            type="button"
            onClick={() => onQuadraFiltroChange('TODAS')}
            className="px-4 py-2 bg-fg text-on-accent hover:bg-fg/90 rounded-xl text-xs font-semibold transition active:scale-95 cursor-pointer font-mono"
          >
            Ver todas as quadras
          </button>
        )}
      </div>
    );
  }

  return (
    <div className="bg-fg/[0.03] border border-fg/[0.06] rounded-2xl overflow-hidden flex flex-col">
      {/* Header da Grade */}
      <div className="p-4 sm:p-5 border-b border-fg/[0.06] flex flex-wrap items-center justify-between gap-3 bg-transparent">
        <div className="flex items-center gap-2.5">
          <Clock className="w-4 h-4 text-fg/70" />
          <span className="text-sm font-semibold text-fg tracking-tight">Linha do Tempo das Quadras</span>
          <span className="text-xs font-mono font-medium text-fg/80 bg-fg/[0.06] px-2.5 py-0.5 rounded-full border border-fg/[0.1]">
            {dataSelecionada.split('-').reverse().join('/')}
          </span>
          <span className="text-xs text-fg/60 font-medium">
            ({quadrasExibidas.length} {quadrasExibidas.length === 1 ? 'quadra' : 'quadras'})
          </span>
          {quadraFiltroId !== 'TODAS' && onQuadraFiltroChange && (
            <button
              type="button"
              onClick={() => onQuadraFiltroChange('TODAS')}
              className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-info bg-info/10 hover:bg-info/20 border border-info/30 rounded-xl transition cursor-pointer active:scale-95 ml-1 font-mono"
              title="Remover filtro e ver todas as quadras"
            >
              <X className="w-3.5 h-3.5" />
              <span>Ver todas ({minhasQuadras.length})</span>
            </button>
          )}
        </div>

        {/* Legenda dos Status */}
        <div className="flex flex-wrap items-center gap-3 text-xs text-fg/60 font-medium">
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-success" />
            <span>Livre</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-info" />
            <span>Confirmado</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-purple" />
            <span>Realizado</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-warning" />
            <span>Pendente Pix</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-danger" />
            <span>Bloqueado</span>
          </div>
        </div>
      </div>

      {/* Tabela de Horários */}
      <div
        ref={containerRef}
        onScroll={(e) => setScrollLeft(e.currentTarget.scrollLeft)}
        className="overflow-x-auto relative scrollbar-thin"
      >
        {/* Linha Indicadora de Horário Atual (Fuso de Brasília) */}
        {showCurrentTimeLine && (
          <div
            className="pointer-events-none absolute left-0 z-30 flex items-center"
            style={{
              top: `${lineTop !== null ? lineTop : 45 + (horaMinutoAtual - 6) * 55}px`,
              width: tableWidth > 0 ? `${tableWidth}px` : '100%',
              minWidth: '100%',
            }}
          >
            <div className="w-full border-t-2 border-danger relative">
              <span
                className="absolute -top-2.5 bg-danger text-white text-xs font-bold px-2 py-0.5 rounded-full shadow-md font-mono"
                style={{ left: `${scrollLeft + 8}px` }}
              >
                AGORA
              </span>
            </div>
          </div>
        )}

        <table ref={tableRef} className="w-full text-left border-collapse min-w-[720px]">
          <thead>
            <tr className="border-b border-fg/[0.1] bg-surface-1 text-xs font-medium text-fg/60">
              <th className="p-3.5 w-24 text-center sticky left-0 bg-surface-1 backdrop-blur z-20 border-r border-fg/[0.1] font-mono">
                Horário
              </th>
              {quadrasExibidas.map((quadra) => {
                const isFiltered = quadraFiltroId === quadra.id_quadra;
                return (
                  <th
                    key={quadra.id_quadra}
                    onClick={() => {
                      if (!isFiltered && onQuadraFiltroChange) {
                        onQuadraFiltroChange(quadra.id_quadra);
                      }
                    }}
                    className={`p-3.5 font-semibold text-fg border-r border-fg/[0.1] min-w-[190px] transition-colors ${
                      !isFiltered && onQuadraFiltroChange
                        ? 'cursor-pointer hover:bg-fg/[0.03] group/th'
                        : 'bg-info/[0.04]'
                    }`}
                    title={
                      !isFiltered && onQuadraFiltroChange
                        ? `Filtrar apenas por ${quadra.nome}`
                        : undefined
                    }
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-1.5 min-w-0">
                        <span
                          className={`truncate tracking-tight transition-colors ${
                            !isFiltered ? 'group-hover/th:text-info' : 'text-info'
                          }`}
                        >
                          {quadra.nome}
                        </span>
                      </div>

                      <div className="flex items-center gap-1.5 shrink-0">
                        <span className="text-xs text-fg/60 font-mono tracking-wider uppercase">
                          {quadra.tipoEsporte.replace('_', ' ')}
                        </span>

                        {isFiltered && onQuadraFiltroChange && (
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              onQuadraFiltroChange('TODAS');
                            }}
                            className="flex items-center gap-1 px-2 py-0.5 text-xs font-medium text-fg/90 hover:text-fg bg-fg/[0.1] hover:bg-fg/[0.2] border border-fg/[0.15] rounded-lg transition active:scale-95 cursor-pointer font-mono"
                            title="Remover filtro de quadra"
                          >
                            <X className="w-3 h-3 text-fg/70" />
                            <span>Remover</span>
                          </button>
                        )}
                      </div>
                    </div>
                  </th>
                );
              })}
            </tr>
          </thead>
          <tbody className="divide-y divide-fg/[0.06] text-sm">
            {HORARIOS_DIA.map((horaStr) => {
              const horaNum = parseInt(horaStr.split(':')[0], 10);
              const proximaHoraStr = `${String((horaNum + 1) % 24).padStart(2, '0')}:00`;

              return (
                <tr
                  key={horaStr}
                  id={`timeline-row-${horaNum}`}
                  className="hover:bg-fg/[0.03] transition-colors"
                >
                  {/* Coluna de Horário Fixa */}
                  <td className="p-3 text-center font-mono text-xs font-semibold text-fg/60 bg-surface-2 sticky left-0 z-10 border-r border-fg/[0.1] select-none">
                    {horaStr}
                  </td>

                  {/* Colunas das Quadras */}
                  {quadrasExibidas.map((quadra) => {
                    const agendamento = encontrarAgendamentoNoSlot(quadra.id_quadra, horaStr);
                    const bloqueio = agendamento ? null : encontrarBloqueioNoSlot(quadra.id_quadra, horaStr);
                    const isLivre = !agendamento && !bloqueio;

                    const isAgPassado = agendamento
                      ? parseDataHoraLocal(agendamento.dataHoraFim) < agora
                      : false;

                    // Filtros de status
                    if (
                      statusFiltro === 'CONFIRMADOS' &&
                      (!agendamento || agendamento.status !== 'CONFIRMADO' || isAgPassado)
                    ) {
                      return (
                        <td
                          key={quadra.id_quadra}
                          className="p-1.5 border-r border-fg/[0.06] bg-bg/40 opacity-20"
                        />
                      );
                    }
                    if (
                      statusFiltro === 'REALIZADOS' &&
                      (!agendamento || !isAgPassado || agendamento.status === 'CANCELADO')
                    ) {
                      return (
                        <td
                          key={quadra.id_quadra}
                          className="p-1.5 border-r border-fg/[0.06] bg-bg/40 opacity-20"
                        />
                      );
                    }
                    if (
                      statusFiltro === 'PENDENTES' &&
                      (!agendamento || agendamento.status !== 'PENDENTE')
                    ) {
                      return (
                        <td
                          key={quadra.id_quadra}
                          className="p-1.5 border-r border-fg/[0.06] bg-bg/40 opacity-20"
                        />
                      );
                    }
                    if (statusFiltro === 'BLOQUEADOS' && !bloqueio) {
                      return (
                        <td
                          key={quadra.id_quadra}
                          className="p-1.5 border-r border-fg/[0.06] bg-bg/40 opacity-20"
                        />
                      );
                    }
                    if (statusFiltro === 'LIVRES' && !isLivre) {
                      return (
                        <td
                          key={quadra.id_quadra}
                          className="p-1.5 border-r border-fg/[0.06] bg-bg/40 opacity-20"
                        />
                      );
                    }

                    const atendeBusca =
                      buscaTermo.trim() !== '' &&
                      agendamento &&
                      (agendamento.nomeUsuario.toLowerCase().includes(buscaTermo.toLowerCase()) ||
                        (agendamento.telefoneUsuario &&
                          agendamento.telefoneUsuario.includes(buscaTermo)));

                    return (
                      <td
                        key={quadra.id_quadra}
                        className="p-1.5 sm:p-2 border-r border-fg/[0.06] align-top relative group min-h-[58px]"
                      >
                        {agendamento ? (
                          <div
                            onClick={() => onAbrirAgendamento(agendamento)}
                            className={`rounded-2xl p-2.5 cursor-pointer transition-all duration-150 flex flex-col justify-between shadow-sm relative overflow-hidden group/card ${
                              isAgPassado
                                ? 'bg-purple/10 hover:bg-purple/15 border border-purple/25 text-fg'
                                : agendamento.status === 'CONFIRMADO'
                                ? 'bg-info/10 hover:bg-info/15 border border-info/25 text-fg'
                                : 'bg-warning/10 hover:bg-warning/15 border border-warning/25 text-fg'
                            } ${
                              atendeBusca ? 'ring-2 ring-fg shadow-lg scale-[1.02] z-10' : ''
                            }`}
                          >
                            <div className="flex items-center justify-between gap-1">
                              <div className="flex items-center gap-1.5 min-w-0">
                                <User className="w-3.5 h-3.5 shrink-0 text-fg/60" />
                                <span className="text-xs font-semibold truncate text-fg tracking-tight">
                                  {agendamento.nomeUsuario}
                                </span>
                              </div>
                              <span
                                className={`text-xs font-mono font-medium px-1.5 py-0.5 rounded-full uppercase tracking-wider shrink-0 ${
                                  isAgPassado
                                    ? 'bg-purple/20 text-purple'
                                    : agendamento.status === 'CONFIRMADO'
                                    ? 'bg-info/20 text-info'
                                    : 'bg-warning/20 text-warning'
                                }`}
                              >
                                {isAgPassado
                                  ? 'Realizado'
                                  : agendamento.status === 'CONFIRMADO'
                                  ? 'Confirmado'
                                  : 'Pix Pend.'}
                              </span>
                            </div>

                            {agendamento.telefoneUsuario && (
                              <div className="flex items-center gap-1 text-xs text-fg/60 mt-1">
                                <Phone className="w-3 h-3 text-fg/60 shrink-0" />
                                <span className="truncate">{agendamento.telefoneUsuario}</span>
                              </div>
                            )}

                            <div className="flex items-center justify-between mt-1 text-xs text-fg/60 font-mono">
                              <span className="font-semibold text-fg">
                                R$ {agendamento.valorTotal.toFixed(2)}
                              </span>
                              <span className="text-fg/60 group-hover/card:text-fg transition font-sans text-xs">
                                Detalhes →
                              </span>
                            </div>
                          </div>
                        ) : bloqueio ? (
                          /* CARD DE BLOQUEIO */
                          <div className="rounded-2xl p-2.5 bg-danger/10 border border-danger/25 text-fg flex flex-col justify-between group/block transition">
                            <div className="flex items-center justify-between gap-1">
                              <div className="flex items-center gap-1.5 min-w-0">
                                <Lock className="w-3.5 h-3.5 text-danger shrink-0" />
                                <span className="text-xs font-semibold text-danger truncate">
                                  {bloqueio.motivo || 'Bloqueado'}
                                </span>
                              </div>
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  onDesbloquear(quadra.id_quadra, dataSelecionada, horaStr, proximaHoraStr, bloqueio);
                                }}
                                title={`Desbloquear horário das ${horaStr} às ${proximaHoraStr}`}
                                className="opacity-0 group-hover/block:opacity-100 p-1 hover:bg-danger/20 text-danger hover:text-fg rounded-lg transition cursor-pointer"
                              >
                                <Ban className="w-3.5 h-3.5" />
                              </button>
                            </div>
                            <span className="text-xs text-fg/60 mt-1 font-mono">
                              {bloqueio.horaInicio
                                ? `${bloqueio.horaInicio.slice(0, 5)} - ${bloqueio.horaFim?.slice(0, 2) === '23' && bloqueio.horaFim?.slice(3, 5) === '59' ? '00:00' : bloqueio.horaFim?.slice(0, 5)}`
                                : 'Dia todo'}
                            </span>
                          </div>
                        ) : (
                          /* SLOT LIVRE */
                          <div className="h-full min-h-[48px] rounded-2xl border border-dashed border-fg/[0.1] hover:border-fg/[0.2] hover:bg-fg/[0.03] transition-all flex items-center justify-center group/slot">
                            <button
                              onClick={() =>
                                onBloquearSlot(quadra.id_quadra, dataSelecionada, horaStr, proximaHoraStr)
                              }
                              className="opacity-0 group-hover/slot:opacity-100 inline-flex items-center gap-1 text-xs font-medium text-fg/80 hover:text-fg bg-fg/[0.1] hover:bg-fg/[0.15] px-2.5 py-1 rounded-xl border border-fg/[0.1] shadow-sm transition transform scale-95 group-hover/slot:scale-100 cursor-pointer tracking-tight"
                            >
                              <Plus className="w-3 h-3" />
                              <span>Bloquear</span>
                            </button>
                          </div>
                        )}
                      </td>
                    );
                  })}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};
