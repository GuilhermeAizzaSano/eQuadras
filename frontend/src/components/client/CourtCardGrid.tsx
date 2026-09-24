import React from 'react';
import { Quadra } from '../../types';
import { getAssetUrl } from '../../api/apiClient';
import { EmptyState, Badge } from '../ui';
import { Calendar as CalendarIcon, MapPin, Info, ChevronLeft, ChevronRight } from 'lucide-react';
import { ModoBusca } from './CourtSearchBar';

interface CourtCardGridProps {
  quadras: Quadra[];
  modoBusca: ModoBusca;
  onSelectCourtDetails: (quadra: Quadra) => void;
  onOpenBookingModal: (quadraId: number) => void;
  paginaAtual?: number;
  totalPaginas?: number;
  totalItens?: number;
  onMudarPagina?: (pagina: number) => void;
}

export const CourtCardGrid: React.FC<CourtCardGridProps> = ({
  quadras,
  modoBusca,
  onSelectCourtDetails,
  onOpenBookingModal,
  paginaAtual = 1,
  totalPaginas = 1,
  totalItens = 0,
  onMudarPagina,
}) => {
  if (quadras.length === 0) {
    return (
      <EmptyState
        icon={MapPin}
        title="Nenhuma quadra encontrada"
        description={
          modoBusca === 'ATRIBUTOS'
            ? 'Nenhuma quadra corresponde aos filtros de nome ou endereço informados.'
            : 'Tente alternar a categoria esportiva ou buscar sem restrição de CEP.'
        }
        className="py-14"
      />
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
      {quadras.map((q) => {
        const primeiraFoto =
          q.fotos && q.fotos.length > 0
            ? getAssetUrl(q.fotos[0])
            : 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=800&q=80';

        return (
          <div
            key={q.id_quadra}
            onClick={() => onSelectCourtDetails(q)}
            className="group relative rounded-2xl sm:rounded-3xl border border-fg/[0.06] hover:border-fg/[0.15] bg-fg/[0.03] hover:bg-fg/[0.03] overflow-hidden transition-all duration-200 cursor-pointer flex flex-col justify-between active:scale-[0.99]"
          >
            {/* Foto de Capa da Quadra */}
            <div className="relative aspect-video w-full overflow-hidden bg-bg">
              <img
                src={primeiraFoto}
                alt={q.nome}
                className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-[1.02]"
                onError={(e) => {
                  e.currentTarget.src =
                    'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=800&q=80';
                }}
              />
              <div className="absolute inset-0 bg-gradient-to-t from-black via-black/20 to-transparent" />

              {/* Badge de Esporte e Status sobre a imagem */}
              <div className="absolute top-3 left-3 right-3 flex items-center justify-between pointer-events-none">
                <Badge
                  variant="neutral"
                  className="bg-black/60 backdrop-blur-md border border-white/[0.1] text-white text-xs"
                >
                  {q.tipoEsporte.replace('_', ' ')}
                </Badge>

                <span className="p-1.5 rounded-full bg-black/60 backdrop-blur-xl border border-white/[0.15] text-white/70 group-hover:text-white transition shadow-sm">
                  <Info className="w-3.5 h-3.5" />
                </span>
              </div>

              <div className="absolute bottom-2.5 left-3.5 right-3.5 flex items-end justify-between">
                <div
                  className="w-2 h-2 rounded-full bg-success"
                  title="Quadra Ativa"
                />
              </div>
            </div>

            {/* Informações da Quadra */}
            <div className="p-5 space-y-3 flex-1 flex flex-col justify-between">
              <div>
                <div className="text-base font-semibold text-fg tracking-tight">
                  {q.nome}
                </div>

                {q.cidade && q.estado && (
                  <div className="text-xs text-fg/60 flex items-center gap-1.5 mt-1 tracking-tight">
                    <MapPin className="w-3.5 h-3.5 text-fg/60 shrink-0" />
                    <span className="truncate">
                      {q.bairro ? `${q.bairro}, ` : ''}
                      {q.cidade} - {q.estado}
                    </span>
                  </div>
                )}
              </div>

              <div className="pt-3 border-t border-fg/[0.1] flex items-center justify-between gap-2 mt-auto">
                <div>
                  <span className="text-xs text-fg/60 uppercase tracking-wider font-medium block">
                    Valor / hora
                  </span>
                  <span className="text-sm font-semibold text-fg font-mono">
                    R$ {q.valorHora.toFixed(2)}
                  </span>
                </div>

                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    onOpenBookingModal(q.id_quadra);
                  }}
                  className="text-xs px-3.5 py-2 rounded-xl transition-all font-medium flex items-center gap-1.5 active:scale-95 text-fg bg-fg/[0.1] hover:bg-fg/[0.15] border border-fg/[0.15] shadow-sm cursor-pointer tracking-tight"
                >
                  <CalendarIcon className="w-3.5 h-3.5 text-fg/70" />
                  <span>Ver Horários</span>
                </button>
              </div>
            </div>
          </div>
        );
      })}

      {totalPaginas > 1 && (
        <div className="col-span-full pt-4 border-t border-fg/[0.1] flex items-center justify-between text-xs">
          <button
            type="button"
            disabled={paginaAtual <= 1}
            onClick={() => onMudarPagina?.(Math.max(1, paginaAtual - 1))}
            className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-xl bg-fg/[0.03] hover:bg-fg/[0.1] text-fg/80 disabled:opacity-30 disabled:cursor-not-allowed transition border border-fg/[0.06] cursor-pointer active:scale-95"
          >
            <ChevronLeft className="w-3.5 h-3.5" />
            <span>Anterior</span>
          </button>
          <span className="text-xs text-fg/60 font-mono">
            Página {paginaAtual} de {totalPaginas} ({totalItens} {totalItens === 1 ? 'quadra' : 'quadras'})
          </span>
          <button
            type="button"
            disabled={paginaAtual >= totalPaginas}
            onClick={() => onMudarPagina?.(Math.min(totalPaginas, paginaAtual + 1))}
            className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-xl bg-fg/[0.03] hover:bg-fg/[0.1] text-fg/80 disabled:opacity-30 disabled:cursor-not-allowed transition border border-fg/[0.06] cursor-pointer active:scale-95"
          >
            <span>Próxima</span>
            <ChevronRight className="w-3.5 h-3.5" />
          </button>
        </div>
      )}
    </div>
  );
};
