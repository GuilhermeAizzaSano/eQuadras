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
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div className="bg-zinc-950 border border-zinc-850 p-5 rounded-2xl shadow-xl space-y-2">
        <div className="flex items-center justify-between text-zinc-400 text-[11px] uppercase tracking-wider font-semibold">
          <span>Receita Acumulada</span>
          <DollarSign className="w-4 h-4 text-emerald-400" />
        </div>
        <div className="text-2xl font-extrabold text-white font-mono">
          R$ {metricas.faturamentoTotal.toFixed(2)}
        </div>
      </div>

      <div className="bg-zinc-950 border border-zinc-850 p-5 rounded-2xl shadow-xl space-y-2">
        <div className="flex items-center justify-between text-zinc-400 text-[11px] uppercase tracking-wider font-semibold">
          <span>Jogos de Hoje</span>
          <TrendingUp className="w-4 h-4 text-emerald-400" />
        </div>
        <div className="text-2xl font-extrabold text-white">
          {metricas.reservasHoje}
        </div>
      </div>

      <div className="bg-zinc-950 border border-zinc-850 p-5 rounded-2xl shadow-xl space-y-2">
        <div className="flex items-center justify-between text-zinc-400 text-[11px] uppercase tracking-wider font-semibold">
          <span>Total de Reservas</span>
          <Users className="w-4 h-4 text-zinc-400" />
        </div>
        <div className="text-2xl font-extrabold text-white">
          {metricas.totalReservas}
        </div>
      </div>

      <div className="bg-zinc-950 border border-zinc-850 p-5 rounded-2xl shadow-xl space-y-2">
        <div className="flex items-center justify-between text-zinc-400 text-[11px] uppercase tracking-wider font-semibold">
          <span>Quadras Ativas</span>
          <ShieldCheck className="w-4 h-4 text-zinc-300" />
        </div>
        <div className="text-2xl font-extrabold text-white">
          {metricas.quadrasAtivas} <span className="text-xs font-normal text-zinc-500">/ {metricas.totalQuadras}</span>
        </div>
      </div>
    </div>
  );
};
