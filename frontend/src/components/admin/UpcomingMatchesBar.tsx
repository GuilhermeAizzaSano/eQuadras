import React from 'react';
import { Agendamento, Quadra } from '../../types';
import { Clock, Phone, ChevronRight } from 'lucide-react';
import { Badge } from '../ui/Badge';

export interface UpcomingMatchesBarProps {
  agendamentosAdmin: Agendamento[];
  minhasQuadras: Quadra[];
  onAbrirAgendamento: (ag: Agendamento) => void;
}

export const UpcomingMatchesBar: React.FC<UpcomingMatchesBarProps> = ({
  agendamentosAdmin,
  minhasQuadras,
  onAbrirAgendamento,
}) => {
  const agora = new Date();
  const hojeIso = `${agora.getFullYear()}-${String(agora.getMonth() + 1).padStart(2, '0')}-${String(
    agora.getDate()
  ).padStart(2, '0')}`;

  const proximas4Horas = new Date(agora.getTime() + 4 * 60 * 60 * 1000);

  const proximosJogos = agendamentosAdmin
    .filter((ag) => {
      if (ag.status === 'CANCELADO') return false;
      const [data] = ag.dataHoraInicio.split('T');
      if (data !== hojeIso) return false;

      const dataFim = new Date(ag.dataHoraFim);
      const dataInicio = new Date(ag.dataHoraInicio);

      // Partida em andamento ou que começa nas próximas 4 horas
      const emAndamento = dataInicio <= agora && dataFim > agora;
      const emBreve = dataInicio >= agora && dataInicio <= proximas4Horas;

      return emAndamento || emBreve;
    })
    .sort((a, b) => new Date(a.dataHoraInicio).getTime() - new Date(b.dataHoraInicio).getTime());

  if (proximosJogos.length === 0) return null;

  return (
    <div className="bg-surface-900/90 border border-surface-800 rounded-2xl p-4 sm:p-5 shadow-xl shadow-black/40 space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span className="flex h-2 w-2 relative">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-brand-400 opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-brand-500 shadow-[0_0_6px_#34d399]" />
          </span>
          <span className="text-xs font-bold uppercase tracking-wider text-white">
            Partidas Imediatas (Hoje)
          </span>
        </div>
        <span className="text-[11px] font-mono text-surface-400 font-semibold">
          {proximosJogos.length} {proximosJogos.length === 1 ? 'partida' : 'partidas'}
        </span>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
        {proximosJogos.map((ag) => {
          const quadra = minhasQuadras.find((q) => q.id_quadra === ag.quadraId);
          const dataInicio = new Date(ag.dataHoraInicio);
          const dataFim = new Date(ag.dataHoraFim);
          const emAndamento = dataInicio <= agora && dataFim > agora;
          const horaInicio = ag.dataHoraInicio.split('T')[1]?.substring(0, 5) || '';
          const horaFim = ag.dataHoraFim.split('T')[1]?.substring(0, 5) || '';

          return (
            <div
              key={ag.id_agendamento}
              onClick={() => onAbrirAgendamento(ag)}
              className="p-3.5 rounded-xl bg-surface-950/80 border border-surface-800 hover:border-surface-700 transition flex items-center justify-between gap-3 cursor-pointer group active:scale-[0.99]"
            >
              <div className="space-y-1 min-w-0">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-white truncate max-w-[140px]">
                    {ag.nomeUsuario}
                  </span>
                  {emAndamento ? (
                    <Badge variant="success" className="text-[9px] py-0 px-1.5 animate-pulse" withDot>
                      AGORA
                    </Badge>
                  ) : (
                    <Badge variant="neutral" className="text-[9px] py-0 px-1.5">
                      EM BREVE
                    </Badge>
                  )}
                </div>

                <div className="flex items-center gap-2 text-[11px] text-surface-400 font-mono">
                  <Clock className="w-3.5 h-3.5 text-brand-400 shrink-0" />
                  <span>
                    {horaInicio} - {horaFim}
                  </span>
                  <span className="text-surface-600">•</span>
                  <span className="text-surface-300 truncate">{quadra?.nome || ag.nomeQuadra}</span>
                </div>

                {ag.telefoneUsuario && (
                  <div className="flex items-center gap-1.5 text-[10px] text-surface-400">
                    <Phone className="w-3 h-3 text-surface-400 shrink-0" />
                    <span>{ag.telefoneUsuario}</span>
                  </div>
                )}
              </div>

              <div className="p-2 rounded-xl bg-surface-900 border border-surface-800 group-hover:bg-brand-500 group-hover:text-surface-950 transition-all text-surface-400 shrink-0">
                <ChevronRight className="w-4 h-4" />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
