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
    <div className="bg-[#121214] border border-white/[0.08] rounded-3xl p-6 sm:p-7 shadow-apple-card shadow-[inset_0_1px_0_0_rgba(255,255,255,0.05)] space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/[0.08] pb-5">
        <div>
          <h2 className="text-lg font-semibold text-white flex items-center gap-2.5 tracking-tight">
            <div className="w-8 h-8 rounded-xl bg-white/[0.06] border border-white/[0.1] flex items-center justify-center">
              <CalendarIcon className="w-4 h-4 text-white/80" />
            </div>
            Calendário Mensal de Ocupação
          </h2>
          <p className="text-xs text-white/50 mt-1 tracking-tight">
            Visão consolidada de disponibilidade. Clique em qualquer data para inspecionar a agenda e horários.
          </p>
        </div>

        {/* Navegação de Mês */}
        <div className="flex items-center gap-2.5 shrink-0">
          <span className="text-sm font-semibold text-white px-2 tracking-tight">{mesAnoExtenso}</span>
          <div className="flex items-center bg-white/[0.04] border border-white/[0.08] rounded-xl p-0.5">
            <button
              onClick={() => onMudarMes(-1)}
              className="p-1.5 hover:bg-white/[0.08] rounded-lg text-white/60 hover:text-white transition active:scale-95 cursor-pointer"
              title="Mês anterior"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={() => onMudarMes(1)}
              className="p-1.5 hover:bg-white/[0.08] rounded-lg text-white/60 hover:text-white transition active:scale-95 cursor-pointer"
              title="Próximo mês"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>

      {/* Cabeçalho dos Dias da Semana */}
      <div className="grid grid-cols-7 gap-2 text-center text-[11px] font-semibold text-white/40 uppercase tracking-wider pb-2 border-b border-white/[0.08]">
        <span className="text-white/40">Dom</span>
        <span>Seg</span>
        <span>Ter</span>
        <span>Qua</span>
        <span>Qui</span>
        <span>Sex</span>
        <span className="text-white/40">Sáb</span>
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
                  ? 'opacity-20 bg-transparent border-white/[0.03] text-white/30 cursor-default'
                  : isDimmed
                  ? 'opacity-30 bg-transparent border-white/[0.04] hover:opacity-75'
                  : isSelected
                  ? 'bg-white/[0.08] border-white/40 shadow-sm ring-1 ring-white/20 z-10'
                  : statusFiltroCalendar === 'PENDENTES' && day.pendentes > 0
                  ? 'bg-[#FF9F0A]/10 border-[#FF9F0A]/30 hover:border-[#FF9F0A]/50 hover:bg-white/[0.06] active:scale-[0.99] cursor-pointer'
                  : statusFiltroCalendar === 'REALIZADOS' && day.realizados > 0
                  ? 'bg-[#BF5AF2]/10 border-[#BF5AF2]/30 hover:border-[#BF5AF2]/50 hover:bg-white/[0.06] active:scale-[0.99] cursor-pointer'
                  : (statusFiltroCalendar === 'AGENDADOS' || statusFiltroCalendar === 'CONFIRMADOS') && (day.confirmados > 0 || day.count > 0)
                  ? 'bg-[#0A84FF]/10 border-[#0A84FF]/30 hover:border-[#0A84FF]/50 hover:bg-white/[0.06] active:scale-[0.99] cursor-pointer'
                  : statusFiltroCalendar === 'LIVRES' && day.livres > 0
                  ? 'bg-[#30D158]/10 border-[#30D158]/30 hover:border-[#30D158]/50 hover:bg-white/[0.06] active:scale-[0.99] cursor-pointer'
                  : statusFiltroCalendar === 'BLOQUEADOS' && day.bloqueados > 0
                  ? 'bg-[#FF453A]/10 border-[#FF453A]/30 hover:border-[#FF453A]/50 hover:bg-white/[0.06] active:scale-[0.99] cursor-pointer'
                  : 'bg-white/[0.03] border-white/[0.06] hover:border-white/[0.18] hover:bg-white/[0.06] active:scale-[0.99] cursor-pointer'
              }`}
            >
              {/* Linha Superior: Dia e Tag Hoje */}
              <div className="w-full flex items-center justify-between">
                <span
                  className={`text-xs sm:text-sm font-semibold font-mono transition ${
                    day.isToday
                      ? 'w-6 h-6 rounded-full bg-white text-black flex items-center justify-center font-bold shadow-sm'
                      : isSelected
                      ? 'text-white font-bold'
                      : 'text-white/80 group-hover:text-white'
                  }`}
                >
                  {day.dayNum}
                </span>

                {day.isCurrentMonth && (
                  <span className="text-[10px] text-white/40 opacity-0 group-hover:opacity-100 transition flex items-center gap-0.5 font-mono">
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
                          ? 'bg-[#0A84FF]/20 border-[#0A84FF]/40 text-[#0A84FF] font-semibold'
                          : 'bg-[#0A84FF]/10 border-[#0A84FF]/25 text-[#0A84FF]'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-[#0A84FF] shrink-0" />
                        <span className="truncate">Confirmados</span>
                      </span>
                      <span className="font-bold">{day.confirmados}</span>
                    </div>
                  )}

                  {day.realizados > 0 && (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-lg border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'REALIZADOS'
                          ? 'bg-[#BF5AF2]/20 border-[#BF5AF2]/40 text-[#BF5AF2] font-semibold'
                          : 'bg-[#BF5AF2]/10 border-[#BF5AF2]/25 text-[#BF5AF2]'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-[#BF5AF2] shrink-0" />
                        <span className="truncate">Realizados</span>
                      </span>
                      <span className="font-bold">{day.realizados}</span>
                    </div>
                  )}

                  {day.pendentes > 0 && (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-lg border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'PENDENTES'
                          ? 'bg-[#FF9F0A]/20 border-[#FF9F0A]/40 text-[#FF9F0A] font-semibold'
                          : 'bg-[#FF9F0A]/10 border-[#FF9F0A]/25 text-[#FF9F0A]'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-[#FF9F0A] shrink-0 animate-pulse" />
                        <span className="truncate">Pendentes Pix</span>
                      </span>
                      <span className="font-bold">{day.pendentes}</span>
                    </div>
                  )}

                  {day.bloqueados > 0 && (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-lg border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'BLOQUEADOS'
                          ? 'bg-[#FF453A]/20 border-[#FF453A]/40 text-[#FF453A] font-semibold'
                          : 'bg-[#FF453A]/10 border-[#FF453A]/25 text-[#FF453A]'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <Ban className="w-2.5 h-2.5 text-[#FF453A] shrink-0" />
                        <span className="truncate">Bloqueados</span>
                      </span>
                      <span className="font-bold">{day.bloqueados}</span>
                    </div>
                  )}

                  {day.isPassado ? (
                    <div className="w-full px-1.5 py-0.5 rounded-lg border border-white/[0.04] bg-white/[0.02] text-white/30 text-[10px] sm:text-[11px] font-mono flex items-center justify-between">
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-white/20 shrink-0" />
                        <span className="truncate">Encerrado</span>
                      </span>
                      <span className="text-[9px] uppercase font-semibold">Passado</span>
                    </div>
                  ) : (
                    <div
                      className={`w-full px-1.5 py-0.5 rounded-lg border text-[10px] sm:text-[11px] font-mono flex items-center justify-between ${
                        statusFiltroCalendar === 'LIVRES'
                          ? 'bg-[#30D158]/20 border-[#30D158]/40 text-[#30D158] font-semibold'
                          : 'bg-[#30D158]/10 border-[#30D158]/25 text-[#30D158]'
                      }`}
                    >
                      <span className="flex items-center gap-1.5">
                        <span className="w-1.5 h-1.5 rounded-full bg-[#30D158] shrink-0" />
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
