import React from 'react';
import { ChevronDown, SlidersHorizontal, Navigation, Search, MapPin, X } from 'lucide-react';

export type ModoBusca = 'ATRIBUTOS' | 'CEP';

export const ESPORTES = ['TODOS', 'FUTEBOL', 'BEACH_TENNIS', 'TENIS', 'FUTSAL', 'VOLEI', 'BASQUETE'] as const;

export interface CourtSearchBarProps {
  filtroEsporte: string;
  onSelectEsporte: (esporte: string) => void;
  modoBusca: ModoBusca;
  onAlternarModoBusca: (modo: ModoBusca) => void;
  buscaNome: string;
  onChangeBuscaNome: (nome: string) => void;
  buscaEndereco: string;
  onChangeBuscaEndereco: (endereco: string) => void;
  onLimparFiltrosAtributos: () => void;
  cepBusca: string;
  onChangeCepBusca: (cep: string) => void;
  onBuscarQuadrasPorLocalizacao: () => void;
  onLimparFiltroCep: () => void;
  loading: boolean;
}

export const CourtSearchBar: React.FC<CourtSearchBarProps> = ({
  filtroEsporte,
  onSelectEsporte,
  modoBusca,
  onAlternarModoBusca,
  buscaNome,
  onChangeBuscaNome,
  buscaEndereco,
  onChangeBuscaEndereco,
  onLimparFiltrosAtributos,
  cepBusca,
  onChangeCepBusca,
  onBuscarQuadrasPorLocalizacao,
  onLimparFiltroCep,
  loading,
}) => {
  return (
    <div className="flex flex-col gap-3.5 bg-fg/[0.03] p-3 sm:p-4 rounded-2xl sm:rounded-3xl border border-fg/[0.06]">
      {/* Linha Superior: Esportes + Chave Seletora de Modo de Busca */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        {/* Dropdown de Esportes no Mobile */}
        <div className="relative block sm:hidden w-full">
          <select
            value={filtroEsporte}
            onChange={(e) => onSelectEsporte(e.target.value)}
            className="w-full bg-fg/[0.03] border border-fg/[0.1] rounded-xl px-3.5 py-2.5 text-xs font-medium text-fg focus:outline-none focus:border-fg/30 transition appearance-none cursor-pointer pr-10 shadow-sm"
          >
            {ESPORTES.map((esp) => (
              <option key={esp} value={esp} className="bg-surface-3 text-fg">
                {esp === 'TODOS' ? 'Todos os Esportes' : esp.replace('_', ' ')}
              </option>
            ))}
          </select>
          <ChevronDown className="w-4 h-4 text-fg/40 absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
        </div>

        {/* Filtro por Esporte (Tipográfico Apple no Desktop) */}
        <div className="hidden sm:flex items-center gap-1 overflow-x-auto pb-1 max-w-full scrollbar-none">
          {ESPORTES.map((esp) => (
            <button
              key={esp}
              onClick={() => onSelectEsporte(esp)}
              className={`text-xs px-3 py-1 rounded-lg whitespace-nowrap transition-all cursor-pointer tracking-tight font-medium ${
                filtroEsporte === esp
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg hover:bg-fg/[0.03]'
              }`}
            >
              {esp === 'TODOS' ? 'Todos os Esportes' : esp.replace('_', ' ')}
            </button>
          ))}
        </div>

        {/* Chave Seletora de Modo: Atributos vs CEP / Proximidade */}
        <div className="flex items-center bg-fg/[0.03] p-1 rounded-xl border border-fg/[0.1] shrink-0 self-start sm:self-auto">
          <button
            type="button"
            onClick={() => onAlternarModoBusca('ATRIBUTOS')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all cursor-pointer ${
              modoBusca === 'ATRIBUTOS'
                ? 'bg-fg text-on-accent font-semibold shadow-sm'
                : 'text-fg/60 hover:text-fg'
            }`}
          >
            <SlidersHorizontal className="w-3.5 h-3.5" />
            <span>Por Atributos</span>
          </button>
          <button
            type="button"
            onClick={() => onAlternarModoBusca('CEP')}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all cursor-pointer ${
              modoBusca === 'CEP'
                ? 'bg-fg text-on-accent font-semibold shadow-sm'
                : 'text-fg/60 hover:text-fg'
            }`}
          >
            <Navigation className="w-3.5 h-3.5" />
            <span>Por CEP (2 km)</span>
          </button>
        </div>
      </div>

      {/* Linha Inferior: Controles Específicos do Modo Ativo */}
      <div className="pt-2 border-t border-fg/[0.06]">
        {modoBusca === 'ATRIBUTOS' ? (
          <div className="flex flex-col sm:flex-row items-center gap-2.5 w-full">
            {/* Campo 1: Nome da Quadra */}
            <div className="relative w-full sm:flex-1">
              <Search className="w-3.5 h-3.5 text-fg/40 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                placeholder="Buscar por nome da quadra..."
                value={buscaNome}
                onChange={(e) => onChangeBuscaNome(e.target.value)}
                className="bg-fg/[0.03] border border-fg/[0.1] rounded-xl pl-9 pr-3.5 py-2 text-xs text-fg placeholder-fg/30 focus:outline-none focus:border-fg/30 focus:ring-2 focus:ring-fg/10 transition w-full"
              />
            </div>

            {/* Campo 2: Endereço (Bairro ou Rua) */}
            <div className="relative w-full sm:flex-1">
              <MapPin className="w-3.5 h-3.5 text-fg/40 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                placeholder="Buscar por bairro ou rua..."
                value={buscaEndereco}
                onChange={(e) => onChangeBuscaEndereco(e.target.value)}
                className="bg-fg/[0.03] border border-fg/[0.1] rounded-xl pl-9 pr-3.5 py-2 text-xs text-fg placeholder-fg/30 focus:outline-none focus:border-fg/30 focus:ring-2 focus:ring-fg/10 transition w-full"
              />
            </div>

            {/* Botão de Limpar Filtros */}
            {(buscaNome || buscaEndereco) && (
              <button
                type="button"
                onClick={onLimparFiltrosAtributos}
                className="flex items-center gap-1 bg-fg/[0.06] hover:bg-fg/[0.1] text-fg/70 hover:text-fg px-3 py-2 rounded-xl text-xs font-medium transition border border-fg/[0.1] shrink-0 active:scale-95 cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
                <span>Limpar</span>
              </button>
            )}
          </div>
        ) : (
          <div className="flex items-center gap-2 w-full sm:w-auto">
            <div className="relative flex-1 sm:w-64">
              <Navigation className="w-3.5 h-3.5 text-fg/40 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                placeholder="Digite o CEP (ex: 15707-585)"
                value={cepBusca}
                onChange={(e) => {
                  const val = e.target.value
                    .replace(/\D/g, '')
                    .replace(/(\d{5})(\d)/, '$1-$2')
                    .substring(0, 9);
                  onChangeCepBusca(val);
                }}
                onKeyDown={(e) => e.key === 'Enter' && onBuscarQuadrasPorLocalizacao()}
                className="bg-fg/[0.03] border border-fg/[0.1] rounded-xl pl-9 pr-3.5 py-2 text-xs text-fg placeholder-fg/30 focus:outline-none focus:border-fg/30 focus:ring-2 focus:ring-fg/10 transition w-full font-mono"
              />
            </div>
            <button
              onClick={onBuscarQuadrasPorLocalizacao}
              disabled={loading}
              className="bg-fg/[0.1] hover:bg-fg/[0.15] text-fg px-4 py-2 rounded-xl text-xs font-medium transition border border-fg/[0.1] disabled:opacity-40 shrink-0 active:scale-[0.98] cursor-pointer tracking-tight"
            >
              Buscar no Raio
            </button>
            {cepBusca && (
              <button
                type="button"
                onClick={onLimparFiltroCep}
                className="flex items-center gap-1 bg-fg/[0.06] hover:bg-fg/[0.1] text-fg/70 hover:text-fg px-3 py-2 rounded-xl text-xs font-medium transition border border-fg/[0.1] shrink-0 active:scale-95 cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
                <span>Limpar</span>
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
};
