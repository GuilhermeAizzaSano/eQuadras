import React from 'react';
import { Quadra, BloqueioHorario, Agendamento, DiaSemana } from '../../types';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Ban
} from 'lucide-react';

interface CalendarOccupancyProps {
  currentMonthDate: Date;
  dataSelecionada: string;
  quadraFiltroCalendarId: number | 'TODAS';
  statusFiltroCalendar: 'TODOS' | 'LIVRES' | 'AGENDADOS' | 'BLOQUEADOS';
  minhasQuadras: Quadra[];
  agendamentosAdmin: Agendamento[];
  mapaBloqueiosPorQuadra: Record<number, BloqueioHorario[]>;
  onQuadraFiltroChange: (id: number | 'TODAS') => void;
  onStatusFiltroChange: (status: 'TODOS' | 'LIVRES' | 'AGENDADOS' | 'BLOQUEADOS') => void;
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
  onQuadraFiltroChange,
  onStatusFiltroChange,
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

  const days: {
    dayNum: number;
    iso: string;
    isCurrentMonth: boolean;
    isToday: boolean;
    isPassado: boolean;
    count: number;
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
      bloqueados: 0,
      livres: 0,
    });
  }

  const hojeIso = new Date().toISOString().split('T')[0];

  // Dias do mês atual
  for (let d = 1; d <= totalDaysInMonth; d++) {
    const iso = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    
    const quadrasConsideradas = quadraFiltroCalendarId === 'TODAS'
      ? minhasQuadras
      : minhasQuadras.filter((q) => q.id_quadra === quadraFiltroCalendarId);

    const count = agendamentosAdmin.filter(
      (a) => a.dataHoraInicio.startsWith(iso) && 
             a.status !== 'CANCELADO' && 
             (quadraFiltroCalendarId === 'TODAS' || a.quadraId === quadraFiltroCalendarId)
    ).length;

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
      bloqueados: 0,
      livres: 0,
    });
  }

  return (
    <div className="bg-zinc-950 border border-zinc-850 rounded-2xl p-6 sm:p-7 shadow-2xl space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-850/80 pb-5">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2.5">
            <CalendarIcon className="w-5 h-5 text-emerald-400" />
            Calendário Mensal de Ocupação
          </h2>
          <p className="text-xs text-zinc-400 mt-1">
            Visão completa de disponibilidade. Clique em qualquer dia para abrir a agenda detalhada com os horários.
          </p>
        </div>

        {/* Controles de Filtros: Por Quadra e Por Status */}
        <div className="flex flex-wrap items-center gap-3">
          {minhasQuadras.length > 0 && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-zinc-400 font-medium">Quadra:</span>
              <select
                value={quadraFiltroCalendarId}
                onChange={(e) => {
                  const val = e.target.value;
                  onQuadraFiltroChange(val === 'TODAS' ? 'TODAS' : Number(val));
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
          )}

          {/* Filtro de Status no Calendário */}
          <div className="flex items-center gap-1 bg-zinc-900 border border-zinc-800 p-1 rounded-xl text-xs">
            <button
              type="button"
              onClick={() => onStatusFiltroChange('TODOS')}
              className={`px-2.5 py-1 rounded-lg font-medium transition ${
                statusFiltroCalendar === 'TODOS'
                  ? 'bg-zinc-750 text-white shadow-sm font-semibold'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              Todos
            </button>
            <button
              type="button"
              onClick={() => onStatusFiltroChange('LIVRES')}
              className={`px-2.5 py-1 rounded-lg font-medium transition flex items-center gap-1.5 ${
                statusFiltroCalendar === 'LIVRES'
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 font-semibold'
                  : 'text-zinc-400 hover:text-emerald-400'
              }`}
              title="Destacar dias com horários disponíveis"
            >
              <span className="w-2 h-2 rounded-full bg-emerald-400" />
              <span>Disponíveis</span>
            </button>
            <button
              type="button"
              onClick={() => onStatusFiltroChange('AGENDADOS')}
              className={`px-2.5 py-1 rounded-lg font-medium transition flex items-center gap-1.5 ${
                statusFiltroCalendar === 'AGENDADOS'
                  ? 'bg-blue-500/20 text-blue-300 border border-blue-500/40 font-semibold'
                  : 'text-zinc-400 hover:text-blue-400'
              }`}
              title="Filtrar dias que possuem agendamentos"
            >
              <span className="w-2 h-2 rounded-full bg-blue-400" />
              <span>Agendados</span>
            </button>
            <button
              type="button"
              onClick={() => onStatusFiltroChange('BLOQUEADOS')}
              className={`px-2.5 py-1 rounded-lg font-medium transition flex items-center gap-1.5 ${
                statusFiltroCalendar === 'BLOQUEADOS'
                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/40 font-semibold'
                  : 'text-zinc-400 hover:text-amber-400'
              }`}
              title="Filtrar dias que possuem horários ou dias bloqueados"
            >
              <Ban className="w-3 h-3 text-amber-400" />
              <span>Bloqueados</span>
            </button>
          </div>
        </div>

        {/* Navegação de Mês */}
        <div className="flex items-center gap-2 shrink-0">
          <span className="text-sm font-bold text-white px-2 font-mono">{mesAnoExtenso}</span>
          <div className="flex items-center bg-zinc-900 border border-zinc-800 rounded-xl p-0.5">
            <button
              onClick={() => onMudarMes(-1)}
              className="p-1.5 hover:bg-zinc-800 rounded-lg text-zinc-400 hover:text-white transition active:scale-95"
              title="Mês anterior"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => onMudarMes(1)}
              className="p-1.5 hover:bg-zinc-800 rounded-lg text-zinc-400 hover:text-white transition active:scale-95"
              title="Próximo mês"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Cabeçalho dos Dias da Semana */}
      <div className="grid grid-cols-7 gap-2 text-center text-xs font-bold text-zinc-400 uppercase tracking-wider pb-2 border-b border-zinc-850/60">
        <span className="text-red-400/90">Dom</span>
        <span>Seg</span>
        <span>Ter</span>
        <span>Qua</span>
        <span>Qui</span>
        <span>Sex</span>
        <span className="text-emerald-400/90">Sáb</span>
      </div>

      {/* Grade de Dias do Mês Expandida */}
      <div className="grid grid-cols-7 gap-2 sm:gap-3">
        {days.map((day, idx) => {
          const isSelected = dataSelecionada === day.iso;

          const atendeFiltroStatus =
            statusFiltroCalendar === 'TODOS'
              ? true
              : statusFiltroCalendar === 'LIVRES'
              ? day.livres > 0
              : statusFiltroCalendar === 'AGENDADOS'
              ? day.count > 0
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
              className={`min-h-[100px] sm:min-h-[115px] p-2.5 sm:p-3 rounded-2xl border flex flex-col justify-between items-start transition-all group ${
                !day.isCurrentMonth
                  ? 'opacity-20 bg-zinc-950/40 border-zinc-900 text-zinc-600 cursor-default'
                  : isDimmed
                  ? 'opacity-30 bg-zinc-950/60 border-zinc-900/60 hover:opacity-75'
                  : isSelected
                  ? 'bg-zinc-900 border-white/60 shadow-xl ring-2 ring-white/40 z-10'
                  : statusFiltroCalendar === 'LIVRES' && day.livres > 0
                  ? 'bg-emerald-950/15 border-emerald-500/40 hover:border-emerald-400 hover:bg-zinc-900 active:scale-[0.99] cursor-pointer'
                  : statusFiltroCalendar === 'AGENDADOS' && day.count > 0
                  ? 'bg-blue-950/20 border-blue-500/50 hover:border-blue-400 hover:bg-zinc-900 active:scale-[0.99] cursor-pointer'
                  : statusFiltroCalendar === 'BLOQUEADOS' && day.bloqueados > 0
                  ? 'bg-amber-950/20 border-amber-500/50 hover:border-amber-400 hover:bg-zinc-900 active:scale-[0.99] cursor-pointer'
                  : 'bg-zinc-900/50 border-zinc-850 hover:border-emerald-500/50 hover:bg-zinc-900 active:scale-[0.99] cursor-pointer'
              }`}
            >
              {/* Linha Superior: Dia e Tag Hoje */}
              <div className="w-full flex items-center justify-between">
                <span
                  className={`text-xs sm:text-sm font-bold font-mono transition ${
                    day.isToday
                      ? 'w-6 h-6 rounded-full bg-emerald-400 text-zinc-950 flex items-center justify-center font-extrabold shadow-sm'
                      : isSelected
                      ? 'text-white'
                      : 'text-zinc-300 group-hover:text-white'
                  }`}
                >
                  {day.dayNum}
                </span>

                {day.isCurrentMonth && (
                  <span className="text-[10px] text-zinc-500 opacity-0 group-hover:opacity-100 transition flex items-center gap-0.5">
                    <span>abrir</span>
                    <span>→</span>
                  </span>
                )}
              </div>

              {/* Linha Central / Inferior: Contadores de Horários */}
              {day.isCurrentMonth && (
                <div className="w-full space-y-1 mt-2">
                  {day.count > 0 && (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-md border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'AGENDADOS'
                          ? 'bg-blue-500/25 border-blue-400 text-blue-200 ring-1 ring-blue-500/30 font-semibold'
                          : 'bg-blue-500/15 border-blue-500/30 text-blue-300'
                      }`}
                    >
                      <span className="flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-blue-400 shrink-0" />
                        <span className="truncate">Agendados</span>
                      </span>
                      <span className="font-bold">{day.count}</span>
                    </div>
                  )}

                  {day.bloqueados > 0 && (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-md border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'BLOQUEADOS'
                          ? 'bg-amber-500/25 border-amber-400 text-amber-200 ring-1 ring-amber-500/30 font-semibold'
                          : 'bg-amber-500/15 border-amber-500/30 text-amber-300'
                      }`}
                    >
                      <span className="flex items-center gap-1">
                        <Ban className="w-2.5 h-2.5 text-amber-400 shrink-0" />
                        <span className="truncate">Bloqueados</span>
                      </span>
                      <span className="font-bold">{day.bloqueados}</span>
                    </div>
                  )}

                  {day.isPassado ? (
                    <div className="w-full px-1.5 py-0.5 rounded-md border border-zinc-850 bg-zinc-900/60 text-zinc-500 text-[10px] sm:text-[11px] font-mono flex items-center justify-between">
                      <span className="flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-zinc-600 shrink-0" />
                        <span className="truncate">Encerrado</span>
                      </span>
                      <span className="text-[9px] uppercase font-semibold">Passado</span>
                    </div>
                  ) : (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-md border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'LIVRES'
                          ? 'bg-emerald-500/20 border-emerald-400 text-emerald-200 ring-1 ring-emerald-500/30 font-semibold'
                          : 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
                      }`}
                    >
                      <span className="flex items-center gap-1">
                        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 shrink-0" />
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
