import React from 'react';
import { Quadra } from '../../types';
import {
  ChevronLeft,
  ChevronRight,
  Filter,
  Search,
  Grid,
  CalendarDays,
} from 'lucide-react';
import { getHojeLocalIso } from '../../utils/dateUtils';

export interface AdminScheduleToolbarProps {
  dataSelecionada: string;
  viewMode: 'TIMELINE' | 'CALENDAR';
  minhasQuadras: Quadra[];
  quadraFiltroId: number | 'TODAS';
  statusFiltro: 'TODOS' | 'CONFIRMADOS' | 'REALIZADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES';
  buscaTermo: string;
  onDataChange: (dataIso: string) => void;
  onViewModeChange: (mode: 'TIMELINE' | 'CALENDAR') => void;
  onQuadraFiltroChange: (quadraId: number | 'TODAS') => void;
  onStatusFiltroChange: (
    status: 'TODOS' | 'CONFIRMADOS' | 'REALIZADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES'
  ) => void;
  onBuscaTermoChange: (termo: string) => void;
  onHojeClick: () => void;
}

export const AdminScheduleToolbar: React.FC<AdminScheduleToolbarProps> = ({
  dataSelecionada,
  viewMode,
  minhasQuadras,
  quadraFiltroId,
  statusFiltro,
  buscaTermo,
  onDataChange,
  onViewModeChange,
  onQuadraFiltroChange,
  onStatusFiltroChange,
  onBuscaTermoChange,
  onHojeClick,
}) => {
  const navegarDia = (offset: number) => {
    try {
      const [ano, mes, dia] = dataSelecionada.split('-').map(Number);
      const d = new Date(ano, mes - 1, dia);
      d.setDate(d.getDate() + offset);
      const novoAno = d.getFullYear();
      const novoMes = String(d.getMonth() + 1).padStart(2, '0');
      const novoDia = String(d.getDate()).padStart(2, '0');
      onDataChange(`${novoAno}-${novoMes}-${novoDia}`);
    } catch {
      // fallback
    }
  };

  const formatarDataExtenso = (iso: string) => {
    try {
      const [ano, mes, dia] = iso.split('-').map(Number);
      const d = new Date(ano, mes - 1, dia);
      return d.toLocaleDateString('pt-BR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
      });
    } catch {
      return iso;
    }
  };

  const hojeIso = getHojeLocalIso();
  const isHoje = dataSelecionada === hojeIso;

  const statusOptions: Array<{
    id: 'TODOS' | 'CONFIRMADOS' | 'REALIZADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES';
    label: string;
    dotClass: string;
  }> = [
    { id: 'TODOS', label: 'Todos', dotClass: 'bg-surface-400' },
    { id: 'CONFIRMADOS', label: 'Confirmados', dotClass: 'bg-blue-400 shadow-[0_0_6px_#60a5fa]' },
    { id: 'REALIZADOS', label: 'Realizados', dotClass: 'bg-purple-400' },
    { id: 'PENDENTES', label: 'Pendentes Pix', dotClass: 'bg-amber-400 shadow-[0_0_6px_#fbbf24]' },
    { id: 'BLOQUEADOS', label: 'Bloqueados', dotClass: 'bg-rose-400' },
    { id: 'LIVRES', label: 'Livres', dotClass: 'bg-emerald-400 shadow-[0_0_6px_#34d399]' },
  ];

  return (
    <div className="bg-surface-900/90 border border-surface-800 rounded-2xl p-4 sm:p-5 shadow-xl shadow-black/40 space-y-4">
      {/* Top Bar: Data, Navegação e Alternador de View */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        {/* Controles de Data */}
        <div className="flex flex-wrap items-center gap-2 sm:gap-3">
          <div className="flex items-center bg-surface-950 p-1 rounded-xl border border-surface-800 shadow-inner">
            <button
              type="button"
              onClick={() => navegarDia(-1)}
              className="p-1.5 rounded-lg text-surface-400 hover:text-white hover:bg-surface-850 transition cursor-pointer"
              title="Dia anterior"
              aria-label="Dia anterior"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              type="button"
              onClick={onHojeClick}
              className={`px-3 py-1 rounded-lg text-xs font-bold transition cursor-pointer ${
                isHoje
                  ? 'bg-white text-surface-950 shadow-sm'
                  : 'text-surface-400 hover:text-white hover:bg-surface-850'
              }`}
            >
              Hoje
            </button>
            <button
              type="button"
              onClick={() => navegarDia(1)}
              className="p-1.5 rounded-lg text-surface-400 hover:text-white hover:bg-surface-850 transition cursor-pointer"
              title="Próximo dia"
              aria-label="Próximo dia"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative">
              <input
                type="date"
                value={dataSelecionada}
                onChange={(e) => onDataChange(e.target.value)}
                className="bg-surface-950 border border-surface-800 rounded-xl px-3 py-2 text-xs text-white font-mono focus:outline-none focus:border-brand-400 transition cursor-pointer"
              />
            </div>
            <span className="text-xs text-surface-400 capitalize hidden sm:inline font-medium">
              {formatarDataExtenso(dataSelecionada)}
            </span>
          </div>
        </div>

        {/* Alternador de Visualização: Timeline vs Calendário */}
        <div className="flex items-center gap-2 self-end md:self-auto">
          <div className="flex bg-surface-950 p-1 rounded-xl border border-surface-800">
            <button
              type="button"
              onClick={() => onViewModeChange('TIMELINE')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
                viewMode === 'TIMELINE'
                  ? 'bg-white text-surface-950 shadow-sm'
                  : 'text-surface-400 hover:text-white'
              }`}
            >
              <Grid className="w-3.5 h-3.5" />
              <span>Grade Horária</span>
            </button>
            <button
              type="button"
              onClick={() => onViewModeChange('CALENDAR')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
                viewMode === 'CALENDAR'
                  ? 'bg-white text-surface-950 shadow-sm'
                  : 'text-surface-400 hover:text-white'
              }`}
            >
              <CalendarDays className="w-3.5 h-3.5" />
              <span>Mês</span>
            </button>
          </div>
        </div>
      </div>

      {/* Linha de Filtros: Quadra, Busca e Status */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3 pt-3 border-t border-surface-800">
        <div className="flex flex-wrap items-center gap-2.5">
          {/* Filtro de Quadras */}
          <div className="relative min-w-[170px]">
            <select
              value={quadraFiltroId}
              onChange={(e) =>
                onQuadraFiltroChange(e.target.value === 'TODAS' ? 'TODAS' : Number(e.target.value))
              }
              className="w-full bg-surface-950 border border-surface-800 rounded-xl px-3 py-2 text-xs font-semibold text-white focus:outline-none focus:border-brand-400 transition appearance-none cursor-pointer pr-8"
            >
              <option value="TODAS">Todas as Quadras ({minhasQuadras.length})</option>
              {minhasQuadras.map((q) => (
                <option key={q.id_quadra} value={q.id_quadra}>
                  {q.nome} ({q.tipoEsporte.replace('_', ' ')})
                </option>
              ))}
            </select>
            <Filter className="w-3.5 h-3.5 text-surface-400 absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
          </div>

          {/* Busca por Nome/Telefone */}
          <div className="relative w-full sm:w-60">
            <input
              type="text"
              placeholder="Buscar atleta ou telefone..."
              value={buscaTermo}
              onChange={(e) => onBuscaTermoChange(e.target.value)}
              className="w-full bg-surface-950 border border-surface-800 rounded-xl pl-9 pr-3 py-2 text-xs text-white placeholder-surface-500 focus:outline-none focus:border-brand-400 transition"
            />
            <Search className="w-3.5 h-3.5 text-surface-400 absolute left-3 top-1/2 -translate-y-1/2" />
          </div>
        </div>

        {/* Filtro Rápido de Status (Pills) */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 max-w-full scrollbar-none">
          {statusOptions.map((opt) => {
            const isSelected = statusFiltro === opt.id;
            return (
              <button
                key={opt.id}
                type="button"
                onClick={() => onStatusFiltroChange(opt.id)}
                className={`text-[11px] font-semibold px-3 py-1.5 rounded-xl border transition-all flex items-center gap-1.5 whitespace-nowrap cursor-pointer active:scale-95 ${
                  isSelected
                    ? 'bg-white text-surface-950 font-bold border-white shadow-sm'
                    : 'bg-surface-950/80 text-surface-400 border-surface-800 hover:border-surface-700 hover:text-white'
                }`}
              >
                <span className={`w-2 h-2 rounded-full ${opt.dotClass}`} />
                <span>{opt.label}</span>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};
