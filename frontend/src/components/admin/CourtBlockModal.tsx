import React from 'react';
import { Quadra, BloqueioHorario } from '../../types';
import { Button, Input } from '../ui';
import { PlusCircle, Ban, X, Loader2 } from 'lucide-react';

interface CourtBlockModalProps {
  quadra: Quadra | null;
  bloqueios: BloqueioHorario[];
  loadingBloqueios: boolean;
  bloqueioData: string;
  bloqueioHoraInicio: string;
  bloqueioHoraFim: string;
  bloqueioMotivo: string;
  isSubmitting?: boolean;
  onClose: () => void;
  onDataChange: (val: string) => void;
  onHoraInicioChange: (val: string) => void;
  onHoraFimChange: (val: string) => void;
  onMotivoChange: (val: string) => void;
  onSubmit: (e: React.FormEvent) => void;
  onRemoverBloqueio: (bloqueioId: number) => void;
}

export const CourtBlockModal: React.FC<CourtBlockModalProps> = ({
  quadra,
  bloqueios,
  loadingBloqueios,
  bloqueioData,
  bloqueioHoraInicio,
  bloqueioHoraFim,
  bloqueioMotivo,
  isSubmitting = false,
  onClose,
  onDataChange,
  onHoraInicioChange,
  onHoraFimChange,
  onMotivoChange,
  onSubmit,
  onRemoverBloqueio,
}) => {
  React.useEffect(() => {
    if (!quadra) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isSubmitting) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [quadra, isSubmitting, onClose]);

  if (!quadra) return null;

  return (
    <div role="dialog" aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-2xl transition-opacity animate-in fade-in duration-200"
      />

      <div className="relative w-full max-w-2xl bg-surface-2 border border-fg/[0.1] rounded-2xl sm:rounded-3xl shadow-2xl shadow-black/80 overflow-hidden z-10 flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-fg/[0.06] flex items-center justify-between gap-4 bg-fg/[0.03]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-warning/10 border border-warning/20 text-warning flex items-center justify-center">
              <Ban className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-semibold text-fg tracking-tight">
                Bloqueios de Horário e Dias
              </h3>
              <p className="text-xs text-fg/60">
                Quadra: <strong className="text-fg/80">{quadra.nome}</strong>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-fg/[0.03] hover:bg-fg/[0.1] text-fg/60 hover:text-fg border border-fg/[0.1] transition active:scale-95 cursor-pointer"
            title="Fechar" aria-label="Fechar"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="overflow-y-auto p-5 sm:p-6 space-y-6 scrollbar-thin">
          {/* Formulário Novo Bloqueio */}
          <form onSubmit={onSubmit} className="p-5 rounded-2xl bg-fg/[0.03] border border-fg/[0.06] space-y-4">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-fg/80 flex items-center gap-2 font-mono">
              <PlusCircle className="w-4 h-4 text-fg/60" />
              Novo Bloqueio
            </h4>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-medium uppercase tracking-wider text-fg/60 mb-1.5 font-mono">
                  Data *
                </label>
                <div className="relative">
                  <input
                    type="date"
                    required
                    value={bloqueioData}
                    onChange={(e) => onDataChange(e.target.value)}
                    className="w-full bg-surface-3 border border-fg/[0.1] rounded-xl px-3 py-2 text-xs text-fg focus:outline-none focus:ring-2 focus:ring-fg/20 focus:border-fg/30 transition font-mono [color-scheme:dark]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium uppercase tracking-wider text-fg/60 mb-1.5 font-mono">
                  Hora Início <span className="text-fg/60 font-normal font-sans">(Opcional)</span>
                </label>
                <input
                  type="time"
                  value={bloqueioHoraInicio}
                  onChange={(e) => onHoraInicioChange(e.target.value)}
                  className="w-full bg-surface-3 border border-fg/[0.1] rounded-xl px-3 py-2 text-xs text-fg focus:outline-none focus:ring-2 focus:ring-fg/20 focus:border-fg/30 transition font-mono [color-scheme:dark]"
                />
              </div>

              <div>
                <label className="block text-xs font-medium uppercase tracking-wider text-fg/60 mb-1.5 font-mono">
                  Hora Fim <span className="text-fg/60 font-normal font-sans">(Opcional)</span>
                </label>
                <input
                  type="time"
                  value={bloqueioHoraFim}
                  onChange={(e) => onHoraFimChange(e.target.value)}
                  className="w-full bg-surface-3 border border-fg/[0.1] rounded-xl px-3 py-2 text-xs text-fg focus:outline-none focus:ring-2 focus:ring-fg/20 focus:border-fg/30 transition font-mono [color-scheme:dark]"
                />
              </div>
            </div>

            <div>
              <Input
                label="Motivo do Bloqueio"
                value={bloqueioMotivo}
                onChange={(e) => onMotivoChange(e.target.value)}
                placeholder="Ex: Manutenção na rede, reforma no piso, evento fechado..."
                hint="Deixe horários em branco para bloquear o dia inteiro."
              />
            </div>

            <div className="flex justify-end pt-1">
              <Button
                type="submit"
                variant="primary"
                disabled={isSubmitting}
                className="cursor-pointer"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Adicionando...</span>
                  </>
                ) : (
                  <>
                    <Ban className="w-3.5 h-3.5" />
                    <span>Adicionar Bloqueio</span>
                  </>
                )}
              </Button>
            </div>
          </form>

          {/* Lista de Bloqueios Existentes */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-fg/60 flex items-center justify-between font-mono">
              <span>Bloqueios Cadastrados ({bloqueios.length})</span>
            </h4>

            {loadingBloqueios ? (
              <div className="py-8 text-center text-fg/60 text-xs font-mono">
                Carregando bloqueios...
              </div>
            ) : bloqueios.length === 0 ? (
              <div className="py-8 text-center text-fg/60 text-xs border border-dashed border-fg/[0.1] rounded-2xl bg-fg/[0.03]">
                Nenhum bloqueio cadastrado para esta quadra.
              </div>
            ) : (
              <div className="space-y-2">
                {bloqueios.map((b) => {
                  const isDiaInteiro = !b.horaInicio || !b.horaFim;
                  return (
                    <div
                      key={b.id}
                      className="p-3.5 rounded-2xl bg-fg/[0.03] border border-fg/[0.06] flex items-center justify-between gap-3 hover:border-fg/20 transition"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-semibold text-fg font-mono">
                            {b.data.split('-').reverse().join('/')}
                          </span>
                          <span className={`text-xs font-medium px-2 py-0.5 rounded-full font-mono ${
                            isDiaInteiro ? 'bg-danger/15 text-danger border border-danger/30' : 'bg-warning/15 text-warning border border-warning/30'
                          }`}>
                            {isDiaInteiro ? 'Dia Inteiro' : `${b.horaInicio?.slice(0, 5)} às ${b.horaFim?.slice(0, 5)}`}
                          </span>
                        </div>
                        {b.motivo && (
                          <p className="text-xs text-fg/60">
                            Motivo: <span className="text-fg/80">{b.motivo}</span>
                          </p>
                        )}
                      </div>

                      <button
                        type="button"
                        onClick={() => onRemoverBloqueio(b.id)}
                        className="px-3 py-1.5 rounded-xl bg-danger/10 hover:bg-danger/20 border border-danger/20 text-danger text-xs font-medium transition active:scale-95 cursor-pointer font-mono"
                      >
                        Desbloquear
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
