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
      <div className="bg-[#121214] border border-white/[0.08] hover:border-white/[0.18] p-5 sm:p-6 rounded-2xl sm:rounded-3xl shadow-apple-card hover:shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.05)] space-y-3 transition-all duration-200 group">
        <div className="flex items-center justify-between text-white/50 text-[11px] uppercase tracking-wider font-semibold">
          <span>Receita Total</span>
          <div className="p-2 rounded-xl bg-white/[0.06] border border-white/[0.1] text-white/80 group-hover:text-white transition-colors">
            <DollarSign className="w-4 h-4" />
          </div>
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-bold text-white font-mono tracking-tight">
            R$ {metricas.faturamentoTotal.toFixed(2)}
          </div>
          <span className="text-[11px] text-white/40 mt-1 block tracking-tight">Faturamento acumulado</span>
        </div>
      </div>

      {/* Jogos de Hoje */}
      <div className="bg-[#121214] border border-white/[0.08] hover:border-white/[0.18] p-5 sm:p-6 rounded-2xl sm:rounded-3xl shadow-apple-card hover:shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.05)] space-y-3 transition-all duration-200 group">
        <div className="flex items-center justify-between text-white/50 text-[11px] uppercase tracking-wider font-semibold">
          <span>Jogos de Hoje</span>
          <div className="p-2 rounded-xl bg-white/[0.06] border border-white/[0.1] text-white/80 group-hover:text-white transition-colors">
            <TrendingUp className="w-4 h-4" />
          </div>
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-bold text-white tracking-[-0.03em]">
            {metricas.reservasHoje}
          </div>
          <span className="text-[11px] text-white/40 mt-1 block tracking-tight">Agendamentos no dia</span>
        </div>
      </div>

      {/* Total de Reservas */}
      <div className="bg-[#121214] border border-white/[0.08] hover:border-white/[0.18] p-5 sm:p-6 rounded-2xl sm:rounded-3xl shadow-apple-card hover:shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.05)] space-y-3 transition-all duration-200 group">
        <div className="flex items-center justify-between text-white/50 text-[11px] uppercase tracking-wider font-semibold">
          <span>Total de Reservas</span>
          <div className="p-2 rounded-xl bg-white/[0.06] border border-white/[0.1] text-white/80 group-hover:text-white transition-colors">
            <Users className="w-4 h-4" />
          </div>
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-bold text-white tracking-[-0.03em]">
            {metricas.totalReservas}
          </div>
          <span className="text-[11px] text-white/40 mt-1 block tracking-tight">Histórico geral da arena</span>
        </div>
      </div>

      {/* Quadras Ativas */}
      <div className="bg-[#121214] border border-white/[0.08] hover:border-white/[0.18] p-5 sm:p-6 rounded-2xl sm:rounded-3xl shadow-apple-card hover:shadow-apple-elevated shadow-[inset_0_1px_0_0_rgba(255,255,255,0.05)] space-y-3 transition-all duration-200 group">
        <div className="flex items-center justify-between text-white/50 text-[11px] uppercase tracking-wider font-semibold">
          <span>Quadras Ativas</span>
          <div className="p-2 rounded-xl bg-white/[0.06] border border-white/[0.1] text-white/80 group-hover:text-white transition-colors">
            <ShieldCheck className="w-4 h-4" />
          </div>
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-bold text-white tracking-[-0.03em]">
            {metricas.quadrasAtivas}{' '}
            <span className="text-sm font-normal text-white/40 font-mono">/ {metricas.totalQuadras}</span>
          </div>
          <span className="text-[11px] text-white/40 mt-1 block tracking-tight">Prontas para locação</span>
        </div>
      </div>
    </div>
  );
};
