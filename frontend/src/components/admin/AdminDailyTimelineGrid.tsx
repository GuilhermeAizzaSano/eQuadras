import React from 'react';
import { Quadra, Agendamento, BloqueioHorario } from '../../types';
import { Lock, Clock, Plus, AlertCircle, User, Phone, Ban } from 'lucide-react';
import { parseDataHoraLocal, extrairDataIso, getHojeLocalIso } from '../../utils/dateUtils';

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
  onDesbloquear: (bloqueioId: number) => void;
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
}) => {
  const agora = new Date();
  const hojeIso = getHojeLocalIso();
  const isHoje = dataSelecionada === hojeIso;
  const horaMinutoAtual = agora.getHours() + agora.getMinutes() / 60;

  const quadrasExibidas =
    quadraFiltroId === 'TODAS'
      ? minhasQuadras
      : minhasQuadras.filter((q) => q.id_quadra === quadraFiltroId);

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
      const bFim = parseInt(b.horaFim.slice(0, 2), 10);
      return horaNum >= bIni && horaNum < bFim;
    });
  };

  const showCurrentTimeLine = isHoje && horaMinutoAtual >= 6 && horaMinutoAtual <= 24;
  const currentTimePercentage = ((horaMinutoAtual - 6) / 18) * 100;

  if (quadrasExibidas.length === 0) {
    return (
      <div className="bg-surface-900 border border-surface-800 rounded-3xl p-12 text-center space-y-3 shadow-xl">
        <AlertCircle className="w-10 h-10 text-surface-500 mx-auto" />
        <h3 className="text-base font-bold text-white tracking-tight">Nenhuma quadra selecionada</h3>
        <p className="text-sm text-surface-400">Selecione outra opção no filtro acima.</p>
      </div>
    );
  }

  return (
    <div className="bg-surface-900 border border-surface-800 rounded-3xl shadow-2xl shadow-black/40 overflow-hidden flex flex-col">
      {/* Header da Grade */}
      <div className="p-4 sm:p-5 border-b border-surface-800 flex flex-wrap items-center justify-between gap-3 bg-surface-950/70">
        <div className="flex items-center gap-2.5">
          <Clock className="w-4 h-4 text-brand-400" />
          <span className="text-sm font-extrabold text-white tracking-tight">Linha do Tempo das Quadras</span>
          <span className="text-xs font-mono font-bold text-brand-400 bg-brand-950/60 px-2.5 py-0.5 rounded-lg border border-brand-500/30">
            {dataSelecionada.split('-').reverse().join('/')}
          </span>
          <span className="text-xs text-surface-400 font-medium">
            ({quadrasExibidas.length} {quadrasExibidas.length === 1 ? 'quadra' : 'quadras'})
          </span>
        </div>

        {/* Legenda dos Status */}
        <div className="flex flex-wrap items-center gap-3 text-xs text-surface-300 font-medium">
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 shadow-[0_0_6px_#34d399]" />
            <span>Livre</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-blue-400 shadow-[0_0_6px_#60a5fa]" />
            <span>Confirmado</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-purple-400" />
            <span>Realizado</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-amber-400 shadow-[0_0_6px_#fbbf24]" />
            <span>Pendente Pix</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-rose-400" />
            <span>Bloqueado</span>
          </div>
        </div>
      </div>

      {/* Tabela de Horários */}
      <div className="overflow-x-auto relative scrollbar-thin">
        <table className="w-full text-left border-collapse min-w-[720px]">
          <thead>
            <tr className="border-b border-surface-800 bg-surface-950/90 text-xs font-semibold text-surface-400">
              <th className="p-3.5 w-24 text-center sticky left-0 bg-surface-950/95 backdrop-blur z-20 border-r border-surface-800 font-mono">
                Horário
              </th>
              {quadrasExibidas.map((quadra) => (
                <th
                  key={quadra.id_quadra}
                  className="p-3.5 font-bold text-white border-r border-surface-800/80 min-w-[190px]"
                >
                  <div className="flex items-center justify-between">
                    <span className="truncate">{quadra.nome}</span>
                    <span className="text-[10px] text-surface-400 font-mono bg-surface-850 px-2 py-0.5 rounded-md ml-1 border border-surface-750">
                      {quadra.tipoEsporte.replace('_', ' ')}
                    </span>
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-surface-800/60 text-sm relative">
            {/* Linha Indicadora de Horário Atual */}
            {showCurrentTimeLine && (
              <tr
                className="pointer-events-none absolute w-full left-0 z-20 flex"
                style={{ top: `${currentTimePercentage}%` }}
              >
                <td colSpan={quadrasExibidas.length + 1} className="w-full p-0 relative">
                  <div className="w-full border-t-2 border-brand-400 shadow-[0_0_10px_#34d399] relative">
                    <span className="absolute -top-2.5 left-2 bg-brand-500 text-surface-950 text-[9px] font-extrabold px-2 py-0.5 rounded-full shadow-lg">
                      AGORA
                    </span>
                  </div>
                </td>
              </tr>
            )}

            {HORARIOS_DIA.map((horaStr) => {
              const horaNum = parseInt(horaStr.split(':')[0], 10);
              const proximaHoraStr = `${String((horaNum + 1) % 24).padStart(2, '0')}:00`;

              return (
                <tr key={horaStr} className="hover:bg-surface-850/20 transition-colors">
                  {/* Coluna de Horário Fixa */}
                  <td className="p-3 text-center font-mono text-xs font-bold text-surface-400 bg-surface-950/90 sticky left-0 z-10 border-r border-surface-800 select-none">
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
                          className="p-1.5 border-r border-surface-800/60 bg-surface-950/40 opacity-20"
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
                          className="p-1.5 border-r border-surface-800/60 bg-surface-950/40 opacity-20"
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
                          className="p-1.5 border-r border-surface-800/60 bg-surface-950/40 opacity-20"
                        />
                      );
                    }
                    if (statusFiltro === 'BLOQUEADOS' && !bloqueio) {
                      return (
                        <td
                          key={quadra.id_quadra}
                          className="p-1.5 border-r border-surface-800/60 bg-surface-950/40 opacity-20"
                        />
                      );
                    }
                    if (statusFiltro === 'LIVRES' && !isLivre) {
                      return (
                        <td
                          key={quadra.id_quadra}
                          className="p-1.5 border-r border-surface-800/60 bg-surface-950/40 opacity-20"
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
                        className="p-1.5 sm:p-2 border-r border-surface-800/60 align-top relative group min-h-[58px]"
                      >
                        {agendamento ? (
                          <div
                            onClick={() => onAbrirAgendamento(agendamento)}
                            className={`rounded-2xl p-2.5 cursor-pointer transition-all duration-150 flex flex-col justify-between shadow-sm relative overflow-hidden group/card ${
                              isAgPassado
                                ? 'bg-purple-950/50 hover:bg-purple-900/60 border border-purple-800/80 text-purple-100'
                                : agendamento.status === 'CONFIRMADO'
                                ? 'bg-blue-950/60 hover:bg-blue-900/70 border border-blue-800/80 text-blue-100'
                                : 'bg-amber-950/60 hover:bg-amber-900/70 border border-amber-800/80 text-amber-100'
                            } ${
                              atendeBusca ? 'ring-2 ring-white shadow-xl scale-[1.02] z-10' : ''
                            }`}
                          >
                            <div className="flex items-center justify-between gap-1">
                              <div className="flex items-center gap-1.5 min-w-0">
                                <User className="w-3.5 h-3.5 shrink-0 text-surface-300" />
                                <span className="text-xs font-bold truncate text-white">
                                  {agendamento.nomeUsuario}
                                </span>
                              </div>
                              <span
                                className={`text-[9px] font-mono font-bold px-1.5 py-0.5 rounded uppercase tracking-wider shrink-0 ${
                                  isAgPassado
                                    ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                                    : agendamento.status === 'CONFIRMADO'
                                    ? 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                                    : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
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
                              <div className="flex items-center gap-1 text-[11px] text-surface-400 mt-1">
                                <Phone className="w-3 h-3 text-surface-500 shrink-0" />
                                <span className="truncate">{agendamento.telefoneUsuario}</span>
                              </div>
                            )}

                            <div className="flex items-center justify-between mt-1 text-[10px] text-surface-400 font-mono">
                              <span className="font-semibold text-white">
                                R$ {agendamento.valorTotal.toFixed(2)}
                              </span>
                              <span className="text-surface-400 group-hover/card:text-white transition font-sans">
                                Detalhes →
                              </span>
                            </div>
                          </div>
                        ) : bloqueio ? (
                          /* CARD DE BLOQUEIO */
                          <div className="rounded-2xl p-2.5 bg-rose-950/40 border border-rose-900/60 text-rose-200 flex flex-col justify-between group/block transition">
                            <div className="flex items-center justify-between gap-1">
                              <div className="flex items-center gap-1.5 min-w-0">
                                <Lock className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                                <span className="text-xs font-semibold text-rose-300 truncate">
                                  {bloqueio.motivo || 'Bloqueado'}
                                </span>
                              </div>
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  onDesbloquear(bloqueio.id);
                                }}
                                title="Desbloquear este horário"
                                className="opacity-0 group-hover/block:opacity-100 p-1 hover:bg-rose-900/40 text-rose-300 hover:text-white rounded-lg transition cursor-pointer"
                              >
                                <Ban className="w-3.5 h-3.5" />
                              </button>
                            </div>
                            <span className="text-[10px] text-rose-400/80 mt-1 font-mono">
                              {bloqueio.horaInicio
                                ? `${bloqueio.horaInicio.slice(0, 5)} - ${bloqueio.horaFim?.slice(0, 5)}`
                                : 'Dia todo'}
                            </span>
                          </div>
                        ) : (
                          /* SLOT LIVRE */
                          <div className="h-full min-h-[48px] rounded-2xl border border-dashed border-surface-800 hover:border-brand-500/50 hover:bg-brand-950/20 transition-all flex items-center justify-center group/slot">
                            <button
                              onClick={() =>
                                onBloquearSlot(quadra.id_quadra, dataSelecionada, horaStr, proximaHoraStr)
                              }
                              className="opacity-0 group-hover/slot:opacity-100 inline-flex items-center gap-1 text-[11px] font-semibold text-brand-400 hover:text-brand-300 bg-brand-950/80 px-2.5 py-1 rounded-xl border border-brand-500/30 shadow transition transform scale-95 group-hover/slot:scale-100 cursor-pointer"
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
