import React from 'react';
import { Quadra, BloqueioHorario } from '../../types';
import { Button, Input } from '../ui';
import { PlusCircle, Ban, X } from 'lucide-react';

interface CourtBlockModalProps {
  quadra: Quadra | null;
  bloqueios: BloqueioHorario[];
  loadingBloqueios: boolean;
  bloqueioData: string;
  bloqueioHoraInicio: string;
  bloqueioHoraFim: string;
  bloqueioMotivo: string;
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
  onClose,
  onDataChange,
  onHoraInicioChange,
  onHoraFimChange,
  onMotivoChange,
  onSubmit,
  onRemoverBloqueio,
}) => {
  if (!quadra) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/60 backdrop-blur-2xl transition-opacity animate-in fade-in duration-200"
      />

      <div className="relative w-full max-w-2xl bg-[#121214] border border-white/[0.1] rounded-2xl sm:rounded-3xl shadow-2xl shadow-black/80 overflow-hidden z-10 flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-white/[0.06] flex items-center justify-between gap-4 bg-white/[0.02]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#FF9F0A]/10 border border-[#FF9F0A]/20 text-[#FF9F0A] flex items-center justify-center">
              <Ban className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-semibold text-white tracking-tight">
                Bloqueios de Horário e Dias
              </h3>
              <p className="text-xs text-white/50">
                Quadra: <strong className="text-white/80">{quadra.nome}</strong>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-white/50 hover:text-white border border-white/[0.08] transition active:scale-95 cursor-pointer"
            title="Fechar"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Content */}
        <div className="overflow-y-auto p-5 sm:p-6 space-y-6 scrollbar-thin">
          {/* Formulário Novo Bloqueio */}
          <form onSubmit={onSubmit} className="p-5 rounded-2xl bg-white/[0.02] border border-white/[0.06] space-y-4">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-white/80 flex items-center gap-2 font-mono">
              <PlusCircle className="w-4 h-4 text-white/60" />
              Novo Bloqueio
            </h4>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-[11px] font-medium uppercase tracking-wider text-white/60 mb-1.5 font-mono">
                  Data *
                </label>
                <div className="relative">
                  <input
                    type="date"
                    required
                    value={bloqueioData}
                    onChange={(e) => onDataChange(e.target.value)}
                    className="w-full bg-[#1c1c1e] border border-white/[0.08] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-white/30 transition font-mono [color-scheme:dark]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-medium uppercase tracking-wider text-white/60 mb-1.5 font-mono">
                  Hora Início <span className="text-white/40 font-normal font-sans">(Opcional)</span>
                </label>
                <input
                  type="time"
                  value={bloqueioHoraInicio}
                  onChange={(e) => onHoraInicioChange(e.target.value)}
                  className="w-full bg-[#1c1c1e] border border-white/[0.08] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-white/30 transition font-mono [color-scheme:dark]"
                />
              </div>

              <div>
                <label className="block text-[11px] font-medium uppercase tracking-wider text-white/60 mb-1.5 font-mono">
                  Hora Fim <span className="text-white/40 font-normal font-sans">(Opcional)</span>
                </label>
                <input
                  type="time"
                  value={bloqueioHoraFim}
                  onChange={(e) => onHoraFimChange(e.target.value)}
                  className="w-full bg-[#1c1c1e] border border-white/[0.08] rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-white/30 transition font-mono [color-scheme:dark]"
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
                className="cursor-pointer"
              >
                <Ban className="w-3.5 h-3.5" />
                <span>Adicionar Bloqueio</span>
              </Button>
            </div>
          </form>

          {/* Lista de Bloqueios Existentes */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-white/40 flex items-center justify-between font-mono">
              <span>Bloqueios Cadastrados ({bloqueios.length})</span>
            </h4>

            {loadingBloqueios ? (
              <div className="py-8 text-center text-white/40 text-xs font-mono">
                Carregando bloqueios...
              </div>
            ) : bloqueios.length === 0 ? (
              <div className="py-8 text-center text-white/40 text-xs border border-dashed border-white/[0.08] rounded-2xl bg-white/[0.01]">
                Nenhum bloqueio cadastrado para esta quadra.
              </div>
            ) : (
              <div className="space-y-2">
                {bloqueios.map((b) => {
                  const isDiaInteiro = !b.horaInicio || !b.horaFim;
                  return (
                    <div
                      key={b.id}
                      className="p-3.5 rounded-2xl bg-white/[0.02] border border-white/[0.06] flex items-center justify-between gap-3 hover:border-white/20 transition"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-semibold text-white font-mono">
                            {b.data.split('-').reverse().join('/')}
                          </span>
                          <span className={`text-[10px] font-medium px-2 py-0.5 rounded-full font-mono ${
                            isDiaInteiro ? 'bg-[#FF453A]/15 text-[#FF453A] border border-[#FF453A]/30' : 'bg-[#FF9F0A]/15 text-[#FF9F0A] border border-[#FF9F0A]/30'
                          }`}>
                            {isDiaInteiro ? 'Dia Inteiro' : `${b.horaInicio?.slice(0, 5)} às ${b.horaFim?.slice(0, 5)}`}
                          </span>
                        </div>
                        {b.motivo && (
                          <p className="text-xs text-white/50">
                            Motivo: <span className="text-white/80">{b.motivo}</span>
                          </p>
                        )}
                      </div>

                      <button
                        type="button"
                        onClick={() => onRemoverBloqueio(b.id)}
                        className="px-3 py-1.5 rounded-xl bg-[#FF453A]/10 hover:bg-[#FF453A]/20 border border-[#FF453A]/20 text-[#FF453A] text-xs font-medium transition active:scale-95 cursor-pointer font-mono"
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
