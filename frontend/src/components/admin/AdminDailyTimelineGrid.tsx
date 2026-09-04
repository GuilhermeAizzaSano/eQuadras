import React from 'react';
import { Quadra, Agendamento, BloqueioHorario } from '../../types';
import { Lock, Clock, Plus, AlertCircle, User, Phone, Ban } from 'lucide-react';

export interface AdminDailyTimelineGridProps {
  dataSelecionada: string;
  minhasQuadras: Quadra[];
  agendamentosAdmin: Agendamento[];
  mapaBloqueiosPorQuadra: Record<number, BloqueioHorario[]>;
  quadraFiltroId: number | 'TODAS';
  statusFiltro: 'TODOS' | 'CONFIRMADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES';
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
  const hojeIso = new Date().toISOString().split('T')[0];
  const isHoje = dataSelecionada === hojeIso;

  // Filtrar quadras com base no filtro selecionado
  const quadrasExibidas = React.useMemo(() => {
    if (quadraFiltroId === 'TODAS') {
      return minhasQuadras;
    }
    return minhasQuadras.filter((q) => q.id_quadra === quadraFiltroId);
  }, [minhasQuadras, quadraFiltroId]);

  // Agendamentos da data selecionada
  const agendamentosDoDia = React.useMemo(() => {
    return agendamentosAdmin.filter((ag) => ag.dataHoraInicio.startsWith(dataSelecionada));
  }, [agendamentosAdmin, dataSelecionada]);

  // Indicador de hora atual para calcular barra vermelha/esmeralda
  const [horaMinutoAtual, setHoraMinutoAtual] = React.useState(() => {
    const d = new Date();
    return d.getHours() + d.getMinutes() / 60;
  });

  React.useEffect(() => {
    if (!isHoje) return;
    const interval = setInterval(() => {
      const d = new Date();
      setHoraMinutoAtual(d.getHours() + d.getMinutes() / 60);
    }, 60000);
    return () => clearInterval(interval);
  }, [isHoje]);

  // Função auxiliar para checar se horário h (ex: "14:00") cai no intervalo do agendamento
  const encontrarAgendamentoNoSlot = (quadraId: number, horaStr: string) => {
    const horaNum = parseInt(horaStr.split(':')[0], 10);
    return agendamentosDoDia.find((ag) => {
      if (ag.quadraId !== quadraId) return false;
      if (ag.status === 'CANCELADO') return false;
      const dInicio = new Date(ag.dataHoraInicio);
      const dFim = new Date(ag.dataHoraFim);
      const hIni = dInicio.getHours();
      const hFim = dFim.getMinutes() > 0 ? dFim.getHours() + 1 : dFim.getHours();
      return horaNum >= hIni && horaNum < hFim;
    });
  };

  // Função auxiliar para checar se horário h cai em bloqueio
  const encontrarBloqueioNoSlot = (quadraId: number, horaStr: string) => {
    const bloqueios = mapaBloqueiosPorQuadra[quadraId] || [];
    const horaNum = parseInt(horaStr.split(':')[0], 10);

    return bloqueios.find((b) => {
      if (b.data !== dataSelecionada) return false;
      // Se não tem horaInicio/fim, bloqueia o dia todo
      if (!b.horaInicio || !b.horaFim) return true;

      const bIni = parseInt(b.horaInicio.slice(0, 2), 10);
      const bFim = parseInt(b.horaFim.slice(0, 2), 10);
      return horaNum >= bIni && horaNum < bFim;
    });
  };

  // Calcular posição do indicador de linha de tempo atual
  // A grade vai de 06:00 a 24:00 (18 horas no total)
  const showCurrentTimeLine = isHoje && horaMinutoAtual >= 6 && horaMinutoAtual <= 24;
  const currentTimePercentage = ((horaMinutoAtual - 6) / 18) * 100;

  if (quadrasExibidas.length === 0) {
    return (
      <div className="bg-zinc-900 border border-zinc-800 rounded-2xl p-12 text-center">
        <AlertCircle className="w-10 h-10 text-zinc-500 mx-auto mb-3" />
        <h3 className="text-base font-semibold text-white">Nenhuma quadra selecionada ou disponível</h3>
        <p className="text-sm text-zinc-400 mt-1">Selecione outra opção no filtro acima.</p>
      </div>
    );
  }

  return (
    <div className="bg-zinc-900 border border-zinc-800 rounded-2xl shadow-xl overflow-hidden flex flex-col">
      {/* Título / Legenda rápida */}
      <div className="p-4 border-b border-zinc-800/80 flex flex-wrap items-center justify-between gap-3 bg-zinc-950/40">
        <div className="flex items-center gap-2">
          <Clock className="w-4 h-4 text-emerald-400" />
          <span className="text-sm font-bold text-white">Linha do Tempo das Quadras</span>
          <span className="text-xs text-zinc-500">
            ({quadrasExibidas.length} {quadrasExibidas.length === 1 ? 'quadra' : 'quadras'})
          </span>
        </div>

        <div className="flex flex-wrap items-center gap-3 text-xs text-zinc-400">
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-emerald-500" />
            <span>Confirmado</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-amber-500" />
            <span>Pendente (Pix)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm bg-zinc-700" />
            <span>Bloqueado</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm border border-dashed border-zinc-600 bg-zinc-950" />
            <span>Livre</span>
          </div>
        </div>
      </div>

      {/* Container com rolagem horizontal e vertical */}
      <div className="relative overflow-x-auto max-h-[750px] overflow-y-auto scrollbar-thin">
        {/* Indicador de Horário Atual (Current Time Bar) */}
        {showCurrentTimeLine && (
          <div
            className="absolute left-0 right-0 z-30 pointer-events-none flex items-center"
            style={{
              top: `calc(49px + ${currentTimePercentage}% * 0.94)`, // compensando header sticky
            }}
          >
            <div className="w-16 flex justify-end pr-1">
              <span className="bg-rose-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded shadow">
                Agora
              </span>
            </div>
            <div className="flex-1 h-[2px] bg-rose-500 shadow-sm" />
          </div>
        )}

        <table className="w-full text-left border-collapse min-w-[700px]">
          {/* Cabeçalho da Tabela Sticky */}
          <thead className="sticky top-0 z-20 bg-zinc-950/95 backdrop-blur border-b border-zinc-800 shadow-md">
            <tr>
              <th className="w-20 sm:w-24 p-3 text-center text-xs font-semibold text-zinc-400 border-r border-zinc-800/80 uppercase tracking-wider sticky left-0 z-30 bg-zinc-950">
                Horário
              </th>
              {quadrasExibidas.map((quadra) => (
                <th
                  key={quadra.id_quadra}
                  className="p-3 text-xs font-semibold text-white border-r border-zinc-800/80 min-w-[190px] max-w-[240px]"
                >
                  <div className="flex flex-col">
                    <span className="truncate text-sm font-bold text-zinc-100">{quadra.nome}</span>
                    <div className="flex items-center justify-between text-[11px] text-zinc-400 mt-0.5 font-normal">
                      <span className="capitalize">{quadra.tipoEsporte.toLowerCase().replace('_', ' ')}</span>
                      <span className="text-emerald-400 font-medium">R$ {quadra.valorHora.toFixed(2)}/h</span>
                    </div>
                  </div>
                </th>
              ))}
            </tr>
          </thead>

          <tbody className="divide-y divide-zinc-800/60 bg-zinc-950/20">
            {HORARIOS_DIA.map((horaStr) => {
              const proximaHoraNum = parseInt(horaStr.split(':')[0], 10) + 1;
              const proximaHoraStr = `${String(proximaHoraNum).padStart(2, '0')}:00`;

              return (
                <tr key={horaStr} className="hover:bg-zinc-850/20 transition-colors">
                  {/* Coluna Sticky de Horários */}
                  <td className="p-2 sm:p-3 text-center text-xs font-mono font-medium text-zinc-400 border-r border-zinc-800/80 sticky left-0 z-10 bg-zinc-950/90 whitespace-nowrap">
                    {horaStr}
                  </td>

                  {/* Células de cada quadra */}
                  {quadrasExibidas.map((quadra) => {
                    const agendamento = encontrarAgendamentoNoSlot(quadra.id_quadra, horaStr);
                    const bloqueio = agendamento ? null : encontrarBloqueioNoSlot(quadra.id_quadra, horaStr);
                    const isLivre = !agendamento && !bloqueio;

                    // Aplicar filtros de status
                    if (statusFiltro === 'CONFIRMADOS' && (!agendamento || agendamento.status !== 'CONFIRMADO')) {
                      return <td key={quadra.id_quadra} className="p-1.5 border-r border-zinc-800/60 bg-zinc-950/40 opacity-20" />;
                    }
                    if (statusFiltro === 'PENDENTES' && (!agendamento || agendamento.status !== 'PENDENTE')) {
                      return <td key={quadra.id_quadra} className="p-1.5 border-r border-zinc-800/60 bg-zinc-950/40 opacity-20" />;
                    }
                    if (statusFiltro === 'BLOQUEADOS' && !bloqueio) {
                      return <td key={quadra.id_quadra} className="p-1.5 border-r border-zinc-800/60 bg-zinc-950/40 opacity-20" />;
                    }
                    if (statusFiltro === 'LIVRES' && !isLivre) {
                      return <td key={quadra.id_quadra} className="p-1.5 border-r border-zinc-800/60 bg-zinc-950/40 opacity-20" />;
                    }

                    // Checar se corresponde à busca por termo
                    const atendeBusca = buscaTermo.trim() !== '' && agendamento && (
                      agendamento.nomeUsuario.toLowerCase().includes(buscaTermo.toLowerCase()) ||
                      (agendamento.telefoneUsuario && agendamento.telefoneUsuario.includes(buscaTermo))
                    );

                    return (
                      <td
                        key={quadra.id_quadra}
                        className="p-1.5 sm:p-2 border-r border-zinc-800/60 align-top relative group min-h-[58px]"
                      >
                        {agendamento ? (
                          /* CARD DE AGENDAMENTO */
                          <div
                            onClick={() => onAbrirAgendamento(agendamento)}
                            className={`rounded-xl p-2.5 cursor-pointer transition-all duration-150 flex flex-col justify-between shadow-sm relative overflow-hidden group/card ${
                              agendamento.status === 'CONFIRMADO'
                                ? 'bg-emerald-950/50 hover:bg-emerald-900/60 border border-emerald-800/80 text-emerald-100'
                                : 'bg-amber-950/50 hover:bg-amber-900/60 border border-amber-800/80 text-amber-100'
                            } ${
                              atendeBusca ? 'ring-2 ring-white shadow-lg scale-[1.02] z-10' : ''
                            }`}
                          >
                            <div className="flex items-center justify-between gap-1">
                              <div className="flex items-center gap-1 min-w-0">
                                <User className="w-3 h-3 flex-shrink-0 text-zinc-300" />
                                <span className="text-xs font-bold truncate text-white">
                                  {agendamento.nomeUsuario}
                                </span>
                              </div>
                              <span
                                className={`text-[10px] font-bold px-1.5 py-0.5 rounded uppercase tracking-wider flex-shrink-0 ${
                                  agendamento.status === 'CONFIRMADO'
                                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                                    : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                                }`}
                              >
                                {agendamento.status === 'CONFIRMADO' ? 'Pago' : 'Pix Pend.'}
                              </span>
                            </div>

                            {agendamento.telefoneUsuario && (
                              <div className="flex items-center gap-1 text-[11px] text-zinc-400 mt-1">
                                <Phone className="w-3 h-3 text-zinc-500 flex-shrink-0" />
                                <span className="truncate">{agendamento.telefoneUsuario}</span>
                              </div>
                            )}

                            <div className="flex items-center justify-between mt-1 text-[10px] text-zinc-400">
                              <span>R$ {agendamento.valorTotal.toFixed(2)}</span>
                              <span className="text-zinc-500 group-hover/card:text-white transition">Detalhes &rarr;</span>
                            </div>
                          </div>
                        ) : bloqueio ? (
                          /* CARD DE BLOQUEIO */
                          <div className="rounded-xl p-2.5 bg-zinc-900/90 border border-zinc-700/60 text-zinc-300 flex flex-col justify-between group/block transition">
                            <div className="flex items-center justify-between gap-1">
                              <div className="flex items-center gap-1.5 min-w-0">
                                <Lock className="w-3.5 h-3.5 text-rose-400 flex-shrink-0" />
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
                                className="opacity-0 group-hover/block:opacity-100 p-1 hover:bg-zinc-800 text-zinc-400 hover:text-rose-400 rounded transition"
                              >
                                <Ban className="w-3.5 h-3.5" />
                              </button>
                            </div>
                            <span className="text-[10px] text-zinc-500 mt-1">
                              {bloqueio.horaInicio ? `${bloqueio.horaInicio.slice(0, 5)} - ${bloqueio.horaFim?.slice(0, 5)}` : 'Dia todo'}
                            </span>
                          </div>
                        ) : (
                          /* SLOT LIVRE */
                          <div className="h-full min-h-[46px] rounded-xl border border-dashed border-zinc-850 hover:border-emerald-500/50 hover:bg-emerald-950/10 transition-all flex items-center justify-center group/slot">
                            <button
                              onClick={() => onBloquearSlot(quadra.id_quadra, dataSelecionada, horaStr, proximaHoraStr)}
                              className="opacity-0 group-hover/slot:opacity-100 inline-flex items-center gap-1 text-[11px] font-medium text-emerald-400 hover:text-emerald-300 bg-emerald-950/60 px-2 py-1 rounded-lg border border-emerald-800/60 shadow transition transform scale-95 group-hover/slot:scale-100"
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
