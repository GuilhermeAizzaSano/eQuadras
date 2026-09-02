import React from 'react';
import { Quadra, BloqueioHorario } from '../../types';
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
        className="fixed inset-0 bg-black/85 backdrop-blur-md transition-opacity animate-in fade-in duration-200"
      />

      <div className="relative w-full max-w-2xl bg-zinc-950 border border-zinc-800 rounded-3xl shadow-2xl overflow-hidden z-10 flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-zinc-850 flex items-center justify-between gap-4 bg-zinc-950">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-amber-950/40 border border-amber-800/40 text-amber-400">
              <Ban className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-bold text-white">
                Bloqueios de Horário e Dias
              </h3>
              <p className="text-xs text-zinc-400">
                Quadra: <strong className="text-zinc-200">{quadra.nome}</strong>
              </p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-full bg-zinc-900 hover:bg-zinc-850 text-zinc-400 hover:text-white border border-zinc-800 transition active:scale-95"
            title="Fechar"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="overflow-y-auto p-5 sm:p-6 space-y-6 scrollbar-thin">
          {/* Formulário Novo Bloqueio */}
          <form onSubmit={onSubmit} className="p-4 rounded-2xl bg-zinc-900/60 border border-zinc-800 space-y-3.5">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-zinc-300 flex items-center gap-2">
              <PlusCircle className="w-4 h-4 text-emerald-400" />
              Novo Bloqueio
            </h4>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-[11px] font-semibold uppercase tracking-wider text-zinc-400 mb-1">
                  Data *
                </label>
                <input
                  type="date"
                  required
                  value={bloqueioData}
                  onChange={(e) => onDataChange(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-white transition [color-scheme:dark]"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold uppercase tracking-wider text-zinc-400 mb-1">
                  Hora Início <span className="text-zinc-500 font-normal">(Opcional)</span>
                </label>
                <input
                  type="time"
                  value={bloqueioHoraInicio}
                  onChange={(e) => onHoraInicioChange(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-white transition [color-scheme:dark]"
                />
              </div>

              <div>
                <label className="block text-[11px] font-semibold uppercase tracking-wider text-zinc-400 mb-1">
                  Hora Fim <span className="text-zinc-500 font-normal">(Opcional)</span>
                </label>
                <input
                  type="time"
                  value={bloqueioHoraFim}
                  onChange={(e) => onHoraFimChange(e.target.value)}
                  className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-white transition [color-scheme:dark]"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold uppercase tracking-wider text-zinc-400 mb-1">
                Motivo do Bloqueio <span className="text-zinc-500 font-normal">(Opcional)</span>
              </label>
              <input
                type="text"
                value={bloqueioMotivo}
                onChange={(e) => onMotivoChange(e.target.value)}
                placeholder="Ex: Manutenção na rede, reforma no piso, evento fechado..."
                className="w-full bg-zinc-950 border border-zinc-800 rounded-xl px-3 py-2 text-xs text-white focus:outline-none focus:border-white transition"
              />
              <p className="text-[10px] text-zinc-500 mt-1">
                Deixe horários em branco para bloquear o dia inteiro.
              </p>
            </div>

            <div className="flex justify-end pt-1">
              <button
                type="submit"
                className="px-4 py-2 rounded-xl text-xs font-bold bg-amber-400 hover:bg-amber-300 text-zinc-950 transition active:scale-95 shadow-md flex items-center gap-1.5"
              >
                <Ban className="w-3.5 h-3.5" />
                <span>Adicionar Bloqueio</span>
              </button>
            </div>
          </form>

          {/* Lista de Bloqueios Existentes */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-zinc-400 flex items-center justify-between">
              <span>Bloqueios Cadastrados ({bloqueios.length})</span>
            </h4>

            {loadingBloqueios ? (
              <div className="py-8 text-center text-zinc-500 text-xs font-mono">
                Carregando bloqueios...
              </div>
            ) : bloqueios.length === 0 ? (
              <div className="py-8 text-center text-zinc-500 text-xs border border-dashed border-zinc-800 rounded-2xl">
                Nenhum bloqueio cadastrado para esta quadra.
              </div>
            ) : (
              <div className="space-y-2">
                {bloqueios.map((b) => {
                  const isDiaInteiro = !b.horaInicio || !b.horaFim;
                  return (
                    <div
                      key={b.id}
                      className="p-3.5 rounded-xl bg-zinc-900/80 border border-zinc-800 flex items-center justify-between gap-3"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-white font-mono">
                            {b.data.split('-').reverse().join('/')}
                          </span>
                          <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-md ${
                            isDiaInteiro ? 'bg-red-950/60 text-red-300 border border-red-800/40' : 'bg-amber-950/60 text-amber-300 border border-amber-800/40'
                          }`}>
                            {isDiaInteiro ? 'Dia Inteiro' : `${b.horaInicio?.slice(0, 5)} às ${b.horaFim?.slice(0, 5)}`}
                          </span>
                        </div>
                        {b.motivo && (
                          <p className="text-xs text-zinc-400">
                            Motivo: <span className="text-zinc-300">{b.motivo}</span>
                          </p>
                        )}
                      </div>

                      <button
                        type="button"
                        onClick={() => onRemoverBloqueio(b.id)}
                        className="px-3 py-1.5 rounded-lg bg-red-950/30 hover:bg-red-950/60 border border-red-900/40 text-red-400 hover:text-red-300 text-xs font-semibold transition active:scale-95"
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
