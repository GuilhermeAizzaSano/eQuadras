import React from 'react';
import { X, Calendar as CalendarIcon, Clock, ChevronRight, Info, ArrowLeft } from 'lucide-react';
import { Quadra, HorarioDisponivel } from '../../types';
import { Badge } from './Badge';
import { EmptyState } from './EmptyState';

interface BookingModalProps {
  isOpen: boolean;
  quadra: Quadra | null;
  dataSelecionada: string;
  setDataSelecionada: (data: string) => void;
  diasDisponiveis: Array<{
    iso: string;
    diaSemana: string;
    diaMes: number;
    mes: string;
    isHoje: boolean;
  }>;
  horarios: HorarioDisponivel[];
  slotsSelecionados: HorarioDisponivel[];
  toggleSlotSelection: (slot: HorarioDisponivel) => void;
  getSlotsOrdenados: () => HorarioDisponivel[];
  isSelecaoContigua: () => boolean;
  loading: boolean;
  onConfirmar: () => void;
  onClose: () => void;
  onOpenDetails: () => void;
}

export const BookingModal: React.FC<BookingModalProps> = ({
  isOpen,
  quadra,
  dataSelecionada,
  setDataSelecionada,
  diasDisponiveis,
  horarios,
  slotsSelecionados,
  toggleSlotSelection,
  getSlotsOrdenados,
  isSelecaoContigua,
  loading,
  onConfirmar,
  onClose,
  onOpenDetails,
}) => {
  if (!isOpen || !quadra) return null;

  const ordenados = getSlotsOrdenados();
  const contiguos = isSelecaoContigua();
  const totalPagar = slotsSelecionados.length > 0 ? quadra.valorHora * ordenados.length : 0;
  const inicioStr = ordenados.length > 0 ? ordenados[0].inicio.substring(0, 5) : '';
  const fimStr = ordenados.length > 0 ? ordenados[ordenados.length - 1].fim.substring(0, 5) : '';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/85 backdrop-blur-md transition-opacity animate-in fade-in duration-200"
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-3xl bg-zinc-950 border border-zinc-800 rounded-3xl shadow-2xl overflow-hidden z-10 flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200">
        
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-zinc-850 flex items-center justify-between gap-4 bg-zinc-950">
          <div>
            <div className="flex items-center gap-2">
              <Badge variant="neutral" className="text-[10px]">
                {quadra.tipoEsporte.replace('_', ' ')}
              </Badge>
              <button
                type="button"
                onClick={onOpenDetails}
                className="text-xs text-emerald-400 hover:text-emerald-300 font-medium flex items-center gap-1 transition"
              >
                <Info className="w-3.5 h-3.5" />
                <span>Ver fotos & detalhes</span>
              </button>
            </div>
            <h2 className="text-xl sm:text-2xl font-bold text-white tracking-tight mt-1 flex items-center gap-2">
              <CalendarIcon className="w-5 h-5 text-emerald-400" />
              {quadra.nome}
            </h2>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-full bg-zinc-900 hover:bg-zinc-850 text-zinc-400 hover:text-white border border-zinc-800 transition active:scale-95"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="overflow-y-auto p-5 sm:p-6 space-y-6 scrollbar-thin">
          
          {/* Seletor de Data */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                Selecione o Dia
              </label>
              <span className="text-xs text-zinc-500 font-mono">
                Próximos 14 dias
              </span>
            </div>

            {/* Carrossel de Dias */}
            <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-thin">
              {diasDisponiveis.map((d) => {
                const isSelected = dataSelecionada === d.iso;
                return (
                  <button
                    key={d.iso}
                    onClick={() => setDataSelecionada(d.iso)}
                    className={`flex-shrink-0 w-16 py-3 px-1 rounded-xl border flex flex-col items-center justify-center transition-all active:scale-[0.98] ${
                      isSelected
                        ? 'bg-white text-zinc-950 border-white shadow-lg font-bold'
                        : 'bg-zinc-900/60 border-zinc-850 text-zinc-400 hover:border-zinc-700 hover:text-white'
                    }`}
                  >
                    <span className="text-[10px] uppercase tracking-wider mb-1 font-semibold">
                      {d.isHoje ? 'Hoje' : d.diaSemana}
                    </span>
                    <span className="text-base font-extrabold">{d.diaMes}</span>
                    <span className="text-[11px] mt-0.5">{d.mes}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Grade de Slots de Horários */}
          <div className="space-y-3.5 pt-4 border-t border-zinc-850">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold uppercase tracking-wider text-zinc-400">
                Horários ({dataSelecionada.split('-').reverse().join('/')})
              </label>
              <div className="flex items-center gap-3 text-xs text-zinc-500 font-medium">
                <span className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-400"></span> Disponível
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-zinc-700"></span> Ocupado
                </span>
              </div>
            </div>

            {loading ? (
              <div className="py-14 text-center text-zinc-500 text-xs font-mono">
                Carregando grade de horários...
              </div>
            ) : horarios.length === 0 ? (
              <EmptyState
                icon={Clock}
                title="Nenhum horário disponível"
                description="Não há horários disponíveis para esta data selecionada."
                className="py-12"
              />
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2.5">
                {horarios.map((slot, idx) => {
                  const horaInicio = slot.inicio.substring(0, 5);
                  const horaFim = slot.fim.substring(0, 5);
                  const isSelected = slotsSelecionados.some((s) => s.inicio === slot.inicio);

                  return (
                    <button
                      key={idx}
                      disabled={!slot.disponivel}
                      onClick={() => toggleSlotSelection(slot)}
                      className={`p-3.5 rounded-xl border text-left flex flex-col justify-between transition-all min-h-[72px] active:scale-[0.98] ${
                        !slot.disponivel
                          ? 'bg-zinc-950/40 border-zinc-900 opacity-30 cursor-not-allowed text-zinc-600'
                          : isSelected
                          ? 'bg-white text-zinc-950 border-white shadow-xl ring-1 ring-white'
                          : 'bg-zinc-900/60 border-zinc-850 text-zinc-200 hover:border-zinc-600 hover:bg-zinc-900'
                      }`}
                    >
                      <div className="text-xs font-bold font-mono">
                        {horaInicio} - {horaFim}
                      </div>
                      <div
                        className={`text-[11px] mt-2 font-mono uppercase tracking-wider ${
                          isSelected
                            ? 'text-zinc-900 font-bold'
                            : slot.disponivel
                            ? 'text-emerald-400 font-semibold'
                            : 'text-zinc-600'
                        }`}
                      >
                        {slot.motivo}
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Rodapé / Resumo & Botão Pagar */}
        <div className="p-4 sm:p-5 bg-zinc-950 border-t border-zinc-850 flex flex-col sm:flex-row items-center justify-between gap-4">
          {slotsSelecionados.length > 0 ? (
            <div className="w-full flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={onOpenDetails}
                  className="py-2.5 px-3 rounded-xl border border-zinc-800 text-xs font-semibold text-zinc-300 hover:text-white hover:bg-zinc-900 transition flex items-center gap-1.5 shrink-0 active:scale-[0.98]"
                  title="Voltar para os detalhes e fotos da quadra"
                >
                  <ArrowLeft className="w-3.5 h-3.5" />
                  <span>Infos da Quadra</span>
                </button>

                <div className="text-center sm:text-left">
                  {contiguos ? (
                    <>
                      <div className="text-xs text-zinc-400">
                        Horário: <strong className="text-white">{inicioStr} às {fimStr}</strong> ({ordenados.length}h)
                      </div>
                      <div className="text-sm font-bold text-emerald-400 font-mono mt-0.5">
                        Total: R$ {totalPagar.toFixed(2)}
                      </div>
                    </>
                  ) : (
                    <div className="text-xs font-bold text-red-400">
                      Selecione apenas horários consecutivos
                    </div>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2 w-full sm:w-auto">
                <button
                  type="button"
                  onClick={onClose}
                  className="py-2.5 px-4 rounded-xl border border-zinc-800 text-xs font-semibold text-zinc-400 hover:text-white hover:bg-zinc-900 transition active:scale-[0.98]"
                >
                  Cancelar
                </button>
                <button
                  onClick={onConfirmar}
                  disabled={!contiguos || loading}
                  className="flex-1 sm:flex-none flex items-center justify-center gap-2 px-6 py-2.5 rounded-xl bg-emerald-400 hover:bg-emerald-300 text-zinc-950 text-xs font-bold transition shadow-lg shadow-emerald-950/20 disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98]"
                >
                  <span>Pagar e Agendar com Pix</span>
                  <ChevronRight className="w-4 h-4 text-zinc-950" />
                </button>
              </div>
            </div>
          ) : (
            <div className="w-full flex items-center justify-between gap-3">
              <button
                type="button"
                onClick={onOpenDetails}
                className="py-2 px-3 rounded-xl border border-zinc-800 text-xs font-semibold text-zinc-300 hover:text-white hover:bg-zinc-900 transition flex items-center gap-1.5 shrink-0 active:scale-[0.98]"
                title="Voltar para os detalhes e fotos da quadra"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Infos da Quadra</span>
              </button>

              <div className="flex items-center gap-2">
                <span className="hidden sm:inline text-xs text-zinc-500">
                  Selecione um ou mais horários disponíveis.
                </span>
                <button
                  type="button"
                  onClick={onClose}
                  className="py-2 px-4 rounded-xl border border-zinc-800 text-xs font-semibold text-zinc-400 hover:text-white hover:bg-zinc-900 transition active:scale-[0.98]"
                >
                  Fechar
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
