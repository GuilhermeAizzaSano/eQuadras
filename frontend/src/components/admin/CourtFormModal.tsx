import React from 'react';
import { TipoEsporte, DiaSemana } from '../../types';
import { Edit2, PlusCircle, X, Clock, Upload, Trash2 } from 'lucide-react';

export const DIAS_SEMANA: { key: DiaSemana; label: string }[] = [
  { key: 'MONDAY', label: 'Segunda-feira' },
  { key: 'TUESDAY', label: 'Terça-feira' },
  { key: 'WEDNESDAY', label: 'Quarta-feira' },
  { key: 'THURSDAY', label: 'Quinta-feira' },
  { key: 'FRIDAY', label: 'Sexta-feira' },
  { key: 'SATURDAY', label: 'Sábado' },
  { key: 'SUNDAY', label: 'Domingo' },
];

export type HorariosPorDia = {
  [key in DiaSemana]: { ativo: boolean; horaInicio: string; horaFim: string };
};

export const DEFAULT_HORARIOS: HorariosPorDia = {
  MONDAY: { ativo: true, horaInicio: '06:00', horaFim: '23:00' },
  TUESDAY: { ativo: true, horaInicio: '06:00', horaFim: '23:00' },
  WEDNESDAY: { ativo: true, horaInicio: '06:00', horaFim: '23:00' },
  THURSDAY: { ativo: true, horaInicio: '06:00', horaFim: '23:00' },
  FRIDAY: { ativo: true, horaInicio: '06:00', horaFim: '23:00' },
  SATURDAY: { ativo: true, horaInicio: '06:00', horaFim: '23:00' },
  SUNDAY: { ativo: true, horaInicio: '06:00', horaFim: '23:00' },
};

interface CourtFormModalProps {
  isOpen: boolean;
  editandoId: number | null;
  nome: string;
  tipoEsporte: TipoEsporte;
  valorHora: string;
  descricao: string;
  dataLimiteAgendamento: string;
  horarios: HorariosPorDia;
  fotosExistentes: string[];
  novasFotosPreviews: string[];
  cep: string;
  logradouro: string;
  bairro: string;
  cidade: string;
  estado: string;
  loading: boolean;
  onClose: () => void;
  onNomeChange: (v: string) => void;
  onTipoEsporteChange: (v: TipoEsporte) => void;
  onValorHoraChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onDescricaoChange: (v: string) => void;
  onDataLimiteChange: (v: string) => void;
  onHorarioChange: (dia: DiaSemana, campo: 'horaInicio' | 'horaFim', valor: string) => void;
  onDiaToggle: (dia: DiaSemana) => void;
  onCopiarSegParaTodos: () => void;
  onAplicarPadraoTodos: () => void;
  onFileChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onRemoverFotoExistente: (url: string) => void;
  onRemoverNovaFoto: (index: number) => void;
  onCepChange: (v: string) => void;
  onLogradouroChange: (v: string) => void;
  onBairroChange: (v: string) => void;
  setCidadeChange: (v: string) => void;
  onEstadoChange: (v: string) => void;
  onSubmit: (e: React.FormEvent) => void;
  getAssetUrl: (path: string) => string;
}

export const CourtFormModal: React.FC<CourtFormModalProps> = ({
  isOpen,
  editandoId,
  nome,
  tipoEsporte,
  valorHora,
  descricao,
  dataLimiteAgendamento,
  horarios,
  fotosExistentes,
  novasFotosPreviews,
  cep,
  logradouro,
  bairro,
  cidade,
  estado,
  loading,
  onClose,
  onNomeChange,
  onTipoEsporteChange,
  onValorHoraChange,
  onDescricaoChange,
  onDataLimiteChange,
  onHorarioChange,
  onDiaToggle,
  onCopiarSegParaTodos,
  onAplicarPadraoTodos,
  onFileChange,
  onRemoverFotoExistente,
  onRemoverNovaFoto,
  onCepChange,
  onLogradouroChange,
  onBairroChange,
  setCidadeChange,
  onEstadoChange,
  onSubmit,
  getAssetUrl,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-zinc-950 border border-zinc-800 rounded-2xl w-full max-w-xl p-6 shadow-2xl space-y-6 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
          <h2 className="text-lg font-bold text-white flex items-center gap-2">
            {editandoId ? (
              <>
                <Edit2 className="w-4 h-4 text-white" />
                Editar Quadra
              </>
            ) : (
              <>
                <PlusCircle className="w-4 h-4 text-white" />
                Nova Quadra
              </>
            )}
          </h2>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-zinc-500 hover:text-white transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
              Nome da Quadra
            </label>
            <input
              type="text"
              required
              value={nome}
              onChange={(e) => onNomeChange(e.target.value)}
              placeholder="Ex: Arena Beach 01"
              className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
              Modalidade Esportiva
            </label>
            <select
              value={tipoEsporte}
              onChange={(e) => onTipoEsporteChange(e.target.value as TipoEsporte)}
              className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
            >
              <option value="FUTEBOL">FUTEBOL</option>
              <option value="FUTSAL">FUTSAL</option>
              <option value="VOLEI">VOLEI</option>
              <option value="BEACH_TENNIS">BEACH_TENNIS</option>
              <option value="BASQUETE">BASQUETE</option>
              <option value="TENIS">TENIS</option>
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
              Valor por Hora (R$)
            </label>
            <input
              type="text"
              required
              value={valorHora}
              onChange={onValorHoraChange}
              placeholder="0.00"
              className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
              Descrição & Informações da Quadra
            </label>
            <textarea
              rows={3}
              value={descricao}
              onChange={(e) => onDescricaoChange(e.target.value)}
              placeholder="Ex: Quadra de saibro coberta, com iluminação LED de alta potência, vestiários com ducha quente e arquibancada."
              className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition resize-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
              Data Limite de Agendamento <span className="text-zinc-500 font-normal">(Opcional)</span>
            </label>
            <input
              type="date"
              value={dataLimiteAgendamento}
              onChange={(e) => onDataLimiteChange(e.target.value)}
              className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition [color-scheme:dark]"
            />
            <p className="text-[11px] text-zinc-500 mt-1">
              Clientes não poderão agendar datas posteriores a este dia. Deixe em branco para permitir reservas contínuas.
            </p>
          </div>

          {/* Seção Horários de Funcionamento */}
          <div className="space-y-3 pt-2 border-t border-zinc-850">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-emerald-400" />
                <label className="text-xs font-semibold uppercase tracking-wider text-zinc-300">
                  Horários de Funcionamento
                </label>
              </div>
              <div className="flex items-center gap-2 text-xs">
                <button
                  type="button"
                  onClick={onCopiarSegParaTodos}
                  className="text-[11px] font-medium text-emerald-400 hover:text-emerald-300 bg-emerald-950/40 hover:bg-emerald-950/70 border border-emerald-800/40 px-2.5 py-1 rounded-lg transition active:scale-95"
                  title="Copiar horário da Segunda-feira para todos os dias"
                >
                  Copiar Seg p/ Todos
                </button>
                <button
                  type="button"
                  onClick={onAplicarPadraoTodos}
                  className="text-[11px] font-medium text-zinc-400 hover:text-zinc-200 bg-zinc-900 hover:bg-zinc-850 border border-zinc-800 px-2.5 py-1 rounded-lg transition active:scale-95"
                  title="Restaurar padrão (06:00 - 23:00 em todos os dias)"
                >
                  Padrão
                </button>
              </div>
            </div>

            <div className="space-y-2 bg-zinc-900/50 p-3 rounded-xl border border-zinc-800/80">
              {DIAS_SEMANA.map((dia) => {
                const diaConfig = horarios[dia.key];
                return (
                  <div
                    key={dia.key}
                    className={`flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2.5 rounded-lg border transition ${
                      diaConfig.ativo
                        ? 'bg-zinc-900 border-zinc-750'
                        : 'bg-zinc-950/40 border-zinc-850/50 opacity-60'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 min-w-[130px]">
                      <input
                        type="checkbox"
                        id={`dia-${dia.key}`}
                        checked={diaConfig.ativo}
                        onChange={() => onDiaToggle(dia.key)}
                        className="w-4 h-4 rounded border-zinc-700 bg-zinc-800 text-emerald-500 focus:ring-emerald-500/20 focus:ring-offset-0 cursor-pointer accent-emerald-500"
                      />
                      <label
                        htmlFor={`dia-${dia.key}`}
                        className="text-xs font-semibold text-zinc-200 cursor-pointer select-none"
                      >
                        {dia.label}
                      </label>
                    </div>

                    {diaConfig.ativo ? (
                      <div className="flex items-center gap-2 text-xs">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[11px] text-zinc-500 font-medium">De:</span>
                          <input
                            type="time"
                            required={diaConfig.ativo}
                            value={diaConfig.horaInicio}
                            onChange={(e) => onHorarioChange(dia.key, 'horaInicio', e.target.value)}
                            className="bg-zinc-950 border border-zinc-750 rounded-lg px-2 py-1 text-xs text-white focus:outline-none focus:border-emerald-500 font-mono"
                          />
                        </div>
                        <span className="text-zinc-600">às</span>
                        <div className="flex items-center gap-1.5">
                          <span className="text-[11px] text-zinc-500 font-medium">Até:</span>
                          <input
                            type="time"
                            required={diaConfig.ativo}
                            value={diaConfig.horaFim}
                            onChange={(e) => onHorarioChange(dia.key, 'horaFim', e.target.value)}
                            className="bg-zinc-950 border border-zinc-750 rounded-lg px-2 py-1 text-xs text-white focus:outline-none focus:border-emerald-500 font-mono"
                          />
                        </div>
                      </div>
                    ) : (
                      <span className="text-[11px] font-medium text-zinc-500 italic py-1">
                        Fechado neste dia
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Seção de Fotos da Quadra */}
          <div className="space-y-2.5">
            <div className="flex items-center justify-between">
              <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400">
                Fotos da Quadra (Máx. 5)
              </label>
              <span className="text-[11px] font-mono text-zinc-500">
                {fotosExistentes.length + novasFotosPreviews.length} / 5 fotos
              </span>
            </div>

            <div className="grid grid-cols-5 gap-2">
              {fotosExistentes.map((url, idx) => (
                <div key={`existente-${idx}`} className="relative aspect-square rounded-xl overflow-hidden border border-zinc-800 bg-zinc-900 group">
                  <img
                    src={getAssetUrl(url)}
                    alt="Foto da quadra"
                    className="w-full h-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => onRemoverFotoExistente(url)}
                    title="Remover foto"
                    className="absolute inset-0 bg-red-950/80 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <Trash2 className="w-4 h-4 text-red-300" />
                  </button>
                </div>
              ))}

              {novasFotosPreviews.map((preview, idx) => (
                <div key={`nova-${idx}`} className="relative aspect-square rounded-xl overflow-hidden border border-emerald-500/50 bg-zinc-900 group">
                  <img
                    src={preview}
                    alt="Nova foto"
                    className="w-full h-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => onRemoverNovaFoto(idx)}
                    title="Remover foto selecionada"
                    className="absolute inset-0 bg-red-950/80 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <Trash2 className="w-4 h-4 text-red-300" />
                  </button>
                </div>
              ))}

              {fotosExistentes.length + novasFotosPreviews.length < 5 && (
                <label className="aspect-square rounded-xl border border-dashed border-zinc-750 hover:border-emerald-400/80 bg-zinc-900/40 hover:bg-zinc-900 flex flex-col items-center justify-center cursor-pointer transition text-zinc-500 hover:text-emerald-400 group">
                  <Upload className="w-4 h-4 transition-transform group-hover:-translate-y-0.5" />
                  <span className="text-[10px] mt-1 font-medium">Adicionar</span>
                  <input
                    type="file"
                    multiple
                    accept="image/jpeg,image/png,image/webp"
                    className="hidden"
                    onChange={onFileChange}
                  />
                </label>
              )}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                CEP
              </label>
              <input
                type="text"
                required
                value={cep}
                onChange={(e) => onCepChange(e.target.value)}
                placeholder="XXXXX-XXX"
                className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                Logradouro
              </label>
              <input
                type="text"
                required
                value={logradouro}
                onChange={(e) => onLogradouroChange(e.target.value)}
                placeholder="Rua, Avenida..."
                className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                Bairro
              </label>
              <input
                type="text"
                required
                value={bairro}
                onChange={(e) => onBairroChange(e.target.value)}
                className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                Cidade
              </label>
              <input
                type="text"
                required
                value={cidade}
                onChange={(e) => setCidadeChange(e.target.value)}
                className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
              />
            </div>

            <div className="col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                Estado (UF)
              </label>
              <input
                type="text"
                required
                value={estado}
                maxLength={2}
                onChange={(e) => onEstadoChange(e.target.value.toUpperCase())}
                placeholder="SP"
                className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
              />
            </div>
          </div>

          <div className="flex gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 rounded-xl border border-zinc-800 text-zinc-400 hover:text-white text-xs font-semibold transition"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 bg-white hover:bg-zinc-200 text-black font-semibold py-2.5 rounded-xl text-xs transition shadow-lg disabled:opacity-50"
            >
              {loading ? 'Salvando...' : editandoId ? 'Salvar Alterações' : 'Cadastrar Quadra'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
