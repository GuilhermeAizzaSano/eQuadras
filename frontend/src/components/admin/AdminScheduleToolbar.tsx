import React from 'react';
import { Quadra } from '../../types';
import {
  Calendar as CalendarIcon,
  ChevronLeft,
  ChevronRight,
  Columns,
  Search,
  RotateCcw,
} from 'lucide-react';

interface AdminScheduleToolbarProps {
  dataSelecionada: string;
  viewMode: 'TIMELINE' | 'CALENDAR';
  minhasQuadras: Quadra[];
  quadraFiltroId: number | 'TODAS';
  statusFiltro: 'TODOS' | 'CONFIRMADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES';
  buscaTermo: string;
  onDataChange: (data: string) => void;
  onViewModeChange: (mode: 'TIMELINE' | 'CALENDAR') => void;
  onQuadraFiltroChange: (id: number | 'TODAS') => void;
  onStatusFiltroChange: (status: 'TODOS' | 'CONFIRMADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES') => void;
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

  const hojeIso = new Date().toISOString().split('T')[0];
  const isHoje = dataSelecionada === hojeIso;

  const statusOptions: Array<{
    id: 'TODOS' | 'CONFIRMADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES';
    label: string;
    activeClass: string;
    dotClass: string;
  }> = [
    { id: 'TODOS', label: 'Todos', activeClass: 'bg-zinc-800 text-white border-zinc-700', dotClass: 'bg-zinc-400' },
    { id: 'CONFIRMADOS', label: 'Confirmados', activeClass: 'bg-emerald-950/80 text-emerald-300 border-emerald-800/80', dotClass: 'bg-emerald-400' },
    { id: 'PENDENTES', label: 'Pendentes Pix', activeClass: 'bg-amber-950/80 text-amber-300 border-amber-800/80', dotClass: 'bg-amber-400' },
    { id: 'BLOQUEADOS', label: 'Bloqueados', activeClass: 'bg-rose-950/80 text-rose-300 border-rose-800/80', dotClass: 'bg-rose-400' },
    { id: 'LIVRES', label: 'Livres', activeClass: 'bg-blue-950/80 text-blue-300 border-blue-800/80', dotClass: 'bg-blue-400' },
  ];

  return (
    <div className="bg-zinc-900 border border-zinc-800/80 rounded-2xl p-4 sm:p-5 shadow-lg space-y-4">
      {/* Top Bar: Data, Navegação e Alternador de View */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        {/* Navegação de Datas */}
        <div className="flex flex-wrap items-center gap-2 sm:gap-3">
          <div className="inline-flex items-center rounded-xl bg-zinc-950 p-1 border border-zinc-800 shadow-inner">
            <button
              onClick={() => navegarDia(-1)}
              title="Dia anterior"
              className="p-1.5 sm:p-2 text-zinc-400 hover:text-white hover:bg-zinc-900 rounded-lg transition"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button
              onClick={onHojeClick}
              className={`px-2.5 py-1 text-xs font-semibold rounded-lg transition ${
                isHoje
                  ? 'bg-emerald-600/20 text-emerald-400 border border-emerald-500/30'
                  : 'text-zinc-300 hover:text-white hover:bg-zinc-900'
              }`}
            >
              Hoje
            </button>
            <button
              onClick={() => navegarDia(1)}
              title="Próximo dia"
              className="p-1.5 sm:p-2 text-zinc-400 hover:text-white hover:bg-zinc-900 rounded-lg transition"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>

          {/* Date Picker Nativo estilizado */}
          <div className="relative flex items-center">
            <input
              type="date"
              value={dataSelecionada}
              onChange={(e) => e.target.value && onDataChange(e.target.value)}
              className="bg-zinc-950 text-white text-xs sm:text-sm font-medium border border-zinc-800 rounded-xl px-3 py-1.5 sm:py-2 focus:outline-none focus:ring-2 focus:ring-emerald-500 transition cursor-pointer [color-scheme:dark]"
            />
          </div>

          {/* Data por Extenso */}
          <span className="text-xs sm:text-sm font-semibold text-zinc-200 hidden md:inline-block pl-1">
            {formatarDataExtenso(dataSelecionada)}
          </span>
        </div>

        {/* Alternador de Visualização (Grade vs Calendário) */}
        <div className="flex items-center gap-2">
          <div className="inline-flex rounded-xl bg-zinc-950 p-1 border border-zinc-800 shadow-inner">
            <button
              onClick={() => onViewModeChange('TIMELINE')}
              className={`flex items-center gap-2 px-3 py-1.5 text-xs font-semibold rounded-lg transition ${
                viewMode === 'TIMELINE'
                  ? 'bg-white text-zinc-950 shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              <Columns className="w-3.5 h-3.5" />
              <span>Grade Diária</span>
            </button>
            <button
              onClick={() => onViewModeChange('CALENDAR')}
              className={`flex items-center gap-2 px-3 py-1.5 text-xs font-semibold rounded-lg transition ${
                viewMode === 'CALENDAR'
                  ? 'bg-white text-zinc-950 shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              <CalendarIcon className="w-3.5 h-3.5" />
              <span>Visão Mensal</span>
            </button>
          </div>
        </div>
      </div>

      {/* Linha 2: Filtros (Quadras na Dropbox, Status e Busca) */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3 pt-3 border-t border-zinc-800/80">
        <div className="flex flex-col sm:flex-row flex-wrap items-start sm:items-center gap-3">
          {/* Dropdown exclusiva de Quadras */}
          <div className="flex items-center gap-2">
            <label className="text-xs text-zinc-400 font-medium whitespace-nowrap">Quadra:</label>
            <select
              value={quadraFiltroId}
              onChange={(e) => {
                const val = e.target.value;
                onQuadraFiltroChange(val === 'TODAS' ? 'TODAS' : Number(val));
              }}
              className="bg-zinc-950 border border-zinc-800 text-xs sm:text-sm text-white rounded-xl px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-emerald-500 transition cursor-pointer [color-scheme:dark]"
            >
              <option value="TODAS">Todas as Quadras ({minhasQuadras.length})</option>
              {minhasQuadras.map((q) => (
                <option key={q.id_quadra} value={q.id_quadra}>
                  {q.nome} {!q.ativa ? '(Inativa)' : ''}
                </option>
              ))}
            </select>
          </div>

          <div className="h-4 w-px bg-zinc-800 hidden sm:block" />

          {/* Pílulas de Status */}
          <div className="flex items-center gap-1.5 overflow-x-auto max-w-full pb-1 sm:pb-0 scrollbar-thin">
            {statusOptions.map((opt) => {
              const active = statusFiltro === opt.id;
              return (
                <button
                  key={opt.id}
                  onClick={() => onStatusFiltroChange(opt.id)}
                  className={`flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium rounded-full border transition whitespace-nowrap ${
                    active
                      ? opt.activeClass
                      : 'bg-zinc-950/50 text-zinc-400 border-zinc-800/80 hover:text-white hover:border-zinc-700'
                  }`}
                >
                  <span className={`w-1.5 h-1.5 rounded-full ${opt.dotClass}`} />
                  <span>{opt.label}</span>
                </button>
              );
            })}
          </div>
        </div>

        {/* Input de Busca */}
        <div className="relative w-full lg:w-72">
          <Search className="w-4 h-4 text-zinc-500 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            placeholder="Buscar por cliente ou telefone..."
            value={buscaTermo}
            onChange={(e) => onBuscaTermoChange(e.target.value)}
            className="w-full bg-zinc-950 border border-zinc-800 text-white text-xs sm:text-sm pl-9 pr-8 py-1.5 rounded-xl focus:outline-none focus:ring-2 focus:ring-emerald-500 transition placeholder:text-zinc-600"
          />
          {buscaTermo && (
            <button
              onClick={() => onBuscaTermoChange('')}
              title="Limpar busca"
              className="absolute right-2.5 top-1/2 -translate-y-1/2 p-0.5 text-zinc-500 hover:text-white rounded"
            >
              <RotateCcw className="w-3.5 h-3.5" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
