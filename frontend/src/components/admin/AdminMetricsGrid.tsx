import React from 'react';
import { DollarSign, TrendingUp, Users, ShieldCheck } from 'lucide-react';

interface AdminMetricsGridProps {
  metricas: {
    faturamentoTotal: number;
    reservasHoje: number;
    totalReservas: number;
    quadrasAtivas: number;
    totalQuadras: number;
  };
}

export const AdminMetricsGrid: React.FC<AdminMetricsGridProps> = ({ metricas }) => {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
      {/* Receita */}
      <div className="bg-surface-900/90 border border-surface-800 p-5 sm:p-6 rounded-2xl shadow-xl shadow-black/40 space-y-3 relative overflow-hidden group hover:border-surface-700 transition-all">
        <div className="absolute top-0 right-0 w-28 h-28 bg-emerald-500/5 rounded-full blur-2xl group-hover:bg-emerald-500/10 transition-all pointer-events-none" />
        <div className="flex items-center justify-between text-surface-400 text-[11px] uppercase tracking-wider font-bold">
          <span>Receita Total</span>
          <div className="p-2 rounded-xl bg-emerald-950/50 border border-emerald-500/20 text-emerald-400">
            <DollarSign className="w-4 h-4" />
          </div>
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-extrabold text-white font-mono tracking-tight">
            R$ {metricas.faturamentoTotal.toFixed(2)}
          </div>
          <span className="text-[11px] text-surface-400 mt-1 block">Faturamento acumulado</span>
        </div>
      </div>

      {/* Jogos de Hoje */}
      <div className="bg-surface-900/90 border border-surface-800 p-5 sm:p-6 rounded-2xl shadow-xl shadow-black/40 space-y-3 relative overflow-hidden group hover:border-surface-700 transition-all">
        <div className="absolute top-0 right-0 w-28 h-28 bg-brand-500/5 rounded-full blur-2xl group-hover:bg-brand-500/10 transition-all pointer-events-none" />
        <div className="flex items-center justify-between text-surface-400 text-[11px] uppercase tracking-wider font-bold">
          <span>Jogos de Hoje</span>
          <div className="p-2 rounded-xl bg-brand-950/50 border border-brand-500/20 text-brand-400">
            <TrendingUp className="w-4 h-4" />
          </div>
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            {metricas.reservasHoje}
          </div>
          <span className="text-[11px] text-surface-400 mt-1 block">Agendamentos no dia</span>
        </div>
      </div>

      {/* Total de Reservas */}
      <div className="bg-surface-900/90 border border-surface-800 p-5 sm:p-6 rounded-2xl shadow-xl shadow-black/40 space-y-3 relative overflow-hidden group hover:border-surface-700 transition-all">
        <div className="absolute top-0 right-0 w-28 h-28 bg-purple-500/5 rounded-full blur-2xl group-hover:bg-purple-500/10 transition-all pointer-events-none" />
        <div className="flex items-center justify-between text-surface-400 text-[11px] uppercase tracking-wider font-bold">
          <span>Total de Reservas</span>
          <div className="p-2 rounded-xl bg-purple-950/50 border border-purple-500/20 text-purple-400">
            <Users className="w-4 h-4" />
          </div>
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            {metricas.totalReservas}
          </div>
          <span className="text-[11px] text-surface-400 mt-1 block">Histórico geral da arena</span>
        </div>
      </div>

      {/* Quadras Ativas */}
      <div className="bg-surface-900/90 border border-surface-800 p-5 sm:p-6 rounded-2xl shadow-xl shadow-black/40 space-y-3 relative overflow-hidden group hover:border-surface-700 transition-all">
        <div className="absolute top-0 right-0 w-28 h-28 bg-sky-500/5 rounded-full blur-2xl group-hover:bg-sky-500/10 transition-all pointer-events-none" />
        <div className="flex items-center justify-between text-surface-400 text-[11px] uppercase tracking-wider font-bold">
          <span>Quadras Ativas</span>
          <div className="p-2 rounded-xl bg-sky-950/50 border border-sky-500/20 text-sky-400">
            <ShieldCheck className="w-4 h-4" />
          </div>
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            {metricas.quadrasAtivas}{' '}
            <span className="text-sm font-normal text-surface-400 font-mono">/ {metricas.totalQuadras}</span>
          </div>
          <span className="text-[11px] text-surface-400 mt-1 block">Prontas para locação</span>
        </div>
      </div>
    </div>
  );
};
