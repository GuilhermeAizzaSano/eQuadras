import React from 'react';
import { X, Calendar as CalendarIcon, Clock, ChevronRight, Info, ArrowLeft, Check } from 'lucide-react';
import { Quadra, HorarioDisponivel } from '../../types';
import { Badge } from './Badge';
import { EmptyState } from './EmptyState';
import { Button } from './Button';

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
    disponivel?: boolean;
    motivoIndisponibilidade?: 'Encerrado' | 'Bloqueado' | 'Fechado';
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
  React.useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !loading) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, loading, onClose]);

  if (!isOpen || !quadra) return null;

  const ordenados = getSlotsOrdenados();
  const contiguos = isSelecaoContigua();
  const totalPagar = slotsSelecionados.length > 0 ? quadra.valorHora * ordenados.length : 0;
  const inicioStr = ordenados.length > 0 ? ordenados[0].inicio.substring(0, 5) : '';
  const fimStr = ordenados.length > 0 ? ordenados[ordenados.length - 1].fim.substring(0, 5) : '';

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6"
    >
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-2xl transition-opacity animate-in fade-in duration-200"
      />

      {/* Modal Container */}
      <div className="relative w-full max-w-3xl bg-surface-1 border border-fg/[0.1] rounded-3xl overflow-hidden z-10 flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-fg/[0.06] flex items-center justify-between gap-4 bg-surface-1/80 backdrop-blur-xl">
          <div>
            <div className="flex items-center gap-2.5">
              <Badge variant="neutral" className="text-xs">
                {quadra.tipoEsporte.replace('_', ' ')}
              </Badge>
              <button
                type="button"
                onClick={onOpenDetails}
                className="text-xs text-fg/70 hover:text-fg font-medium flex items-center gap-1.5 transition cursor-pointer"
              >
                <Info className="w-3.5 h-3.5" />
                <span>Ver fotos & detalhes</span>
              </button>
            </div>
            <h2 className="text-xl sm:text-2xl font-semibold text-fg tracking-tight mt-1.5 flex items-center gap-2">
              <CalendarIcon className="w-5 h-5 text-fg/70" />
              <span>{quadra.nome}</span>
            </h2>
          </div>

          <button
            onClick={onClose}
            aria-label="Fechar modal de agendamento"
            className="p-2 rounded-full bg-fg/[0.06] hover:bg-fg/[0.15] text-fg/60 hover:text-fg border border-fg/[0.1] transition active:scale-95 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="overflow-y-auto p-5 sm:p-6 space-y-6 scrollbar-thin">
          {/* Seletor de Data */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold uppercase tracking-wider text-fg/60">
                Selecione o Dia
              </label>
              <span className="text-xs text-fg/60 font-mono font-medium">
                Próximos 14 dias
              </span>
            </div>

            {/* Fita de Dias */}
            <div className="flex gap-2 overflow-x-auto pb-2 scrollbar-thin">
              {diasDisponiveis.map((d) => {
                const isSelected = dataSelecionada === d.iso;
                const isDisponivel = d.disponivel !== false;
                const badgeText = d.motivoIndisponibilidade || 'Fechado';

                return (
                  <button
                    key={d.iso}
                    type="button"
                    disabled={!isDisponivel}
                    onClick={() => {
                      if (isDisponivel) {
                        setDataSelecionada(d.iso);
                      }
                    }}
                    title={
                      !isDisponivel
                        ? badgeText === 'Encerrado'
                          ? 'Data limite de agendamento encerrada'
                          : badgeText === 'Bloqueado'
                          ? 'Dia bloqueado pelo administrador'
                          : 'Quadra fechada neste dia'
                        : undefined
                    }
                    className={`shrink-0 w-16 py-3 px-1 rounded-2xl border flex flex-col items-center justify-center transition-all cursor-pointer ${
                      !isDisponivel
                        ? 'opacity-30 cursor-not-allowed bg-transparent border-fg/[0.03] text-fg/60 select-none'
                        : isSelected
                        ? 'bg-fg text-on-accent border-fg shadow-sm font-semibold active:scale-[0.98]'
                        : 'bg-fg/[0.03] border-fg/[0.1] text-fg/70 hover:border-fg/[0.15] hover:bg-fg/[0.1] active:scale-[0.98]'
                    }`}
                  >
                    <span className="text-xs uppercase tracking-wider mb-1 font-semibold">
                      {d.isHoje ? 'Hoje' : d.diaSemana}
                    </span>
                    <span className="text-base font-bold">{d.diaMes}</span>
                    <span className="text-xs mt-0.5 opacity-70">{d.mes}</span>
                    {!isDisponivel && (
                      <span
                        className={`text-[8px] uppercase tracking-wider font-bold mt-1 px-1 rounded ${
                          badgeText === 'Encerrado'
                            ? 'text-warning bg-warning/10'
                            : badgeText === 'Bloqueado'
                            ? 'text-danger bg-danger/10'
                            : 'text-fg/60'
                        }`}
                      >
                        {badgeText}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Grade de Slots de Horários */}
          <div className="space-y-3.5 pt-4 border-t border-fg/[0.1]">
            <div className="flex items-center justify-between">
              <label className="text-xs font-semibold uppercase tracking-wider text-fg/60">
                Horários ({dataSelecionada.split('-').reverse().join('/')})
              </label>
              <div className="flex items-center gap-3 text-xs text-fg/60 font-medium">
                <span className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-success" />
                  <span>Disponível</span>
                </span>
                <span className="flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-fg/20" />
                  <span>Ocupado</span>
                </span>
              </div>
            </div>

            {loading ? (
              <div className="py-14 text-center text-fg/60 text-xs font-mono">
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
                      className={`p-3.5 rounded-2xl border text-left flex flex-col justify-between transition-all min-h-[76px] cursor-pointer active:scale-[0.98] ${
                        !slot.disponivel
                          ? 'bg-transparent border-fg/[0.03] opacity-30 cursor-not-allowed text-fg/60'
                          : isSelected
                          ? 'bg-fg text-on-accent border-fg shadow-sm'
                          : 'bg-fg/[0.03] border-fg/[0.1] text-fg/90 hover:border-fg/[0.15] hover:bg-fg/[0.1]'
                      }`}
                    >
                      <div className="flex items-center justify-between w-full">
                        <span className="text-xs font-semibold font-mono tracking-tight">
                          {horaInicio} - {horaFim}
                        </span>
                        {isSelected && <Check className="w-3.5 h-3.5 text-on-accent stroke-[3]" />}
                      </div>
                      <div
                        className={`text-xs mt-2 font-mono uppercase tracking-wider font-semibold ${
                          isSelected
                            ? 'text-on-accent/70'
                            : slot.disponivel
                            ? 'text-success'
                            : 'text-fg/60'
                        }`}
                      >
                        {isSelected ? 'Selecionado' : slot.motivo}
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Rodapé / Resumo & Botão Pagar */}
        <div className="p-4 sm:p-5 bg-surface-1 border-t border-fg/[0.1] flex flex-col sm:flex-row items-center justify-between gap-4">
          {slotsSelecionados.length > 0 ? (
            <div className="w-full flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={onOpenDetails}
                  leftIcon={<ArrowLeft className="w-3.5 h-3.5" />}
                  title="Voltar para os detalhes e fotos da quadra"
                >
                  Infos
                </Button>

                <div className="text-center sm:text-left">
                  {contiguos ? (
                    <>
                      <div className="text-xs text-fg/60">
                        Horário: <strong className="text-fg font-medium">{inicioStr} às {fimStr}</strong> ({ordenados.length}h)
                      </div>
                      <div className="text-base font-bold text-fg font-mono mt-0.5 tracking-tight">
                        Total: R$ {totalPagar.toFixed(2)}
                      </div>
                    </>
                  ) : (
                    <div className="text-xs font-semibold text-danger">
                      Selecione apenas horários consecutivos
                    </div>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2.5 w-full sm:w-auto">
                <Button
                  type="button"
                  variant="outline"
                  size="md"
                  onClick={onClose}
                >
                  Cancelar
                </Button>
                <Button
                  type="button"
                  variant="primary"
                  size="md"
                  onClick={onConfirmar}
                  disabled={!contiguos || loading}
                  rightIcon={<ChevronRight className="w-4 h-4" />}
                >
                  Pagar com Pix
                </Button>
              </div>
            </div>
          ) : (
            <div className="w-full flex items-center justify-between gap-3">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={onOpenDetails}
                leftIcon={<ArrowLeft className="w-3.5 h-3.5" />}
              >
                Infos da Quadra
              </Button>

              <div className="text-xs text-fg/60">
                Selecione um ou mais horários acima para reservar
              </div>

              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={onClose}
              >
                Fechar
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
