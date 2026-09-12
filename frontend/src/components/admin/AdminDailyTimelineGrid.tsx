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
      <div className="bg-[#121214] border border-white/[0.08] rounded-2xl sm:rounded-3xl p-12 text-center space-y-3 shadow-[inset_0_1px_0_0_rgba(255,255,255,0.06)]">
        <AlertCircle className="w-10 h-10 text-white/30 mx-auto" />
        <h3 className="text-base font-semibold text-white tracking-tight">Nenhuma quadra selecionada</h3>
        <p className="text-sm text-white/50">Selecione outra opção no filtro acima.</p>
      </div>
    );
  }

  return (
    <div className="bg-[#121214] border border-white/[0.08] rounded-3xl shadow-apple-card shadow-[inset_0_1px_0_0_rgba(255,255,255,0.05)] overflow-hidden flex flex-col">
      {/* Header da Grade */}
      <div className="p-4 sm:p-5 border-b border-white/[0.08] flex flex-wrap items-center justify-between gap-3 bg-[#121214]">
        <div className="flex items-center gap-2.5">
          <Clock className="w-4 h-4 text-white/70" />
          <span className="text-sm font-semibold text-white tracking-tight">Linha do Tempo das Quadras</span>
          <span className="text-xs font-mono font-medium text-white/80 bg-white/[0.06] px-2.5 py-0.5 rounded-full border border-white/[0.1]">
            {dataSelecionada.split('-').reverse().join('/')}
          </span>
          <span className="text-xs text-white/40 font-medium">
            ({quadrasExibidas.length} {quadrasExibidas.length === 1 ? 'quadra' : 'quadras'})
          </span>
        </div>

        {/* Legenda dos Status */}
        <div className="flex flex-wrap items-center gap-3 text-xs text-white/60 font-medium">
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-[#30D158]" />
            <span>Livre</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-[#0A84FF]" />
            <span>Confirmado</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-[#BF5AF2]" />
            <span>Realizado</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-[#FF9F0A]" />
            <span>Pendente Pix</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-[#FF453A]" />
            <span>Bloqueado</span>
          </div>
        </div>
      </div>

      {/* Tabela de Horários */}
      <div className="overflow-x-auto relative scrollbar-thin">
        <table className="w-full text-left border-collapse min-w-[720px]">
          <thead>
            <tr className="border-b border-white/[0.08] bg-[#0c0c0e] text-xs font-medium text-white/50">
              <th className="p-3.5 w-24 text-center sticky left-0 bg-[#0c0c0e] backdrop-blur z-20 border-r border-white/[0.08] font-mono">
                Horário
              </th>
              {quadrasExibidas.map((quadra) => (
                <th
                  key={quadra.id_quadra}
                  className="p-3.5 font-semibold text-white border-r border-white/[0.08] min-w-[190px]"
                >
                  <div className="flex items-center justify-between">
                    <span className="truncate tracking-tight">{quadra.nome}</span>
                    <span className="text-[10px] text-white/50 font-mono bg-white/[0.05] px-2 py-0.5 rounded-full ml-1 border border-white/[0.08]">
                      {quadra.tipoEsporte.replace('_', ' ')}
                    </span>
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-white/[0.06] text-sm relative">
            {/* Linha Indicadora de Horário Atual */}
            {showCurrentTimeLine && (
              <tr
                className="pointer-events-none absolute w-full left-0 z-20 flex"
                style={{ top: `${currentTimePercentage}%` }}
              >
                <td colSpan={quadrasExibidas.length + 1} className="w-full p-0 relative">
                  <div className="w-full border-t-2 border-[#FF453A] relative">
                    <span className="absolute -top-2.5 left-2 bg-[#FF453A] text-white text-[9px] font-semibold px-2 py-0.5 rounded-full shadow-sm">
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
                <tr key={horaStr} className="hover:bg-white/[0.02] transition-colors">
                  {/* Coluna de Horário Fixa */}
                  <td className="p-3 text-center font-mono text-xs font-semibold text-white/50 bg-[#121214] sticky left-0 z-10 border-r border-white/[0.08] select-none">
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
                          className="p-1.5 border-r border-white/[0.06] bg-black/40 opacity-20"
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
                          className="p-1.5 border-r border-white/[0.06] bg-black/40 opacity-20"
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
                          className="p-1.5 border-r border-white/[0.06] bg-black/40 opacity-20"
                        />
                      );
                    }
                    if (statusFiltro === 'BLOQUEADOS' && !bloqueio) {
                      return (
                        <td
                          key={quadra.id_quadra}
                          className="p-1.5 border-r border-white/[0.06] bg-black/40 opacity-20"
                        />
                      );
                    }
                    if (statusFiltro === 'LIVRES' && !isLivre) {
                      return (
                        <td
                          key={quadra.id_quadra}
                          className="p-1.5 border-r border-white/[0.06] bg-black/40 opacity-20"
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
                        className="p-1.5 sm:p-2 border-r border-white/[0.06] align-top relative group min-h-[58px]"
                      >
                        {agendamento ? (
                          <div
                            onClick={() => onAbrirAgendamento(agendamento)}
                            className={`rounded-2xl p-2.5 cursor-pointer transition-all duration-150 flex flex-col justify-between shadow-sm relative overflow-hidden group/card ${
                              isAgPassado
                                ? 'bg-[#BF5AF2]/10 hover:bg-[#BF5AF2]/15 border border-[#BF5AF2]/25 text-white'
                                : agendamento.status === 'CONFIRMADO'
                                ? 'bg-[#0A84FF]/10 hover:bg-[#0A84FF]/15 border border-[#0A84FF]/25 text-white'
                                : 'bg-[#FF9F0A]/10 hover:bg-[#FF9F0A]/15 border border-[#FF9F0A]/25 text-white'
                            } ${
                              atendeBusca ? 'ring-2 ring-white shadow-lg scale-[1.02] z-10' : ''
                            }`}
                          >
                            <div className="flex items-center justify-between gap-1">
                              <div className="flex items-center gap-1.5 min-w-0">
                                <User className="w-3.5 h-3.5 shrink-0 text-white/50" />
                                <span className="text-xs font-semibold truncate text-white tracking-tight">
                                  {agendamento.nomeUsuario}
                                </span>
                              </div>
                              <span
                                className={`text-[9px] font-mono font-medium px-1.5 py-0.5 rounded-full uppercase tracking-wider shrink-0 ${
                                  isAgPassado
                                    ? 'bg-[#BF5AF2]/20 text-[#BF5AF2]'
                                    : agendamento.status === 'CONFIRMADO'
                                    ? 'bg-[#0A84FF]/20 text-[#0A84FF]'
                                    : 'bg-[#FF9F0A]/20 text-[#FF9F0A]'
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
                              <div className="flex items-center gap-1 text-[11px] text-white/40 mt-1">
                                <Phone className="w-3 h-3 text-white/40 shrink-0" />
                                <span className="truncate">{agendamento.telefoneUsuario}</span>
                              </div>
                            )}

                            <div className="flex items-center justify-between mt-1 text-[10px] text-white/50 font-mono">
                              <span className="font-semibold text-white">
                                R$ {agendamento.valorTotal.toFixed(2)}
                              </span>
                              <span className="text-white/40 group-hover/card:text-white transition font-sans text-[11px]">
                                Detalhes →
                              </span>
                            </div>
                          </div>
                        ) : bloqueio ? (
                          /* CARD DE BLOQUEIO */
                          <div className="rounded-2xl p-2.5 bg-[#FF453A]/10 border border-[#FF453A]/25 text-white flex flex-col justify-between group/block transition">
                            <div className="flex items-center justify-between gap-1">
                              <div className="flex items-center gap-1.5 min-w-0">
                                <Lock className="w-3.5 h-3.5 text-[#FF453A] shrink-0" />
                                <span className="text-xs font-semibold text-[#FF453A] truncate">
                                  {bloqueio.motivo || 'Bloqueado'}
                                </span>
                              </div>
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  onDesbloquear(bloqueio.id);
                                }}
                                title="Desbloquear este horário"
                                className="opacity-0 group-hover/block:opacity-100 p-1 hover:bg-[#FF453A]/20 text-[#FF453A] hover:text-white rounded-lg transition cursor-pointer"
                              >
                                <Ban className="w-3.5 h-3.5" />
                              </button>
                            </div>
                            <span className="text-[10px] text-white/50 mt-1 font-mono">
                              {bloqueio.horaInicio
                                ? `${bloqueio.horaInicio.slice(0, 5)} - ${bloqueio.horaFim?.slice(0, 5)}`
                                : 'Dia todo'}
                            </span>
                          </div>
                        ) : (
                          /* SLOT LIVRE */
                          <div className="h-full min-h-[48px] rounded-2xl border border-dashed border-white/[0.08] hover:border-white/[0.2] hover:bg-white/[0.03] transition-all flex items-center justify-center group/slot">
                            <button
                              onClick={() =>
                                onBloquearSlot(quadra.id_quadra, dataSelecionada, horaStr, proximaHoraStr)
                              }
                              className="opacity-0 group-hover/slot:opacity-100 inline-flex items-center gap-1 text-[11px] font-medium text-white/80 hover:text-white bg-white/[0.08] hover:bg-white/[0.14] px-2.5 py-1 rounded-xl border border-white/[0.1] shadow-sm transition transform scale-95 group-hover/slot:scale-100 cursor-pointer tracking-tight"
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
