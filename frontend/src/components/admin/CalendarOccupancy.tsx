import React from 'react';
import { Quadra, BloqueioHorario, Agendamento, DiaSemana } from '../../types';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Ban
} from 'lucide-react';
import { parseDataHoraLocal, getHojeLocalIso } from '../../utils/dateUtils';

interface CalendarOccupancyProps {
  currentMonthDate: Date;
  dataSelecionada: string;
  quadraFiltroCalendarId: number | 'TODAS';
  statusFiltroCalendar: 'TODOS' | 'LIVRES' | 'AGENDADOS' | 'CONFIRMADOS' | 'REALIZADOS' | 'PENDENTES' | 'BLOQUEADOS';
  minhasQuadras: Quadra[];
  agendamentosAdmin: Agendamento[];
  mapaBloqueiosPorQuadra: Record<number, BloqueioHorario[]>;
  onMudarMes: (offset: number) => void;
  onAbrirAgendaDoDia: (dataIso: string) => void;
}

export const CalendarOccupancy: React.FC<CalendarOccupancyProps> = ({
  currentMonthDate,
  dataSelecionada,
  quadraFiltroCalendarId,
  statusFiltroCalendar,
  minhasQuadras,
  agendamentosAdmin,
  mapaBloqueiosPorQuadra,
  onMudarMes,
  onAbrirAgendaDoDia,
}) => {
  const meses = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];
  const mesAnoExtenso = `${meses[currentMonthDate.getMonth()]} de ${currentMonthDate.getFullYear()}`;

  const year = currentMonthDate.getFullYear();
  const month = currentMonthDate.getMonth();
  const firstDayOfWeek = new Date(year, month, 1).getDay(); // 0 = Domingo
  const totalDaysInMonth = new Date(year, month + 1, 0).getDate();
  const prevMonthTotalDays = new Date(year, month, 0).getDate();

  const agoraMomento = new Date();

  const days: {
    dayNum: number;
    iso: string;
    isCurrentMonth: boolean;
    isToday: boolean;
    isPassado: boolean;
    count: number;
    confirmados: number;
    realizados: number;
    pendentes: number;
    bloqueados: number;
    livres: number;
  }[] = [];

  // Dias do mês anterior para preencher a primeira semana
  for (let i = firstDayOfWeek - 1; i >= 0; i--) {
    const dayNum = prevMonthTotalDays - i;
    const d = new Date(year, month - 1, dayNum);
    const iso = d.toISOString().split('T')[0];
    days.push({
      dayNum,
      iso,
      isCurrentMonth: false,
      isToday: false,
      isPassado: true,
      count: 0,
      confirmados: 0,
      realizados: 0,
      pendentes: 0,
      bloqueados: 0,
      livres: 0,
    });
  }

  const hojeIso = getHojeLocalIso();

  // Dias do mês atual
  for (let d = 1; d <= totalDaysInMonth; d++) {
    const iso = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    
    const quadrasConsideradas = quadraFiltroCalendarId === 'TODAS'
      ? minhasQuadras
      : minhasQuadras.filter((q) => q.id_quadra === quadraFiltroCalendarId);

    const agendamentosDoDia = agendamentosAdmin.filter(
      (a) => a.dataHoraInicio.startsWith(iso) && 
             a.status !== 'CANCELADO' && 
             (quadraFiltroCalendarId === 'TODAS' || a.quadraId === quadraFiltroCalendarId)
    );

    const count = agendamentosDoDia.length;
    const pendentes = agendamentosDoDia.filter((a) => a.status === 'PENDENTE').length;
    
    // Separar agendamentos realizados (já passados) de confirmados futuros/em andamento
    let confirmados = 0;
    let realizados = 0;
    agendamentosDoDia.forEach((a) => {
      const dataFim = parseDataHoraLocal(a.dataHoraFim);
      const jaPassou = dataFim < agoraMomento;
      if (jaPassou) {
        realizados++;
      } else if (a.status === 'CONFIRMADO') {
        confirmados++;
      }
    });

    const bloqueiosDoDia: BloqueioHorario[] = [];
    quadrasConsideradas.forEach((q) => {
      const lista = mapaBloqueiosPorQuadra[q.id_quadra] || [];
      lista.forEach((b) => {
        if (b.data === iso) {
          bloqueiosDoDia.push(b);
        }
      });
    });

    const dayOfWeekNum = new Date(year, month, d).getDay();
    const diaMap: DiaSemana[] = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];
    const diaSemanaAtual = diaMap[dayOfWeekNum];

    let slotsPossiveis = 0;
    quadrasConsideradas.forEach((q) => {
      if (!q.ativa) return;
      if (q.dataLimiteAgendamento && iso > q.dataLimiteAgendamento) return;
      const disp = q.disponibilidades?.find((dd) => dd.diaSemana === diaSemanaAtual);
      if (disp) {
        const hIni = parseInt(disp.horaInicio.slice(0, 2), 10);
        const hFim = parseInt(disp.horaFim.slice(0, 2), 10);
        if (hFim > hIni) slotsPossiveis += (hFim - hIni);
      } else {
        slotsPossiveis += 17;
      }
    });

    const isPassado = iso < hojeIso;
    const isHoje = iso === hojeIso;
    const horaAtual = new Date().getHours();

    let slotsPassadosHoje = 0;
    let slotsBloqueadosReais = 0;

    quadrasConsideradas.forEach((q) => {
      if (!q.ativa) return;
      if (q.dataLimiteAgendamento && iso > q.dataLimiteAgendamento) return;
      const disp = q.disponibilidades?.find((dd) => dd.diaSemana === diaSemanaAtual);
      const hIni = disp ? parseInt(disp.horaInicio.slice(0, 2), 10) : 6;
      const hFim = disp ? parseInt(disp.horaFim.slice(0, 2), 10) : 23;

      const bQuadra = (mapaBloqueiosPorQuadra[q.id_quadra] || []).filter((b) => b.data === iso);
      const bDiaTodo = bQuadra.some((b) => !b.horaInicio || !b.horaFim);

      for (let h = hIni; h < hFim; h++) {
        const hPassada = isHoje && h < horaAtual;
        if (hPassada) {
          slotsPassadosHoje++;
          continue;
        }

        const hStr = `${String(h).padStart(2, '0')}:00:00`;
        const isBloq = bDiaTodo || bQuadra.some((b) => {
          if (!b.horaInicio || !b.horaFim) return true;
          return hStr >= b.horaInicio && hStr < b.horaFim;
        });

        if (isBloq) {
          slotsBloqueadosReais++;
        }
      }
    });

    const livresCont = isPassado
      ? 0
      : Math.max(0, slotsPossiveis - count - slotsPassadosHoje - slotsBloqueadosReais);

    const bloqueadosCont = isPassado ? 0 : slotsBloqueadosReais > 0 ? slotsBloqueadosReais : bloqueiosDoDia.length;

    days.push({
      dayNum: d,
      iso,
      isCurrentMonth: true,
      isToday: isHoje,
      isPassado,
      count,
      confirmados,
      realizados,
      pendentes,
      bloqueados: bloqueadosCont,
      livres: livresCont,
    });
  }

  // Dias do próximo mês para completar a grade
  const remaining = (7 - (days.length % 7)) % 7;
  for (let d = 1; d <= remaining; d++) {
    const nextDate = new Date(year, month + 1, d);
    const iso = nextDate.toISOString().split('T')[0];
    days.push({
      dayNum: d,
      iso,
      isCurrentMonth: false,
      isToday: false,
      isPassado: false,
      count: 0,
      confirmados: 0,
      realizados: 0,
      pendentes: 0,
      bloqueados: 0,
      livres: 0,
    });
  }

  return (
    <div className="bg-surface-900 border border-surface-800 rounded-3xl p-6 sm:p-7 shadow-2xl space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-surface-800 pb-5">
        <div>
          <h2 className="text-lg font-bold text-white flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-brand-500/10 border border-brand-500/20 flex items-center justify-center">
              <CalendarIcon className="w-4 h-4 text-brand-400" />
            </div>
            Calendário Mensal de Ocupação
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Visão consolidada de disponibilidade. Clique em qualquer data para inspecionar a agenda e horários.
          </p>
        </div>

        {/* Navegação de Mês */}
        <div className="flex items-center gap-2 shrink-0">
          <span className="text-sm font-bold text-white px-2 font-mono">{mesAnoExtenso}</span>
          <div className="flex items-center bg-surface-950 border border-surface-800 rounded-xl p-0.5">
            <button
              onClick={() => onMudarMes(-1)}
              className="p-1.5 hover:bg-surface-800 rounded-lg text-zinc-400 hover:text-white transition active:scale-95 cursor-pointer"
              title="Mês anterior"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => onMudarMes(1)}
              className="p-1.5 hover:bg-surface-800 rounded-lg text-zinc-400 hover:text-white transition active:scale-95 cursor-pointer"
              title="Próximo mês"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Cabeçalho dos Dias da Semana */}
      <div className="grid grid-cols-7 gap-2 text-center text-[11px] font-bold text-zinc-400 uppercase tracking-wider pb-2 border-b border-surface-800/80 font-mono">
        <span className="text-rose-400/90">Dom</span>
        <span>Seg</span>
        <span>Ter</span>
        <span>Qua</span>
        <span>Qui</span>
        <span>Sex</span>
        <span className="text-brand-400/90">Sáb</span>
      </div>

      {/* Grade de Dias do Mês Expandida */}
      <div className="grid grid-cols-7 gap-2 sm:gap-2.5">
        {days.map((day, idx) => {
          const isSelected = dataSelecionada === day.iso;

          const atendeFiltroStatus =
            statusFiltroCalendar === 'TODOS'
              ? true
              : statusFiltroCalendar === 'LIVRES'
              ? day.livres > 0
              : statusFiltroCalendar === 'AGENDADOS'
              ? day.count > 0
              : statusFiltroCalendar === 'CONFIRMADOS'
              ? day.confirmados > 0
              : statusFiltroCalendar === 'REALIZADOS'
              ? day.realizados > 0
              : statusFiltroCalendar === 'PENDENTES'
              ? day.pendentes > 0
              : statusFiltroCalendar === 'BLOQUEADOS'
              ? day.bloqueados > 0
              : true;

          const isDimmed = day.isCurrentMonth && !atendeFiltroStatus;

          return (
            <button
              key={idx}
              type="button"
              onClick={() => {
                if (!day.isCurrentMonth) return;
                onAbrirAgendaDoDia(day.iso);
              }}
              className={`min-h-[110px] sm:min-h-[124px] p-2.5 rounded-2xl border flex flex-col justify-between items-start transition-all group relative ${
                !day.isCurrentMonth
                  ? 'opacity-20 bg-surface-950/40 border-surface-900 text-zinc-600 cursor-default'
                  : isDimmed
                  ? 'opacity-30 bg-surface-950/60 border-surface-900/60 hover:opacity-75'
                  : isSelected
                  ? 'bg-surface-850 border-brand-400 shadow-xl ring-2 ring-brand-400/30 z-10'
                  : statusFiltroCalendar === 'PENDENTES' && day.pendentes > 0
                  ? 'bg-amber-950/20 border-amber-500/40 hover:border-amber-400 hover:bg-surface-850 active:scale-[0.99] cursor-pointer'
                  : statusFiltroCalendar === 'REALIZADOS' && day.realizados > 0
                  ? 'bg-purple-950/20 border-purple-500/40 hover:border-purple-400 hover:bg-surface-850 active:scale-[0.99] cursor-pointer'
                  : (statusFiltroCalendar === 'AGENDADOS' || statusFiltroCalendar === 'CONFIRMADOS') && (day.confirmados > 0 || day.count > 0)
                  ? 'bg-sky-950/20 border-sky-500/40 hover:border-sky-400 hover:bg-surface-850 active:scale-[0.99] cursor-pointer'
                  : statusFiltroCalendar === 'LIVRES' && day.livres > 0
                  ? 'bg-brand-950/20 border-brand-500/30 hover:border-brand-400 hover:bg-surface-850 active:scale-[0.99] cursor-pointer'
                  : statusFiltroCalendar === 'BLOQUEADOS' && day.bloqueados > 0
                  ? 'bg-rose-950/20 border-rose-500/40 hover:border-rose-400 hover:bg-surface-850 active:scale-[0.99] cursor-pointer'
                  : 'bg-surface-950/60 border-surface-800 hover:border-brand-500/40 hover:bg-surface-850/70 active:scale-[0.99] cursor-pointer'
              }`}
            >
              {/* Linha Superior: Dia e Tag Hoje */}
              <div className="w-full flex items-center justify-between">
                <span
                  className={`text-xs sm:text-sm font-bold font-mono transition ${
                    day.isToday
                      ? 'w-6 h-6 rounded-full bg-brand-400 text-surface-950 flex items-center justify-center font-extrabold shadow-sm'
                      : isSelected
                      ? 'text-white'
                      : 'text-zinc-300 group-hover:text-white'
                  }`}
                >
                  {day.dayNum}
                </span>

                {day.isCurrentMonth && (
                  <span className="text-[10px] text-zinc-500 opacity-0 group-hover:opacity-100 transition flex items-center gap-0.5 font-mono">
                    <span>abrir</span>
                    <span>→</span>
                  </span>
                )}
              </div>

              {/* Linha Central / Inferior: Contadores de Horários */}
              {day.isCurrentMonth && (
                <div className="w-full space-y-1 mt-2">
                  {day.confirmados > 0 && (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-lg border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'AGENDADOS' || statusFiltroCalendar === 'CONFIRMADOS'
                          ? 'bg-sky-500/25 border-sky-400 text-sky-200 ring-1 ring-sky-500/30 font-semibold'
                          : 'bg-sky-500/15 border-sky-500/30 text-sky-300'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-sky-400 shrink-0" />
                        <span className="truncate">Confirmados</span>
                      </span>
                      <span className="font-bold">{day.confirmados}</span>
                    </div>
                  )}

                  {day.realizados > 0 && (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-lg border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'REALIZADOS'
                          ? 'bg-purple-500/25 border-purple-400 text-purple-200 ring-1 ring-purple-500/30 font-semibold'
                          : 'bg-purple-500/15 border-purple-500/30 text-purple-300'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-purple-400 shrink-0" />
                        <span className="truncate">Realizados</span>
                      </span>
                      <span className="font-bold">{day.realizados}</span>
                    </div>
                  )}

                  {day.pendentes > 0 && (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-lg border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'PENDENTES'
                          ? 'bg-amber-500/25 border-amber-400 text-amber-200 ring-1 ring-amber-500/30 font-semibold'
                          : 'bg-amber-500/15 border-amber-500/30 text-amber-300'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-amber-400 shrink-0 animate-pulse" />
                        <span className="truncate">Pendentes Pix</span>
                      </span>
                      <span className="font-bold">{day.pendentes}</span>
                    </div>
                  )}

                  {day.bloqueados > 0 && (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-lg border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'BLOQUEADOS'
                          ? 'bg-rose-500/25 border-rose-400 text-rose-200 ring-1 ring-rose-500/30 font-semibold'
                          : 'bg-rose-500/15 border-rose-500/30 text-rose-300'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <Ban className="w-2.5 h-2.5 text-rose-400 shrink-0" />
                        <span className="truncate">Bloqueados</span>
                      </span>
                      <span className="font-bold">{day.bloqueados}</span>
                    </div>
                  )}

                  {day.isPassado ? (
                    <div className="w-full px-1.5 py-0.5 rounded-lg border border-surface-800 bg-surface-900/60 text-zinc-500 text-[10px] sm:text-[11px] font-mono flex items-center justify-between">
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-zinc-600 shrink-0" />
                        <span className="truncate">Encerrado</span>
                      </span>
                      <span className="text-[9px] uppercase font-semibold">Passado</span>
                    </div>
                  ) : (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-lg border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'LIVRES'
                          ? 'bg-brand-500/20 border-brand-400 text-brand-200 ring-1 ring-brand-500/30 font-semibold'
                          : 'bg-brand-500/10 border-brand-500/20 text-brand-400'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-brand-400 shrink-0" />
                        <span className="truncate">Livres</span>
                      </span>
                      <span className="font-bold">{day.livres}</span>
                    </div>
                  )}
                </div>
              )}
            </button>
          );
        })}
      </div>
    </div>
  );
};
