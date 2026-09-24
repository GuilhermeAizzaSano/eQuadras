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
      <div className="bg-fg/[0.03] border border-fg/[0.06] hover:border-fg/[0.15] p-5 sm:p-6 rounded-2xl space-y-3 transition-all duration-200 group">
        <div className="flex items-center justify-between text-fg/50 text-[11px] uppercase tracking-wider font-medium">
          <span>Receita Total</span>
          <DollarSign className="w-4 h-4 text-fg/60" />
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-bold text-fg font-mono tracking-tight">
            R$ {metricas.faturamentoTotal.toFixed(2)}
          </div>
          <span className="text-[11px] text-fg/40 mt-1 block tracking-tight">Faturamento acumulado</span>
        </div>
      </div>

      {/* Jogos de Hoje */}
      <div className="bg-fg/[0.03] border border-fg/[0.06] hover:border-fg/[0.15] p-5 sm:p-6 rounded-2xl space-y-3 transition-all duration-200 group">
        <div className="flex items-center justify-between text-fg/50 text-[11px] uppercase tracking-wider font-medium">
          <span>Jogos de Hoje</span>
          <TrendingUp className="w-4 h-4 text-fg/60" />
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-bold text-fg tracking-[-0.03em]">
            {metricas.reservasHoje}
          </div>
          <span className="text-[11px] text-fg/40 mt-1 block tracking-tight">Agendamentos no dia</span>
        </div>
      </div>

      {/* Total de Reservas */}
      <div className="bg-fg/[0.03] border border-fg/[0.06] hover:border-fg/[0.15] p-5 sm:p-6 rounded-2xl space-y-3 transition-all duration-200 group">
        <div className="flex items-center justify-between text-fg/50 text-[11px] uppercase tracking-wider font-medium">
          <span>Total de Reservas</span>
          <Users className="w-4 h-4 text-fg/60" />
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-bold text-fg tracking-[-0.03em]">
            {metricas.totalReservas}
          </div>
          <span className="text-[11px] text-fg/40 mt-1 block tracking-tight">Histórico geral da arena</span>
        </div>
      </div>

      {/* Quadras Ativas */}
      <div className="bg-fg/[0.03] border border-fg/[0.06] hover:border-fg/[0.15] p-5 sm:p-6 rounded-2xl space-y-3 transition-all duration-200 group">
        <div className="flex items-center justify-between text-fg/50 text-[11px] uppercase tracking-wider font-medium">
          <span>Quadras Ativas</span>
          <ShieldCheck className="w-4 h-4 text-fg/60" />
        </div>
        <div>
          <div className="text-2xl sm:text-3xl font-bold text-fg tracking-[-0.03em]">
            {metricas.quadrasAtivas}
            <span className="text-sm font-normal text-fg/40 ml-1.5 font-mono">
              / {metricas.totalQuadras}
            </span>
          </div>
          <span className="text-[11px] text-fg/40 mt-1 block tracking-tight">Prontas para locação</span>
        </div>
      </div>
    </div>
  );
};
