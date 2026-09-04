import React from 'react';
import { Agendamento, Quadra } from '../../types';
import { Clock, Phone, ChevronRight, Play, CheckCircle2 } from 'lucide-react';

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
  const hojeIso = agora.toISOString().split('T')[0];

  // Mapa rápido de id_quadra -> Quadra
  const quadrasMap = React.useMemo(() => {
    const map = new Map<number, Quadra>();
    minhasQuadras.forEach((q) => map.set(q.id_quadra, q));
    return map;
  }, [minhasQuadras]);

  // Filtrar jogos de hoje não cancelados
  const jogosProximos = React.useMemo(() => {
    const quatroHorasFrenteMs = agora.getTime() + 4 * 60 * 60 * 1000;
    const agoraMs = agora.getTime();

    return agendamentosAdmin
      .filter((ag) => {
        if (ag.status === 'CANCELADO') return false;
        if (!ag.dataHoraInicio.startsWith(hojeIso)) return false;

        const fimMs = new Date(ag.dataHoraFim).getTime();
        const inicioMs = new Date(ag.dataHoraInicio).getTime();

        // Jogo ainda não terminou E (já começou ou começa nas próximas 4 horas)
        return fimMs > agoraMs && inicioMs <= quatroHorasFrenteMs;
      })
      .sort((a, b) => new Date(a.dataHoraInicio).getTime() - new Date(b.dataHoraInicio).getTime());
  }, [agendamentosAdmin, hojeIso, agora]);

  if (jogosProximos.length === 0) {
    return null;
  }

  const formatarHora = (iso: string) => {
    try {
      const d = new Date(iso);
      return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    } catch {
      return '';
    }
  };

  const getStatusRelativo = (inicioIso: string, fimIso: string) => {
    const agoraMs = Date.now();
    const inicioMs = new Date(inicioIso).getTime();
    const fimMs = new Date(fimIso).getTime();

    if (agoraMs >= inicioMs && agoraMs < fimMs) {
      return {
        tipo: 'ANDAMENTO',
        label: 'Em andamento',
        badgeClass: 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30',
        pulsing: true,
      };
    }

    const diffMin = Math.round((inicioMs - agoraMs) / (1000 * 60));
    if (diffMin <= 0) {
      return {
        tipo: 'INICIANDO',
        label: 'Iniciando agora',
        badgeClass: 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30',
        pulsing: true,
      };
    }

    if (diffMin < 60) {
      return {
        tipo: 'BREVE',
        label: `Em ${diffMin} min`,
        badgeClass: 'bg-amber-500/20 text-amber-300 border border-amber-500/30',
        pulsing: false,
      };
    }

    const horas = Math.floor(diffMin / 60);
    const mins = diffMin % 60;
    return {
      tipo: 'HORARIO',
      label: mins > 0 ? `Em ${horas}h ${mins}m` : `Em ${horas}h`,
      badgeClass: 'bg-blue-500/20 text-blue-300 border border-blue-500/30',
      pulsing: false,
    };
  };

  return (
    <div className="bg-zinc-900/90 border border-zinc-800/90 rounded-2xl p-4 shadow-lg space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="relative flex h-2.5 w-2.5">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500"></span>
          </div>
          <h3 className="text-xs sm:text-sm font-bold tracking-tight text-white uppercase flex items-center gap-2">
            <span>Próximas Partidas de Hoje</span>
            <span className="text-[11px] font-medium text-zinc-400 normal-case bg-zinc-800 px-2 py-0.5 rounded-full">
              {jogosProximos.length} {jogosProximos.length === 1 ? 'jogo' : 'jogos'}
            </span>
          </h3>
        </div>
        <span className="text-[11px] text-zinc-500 hidden sm:inline-block">Próximas 4 horas</span>
      </div>

      {/* Carrossel / Barra de cards horizontais */}
      <div className="flex items-center gap-3 overflow-x-auto pb-2 scrollbar-thin">
        {jogosProximos.map((ag) => {
          const quadra = quadrasMap.get(ag.quadraId);
          const status = getStatusRelativo(ag.dataHoraInicio, ag.dataHoraFim);
          const horaInicioStr = formatarHora(ag.dataHoraInicio);
          const horaFimStr = formatarHora(ag.dataHoraFim);
          const telefoneLimpo = ag.telefoneUsuario ? ag.telefoneUsuario.replace(/\D/g, '') : '';

          return (
            <div
              key={ag.id_agendamento}
              onClick={() => onAbrirAgendamento(ag)}
              className="flex-shrink-0 w-72 sm:w-80 bg-zinc-950 hover:bg-zinc-850/80 border border-zinc-800/90 hover:border-zinc-700 rounded-xl p-3.5 transition cursor-pointer group shadow-sm flex flex-col justify-between gap-3"
            >
              {/* Header do Card: Quadra + Status Relativo */}
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <h4 className="text-sm font-semibold text-white truncate group-hover:text-emerald-400 transition">
                    {ag.nomeQuadra || quadra?.nome || 'Quadra'}
                  </h4>
                  <div className="flex items-center gap-1.5 text-xs text-zinc-400 mt-0.5">
                    <Clock className="w-3.5 h-3.5 text-zinc-500" />
                    <span>{horaInicioStr} - {horaFimStr}</span>
                    {quadra?.tipoEsporte && (
                      <>
                        <span className="text-zinc-600">•</span>
                        <span className="text-zinc-400 capitalize">{quadra.tipoEsporte.toLowerCase().replace('_', ' ')}</span>
                      </>
                    )}
                  </div>
                </div>

                <span
                  className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-semibold whitespace-nowrap ${status.badgeClass}`}
                >
                  {status.pulsing ? (
                    <Play className="w-2.5 h-2.5 fill-current" />
                  ) : (
                    <Clock className="w-2.5 h-2.5" />
                  )}
                  {status.label}
                </span>
              </div>

              {/* Footer do Card: Cliente, Telefone e Ação */}
              <div className="flex items-center justify-between pt-2 border-t border-zinc-900 gap-2">
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-medium text-zinc-300 truncate">
                    {ag.nomeUsuario}
                  </p>
                  {ag.telefoneUsuario && (
                    <a
                      href={`https://wa.me/55${telefoneLimpo}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={(e) => e.stopPropagation()}
                      className="inline-flex items-center gap-1 text-[11px] text-emerald-400 hover:text-emerald-300 hover:underline mt-0.5"
                    >
                      <Phone className="w-3 h-3" />
                      <span>{ag.telefoneUsuario}</span>
                    </a>
                  )}
                </div>

                <div className="flex items-center gap-1">
                  {ag.status === 'CONFIRMADO' && (
                    <span title="Confirmado" className="text-emerald-400">
                      <CheckCircle2 className="w-4 h-4" />
                    </span>
                  )}
                  <ChevronRight className="w-4 h-4 text-zinc-600 group-hover:text-white transition group-hover:translate-x-0.5" />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
