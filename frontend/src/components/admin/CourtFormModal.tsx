import React from 'react';
import { TipoEsporte, DiaSemana } from '../../types';
import { Button, Input, Select } from '../ui';
import { Edit2, PlusCircle, X, Clock, Upload, Trash2, MapPin } from 'lucide-react';

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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-2xl animate-in fade-in duration-200">
      <div className="bg-[#121214] border border-white/[0.1] rounded-2xl sm:rounded-3xl w-full max-w-xl p-6 sm:p-7 shadow-2xl shadow-black/80 space-y-6 max-h-[90vh] overflow-y-auto scrollbar-thin">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-white/[0.06] pb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-white/[0.06] border border-white/[0.1] text-white flex items-center justify-center">
              {editandoId ? <Edit2 className="w-4 h-4" /> : <PlusCircle className="w-4 h-4" />}
            </div>
            <div>
              <h2 className="text-lg font-semibold text-white tracking-tight">
                {editandoId ? 'Editar Quadra' : 'Nova Quadra'}
              </h2>
              <p className="text-xs text-white/50">
                {editandoId ? 'Atualize as configurações e fotos da quadra' : 'Preencha as informações para disponibilizar uma nova arena'}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-white/50 hover:text-white border border-white/[0.08] transition cursor-pointer"
            title="Fechar"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <form onSubmit={onSubmit} className="space-y-4">
          <Input
            label="Nome da Quadra"
            required
            value={nome}
            onChange={(e) => onNomeChange(e.target.value)}
            placeholder="Ex: Arena Beach 01"
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Select
              label="Modalidade Esportiva"
              value={tipoEsporte}
              onChange={(e) => onTipoEsporteChange(e.target.value as TipoEsporte)}
            >
              <option value="FUTEBOL">FUTEBOL</option>
              <option value="FUTSAL">FUTSAL</option>
              <option value="VOLEI">VOLEI</option>
              <option value="BEACH_TENNIS">BEACH_TENNIS</option>
              <option value="BASQUETE">BASQUETE</option>
              <option value="TENIS">TENIS</option>
            </Select>

            <Input
              label="Valor por Hora (R$)"
              required
              value={valorHora}
              onChange={onValorHoraChange}
              placeholder="0.00"
            />
          </div>

          <div>
            <label className="block text-[11px] font-medium uppercase tracking-wider text-white/60 mb-1.5 font-mono">
              Descrição & Informações da Quadra
            </label>
            <textarea
              rows={3}
              value={descricao}
              onChange={(e) => onDescricaoChange(e.target.value)}
              placeholder="Ex: Quadra de saibro coberta, com iluminação LED de alta potência, vestiários com ducha quente e arquibancada."
              className="w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-4 py-2.5 text-xs text-white placeholder-white/30 focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-white/30 transition resize-none leading-relaxed"
            />
          </div>

          <div>
            <label className="block text-[11px] font-medium uppercase tracking-wider text-white/60 mb-1.5 font-mono">
              Data Limite de Agendamento <span className="text-white/40 font-normal font-sans">(Opcional)</span>
            </label>
            <input
              type="date"
              value={dataLimiteAgendamento}
              onChange={(e) => onDataLimiteChange(e.target.value)}
              className="w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-4 py-2 text-xs text-white focus:outline-none focus:ring-2 focus:ring-white/20 focus:border-white/30 transition font-mono [color-scheme:dark]"
            />
            <p className="text-[11px] text-white/40 mt-1">
              Clientes não poderão agendar datas posteriores a este dia. Deixe em branco para permitir reservas contínuas.
            </p>
          </div>

          {/* Seção Horários de Funcionamento */}
          <div className="space-y-3 pt-2 border-t border-white/[0.06]">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4 text-white/70" />
                <label className="text-xs font-semibold uppercase tracking-wider text-white/80 font-mono">
                  Horários de Funcionamento
                </label>
              </div>
              <div className="flex items-center gap-2 text-xs">
                <button
                  type="button"
                  onClick={onCopiarSegParaTodos}
                  className="text-[11px] font-medium text-white/80 hover:text-white bg-white/[0.06] hover:bg-white/[0.1] border border-white/[0.1] px-2.5 py-1 rounded-lg transition active:scale-95 cursor-pointer font-mono"
                  title="Copiar horário da Segunda-feira para todos os dias"
                >
                  Copiar Seg p/ Todos
                </button>
                <button
                  type="button"
                  onClick={onAplicarPadraoTodos}
                  className="text-[11px] font-medium text-white/50 hover:text-white bg-white/[0.03] hover:bg-white/[0.06] border border-white/[0.08] px-2.5 py-1 rounded-lg transition active:scale-95 cursor-pointer font-mono"
                  title="Restaurar padrão (06:00 - 23:00 em todos os dias)"
                >
                  Padrão
                </button>
              </div>
            </div>

            <div className="space-y-2 bg-white/[0.02] p-3 rounded-2xl border border-white/[0.06]">
              {DIAS_SEMANA.map((dia) => {
                const diaConfig = horarios[dia.key];
                return (
                  <div
                    key={dia.key}
                    className={`flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2.5 rounded-xl border transition ${
                      diaConfig.ativo
                        ? 'bg-white/[0.04] border-white/[0.08]'
                        : 'bg-white/[0.01] border-white/[0.04] opacity-40'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 min-w-[130px]">
                      <input
                        type="checkbox"
                        id={`dia-${dia.key}`}
                        checked={diaConfig.ativo}
                        onChange={() => onDiaToggle(dia.key)}
                        className="w-4 h-4 rounded border-white/20 bg-white/10 text-white focus:ring-white/20 focus:ring-offset-0 cursor-pointer accent-white"
                      />
                      <label
                        htmlFor={`dia-${dia.key}`}
                        className="text-xs font-medium text-white/90 cursor-pointer select-none"
                      >
                        {dia.label}
                      </label>
                    </div>

                    {diaConfig.ativo ? (
                      <div className="flex items-center gap-2 text-xs">
                        <div className="flex items-center gap-1.5">
                          <span className="text-[11px] text-white/40 font-medium font-mono">De:</span>
                          <input
                            type="time"
                            required={diaConfig.ativo}
                            value={diaConfig.horaInicio}
                            onChange={(e) => onHorarioChange(dia.key, 'horaInicio', e.target.value)}
                            className="bg-[#1c1c1e] border border-white/[0.08] rounded-lg px-2 py-1 text-xs text-white focus:outline-none focus:ring-2 focus:ring-white/20 font-mono [color-scheme:dark]"
                          />
                        </div>
                        <span className="text-white/40">às</span>
                        <div className="flex items-center gap-1.5">
                          <span className="text-[11px] text-white/40 font-medium font-mono">Até:</span>
                          <input
                            type="time"
                            required={diaConfig.ativo}
                            value={diaConfig.horaFim}
                            onChange={(e) => onHorarioChange(dia.key, 'horaFim', e.target.value)}
                            className="bg-[#1c1c1e] border border-white/[0.08] rounded-lg px-2 py-1 text-xs text-white focus:outline-none focus:ring-2 focus:ring-white/20 font-mono [color-scheme:dark]"
                          />
                        </div>
                      </div>
                    ) : (
                      <span className="text-[11px] font-medium text-white/40 italic py-1">
                        Fechado neste dia
                      </span>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Seção de Fotos da Quadra */}
          <div className="space-y-2.5 pt-2 border-t border-white/[0.06]">
            <div className="flex items-center justify-between">
              <label className="block text-[11px] font-medium uppercase tracking-wider text-white/60 font-mono">
                Fotos da Quadra (Máx. 5)
              </label>
              <span className="text-[11px] font-mono text-white/40">
                {fotosExistentes.length + novasFotosPreviews.length} / 5 fotos
              </span>
            </div>

            <div className="grid grid-cols-5 gap-2">
              {fotosExistentes.map((url, idx) => (
                <div key={`existente-${idx}`} className="relative aspect-square rounded-2xl overflow-hidden border border-white/[0.08] bg-[#1c1c1e] group">
                  <img
                    src={getAssetUrl(url)}
                    alt="Foto da quadra"
                    className="w-full h-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => onRemoverFotoExistente(url)}
                    title="Remover foto"
                    className="absolute inset-0 bg-black/70 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4 text-[#FF453A]" />
                  </button>
                </div>
              ))}

              {novasFotosPreviews.map((preview, idx) => (
                <div key={`nova-${idx}`} className="relative aspect-square rounded-2xl overflow-hidden border border-white/20 bg-[#1c1c1e] group">
                  <img
                    src={preview}
                    alt="Nova foto"
                    className="w-full h-full object-cover"
                  />
                  <button
                    type="button"
                    onClick={() => onRemoverNovaFoto(idx)}
                    title="Remover foto selecionada"
                    className="absolute inset-0 bg-black/70 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4 text-[#FF453A]" />
                  </button>
                </div>
              ))}

              {fotosExistentes.length + novasFotosPreviews.length < 5 && (
                <label className="aspect-square rounded-2xl border border-dashed border-white/15 hover:border-white/40 bg-white/[0.02] hover:bg-white/[0.05] flex flex-col items-center justify-center cursor-pointer transition text-white/40 hover:text-white group">
                  <Upload className="w-4 h-4 transition-transform group-hover:-translate-y-0.5" />
                  <span className="text-[10px] mt-1 font-medium font-mono">Adicionar</span>
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

          {/* Endereço */}
          <div className="space-y-3 pt-2 border-t border-white/[0.06]">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-white/80 flex items-center gap-2 font-mono">
              <MapPin className="w-4 h-4 text-white/60" />
              Localização & Endereço
            </h4>

            <div className="grid grid-cols-2 gap-3">
              <div className="col-span-2 sm:col-span-1">
                <Input
                  label="CEP"
                  required
                  value={cep}
                  onChange={(e) => onCepChange(e.target.value)}
                  placeholder="XXXXX-XXX"
                />
              </div>

              <div className="col-span-2">
                <Input
                  label="Logradouro"
                  required
                  value={logradouro}
                  onChange={(e) => onLogradouroChange(e.target.value)}
                  placeholder="Rua, Avenida..."
                />
              </div>

              <div>
                <Input
                  label="Bairro"
                  required
                  value={bairro}
                  onChange={(e) => onBairroChange(e.target.value)}
                />
              </div>

              <div>
                <Input
                  label="Cidade"
                  required
                  value={cidade}
                  onChange={(e) => setCidadeChange(e.target.value)}
                />
              </div>

              <div className="col-span-2 sm:col-span-1">
                <Input
                  label="Estado (UF)"
                  required
                  value={estado}
                  maxLength={2}
                  onChange={(e) => onEstadoChange(e.target.value.toUpperCase())}
                  placeholder="SP"
                />
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3 pt-4 border-t border-white/[0.06]">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              className="flex-1 cursor-pointer"
            >
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="primary"
              disabled={loading}
              className="flex-1 cursor-pointer font-medium"
            >
              {loading ? 'Salvando...' : editandoId ? 'Salvar Alterações' : 'Cadastrar Quadra'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
