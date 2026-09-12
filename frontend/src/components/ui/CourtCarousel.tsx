import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, Image as ImageIcon } from 'lucide-react';
import { getAssetUrl } from '../../api/apiClient';

interface CourtCarouselProps {
  fotos?: string[];
  nomeQuadra: string;
  className?: string;
  aspectRatio?: 'video' | 'square' | 'wide';
}

export const CourtCarousel: React.FC<CourtCarouselProps> = ({
  fotos = [],
  nomeQuadra,
  className = '',
  aspectRatio = 'video',
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);

  const listaFotos =
    fotos && fotos.length > 0
      ? fotos
      : ['https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1200&q=80'];

  const prevSlide = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev === 0 ? listaFotos.length - 1 : prev - 1));
  };

  const nextSlide = (e: React.MouseEvent) => {
    e.stopPropagation();
    setCurrentIndex((prev) => (prev === listaFotos.length - 1 ? 0 : prev + 1));
  };

  const aspectClasses = {
    video: 'aspect-video',
    square: 'aspect-square',
    wide: 'aspect-[21/9]',
  };

  const resolveImageUrl = (url: string) => {
    return getAssetUrl(url);
  };

  return (
    <div
      className={`relative w-full overflow-hidden rounded-2xl sm:rounded-3xl bg-black border border-white/[0.08] group ${aspectClasses[aspectRatio]} ${className}`}
    >
      {/* Imagem Atual */}
      <img
        src={resolveImageUrl(listaFotos[currentIndex])}
        alt={`${nomeQuadra} - foto ${currentIndex + 1}`}
        className="w-full h-full object-cover select-none transition-transform duration-500 group-hover:scale-[1.02]"
        onError={(e) => {
          e.currentTarget.src =
            'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=1200&q=80';
        }}
      />

      {/* Gradiente sutil */}
      <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent pointer-events-none" />

      {/* Botões de Navegação */}
      {listaFotos.length > 1 && (
        <>
          <button
            type="button"
            onClick={prevSlide}
            aria-label="Foto anterior"
            className="absolute left-3 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/60 hover:bg-black/80 text-white border border-white/[0.15] backdrop-blur-xl opacity-0 group-hover:opacity-100 transition-all active:scale-95 shadow-lg cursor-pointer"
          >
            <ChevronLeft className="w-4 h-4" />
          </button>

          <button
            type="button"
            onClick={nextSlide}
            aria-label="Próxima foto"
            className="absolute right-3 top-1/2 -translate-y-1/2 p-2 rounded-full bg-black/60 hover:bg-black/80 text-white border border-white/[0.15] backdrop-blur-xl opacity-0 group-hover:opacity-100 transition-all active:scale-95 shadow-lg cursor-pointer"
          >
            <ChevronRight className="w-4 h-4" />
          </button>

          {/* Indicadores de Paginação */}
          <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-black/50 backdrop-blur-xl border border-white/[0.1]">
            {listaFotos.map((_, idx) => (
              <button
                key={idx}
                type="button"
                onClick={(e) => {
                  e.stopPropagation();
                  setCurrentIndex(idx);
                }}
                className={`h-1.5 rounded-full transition-all cursor-pointer ${
                  currentIndex === idx ? 'w-4 bg-white' : 'w-1.5 bg-white/30 hover:bg-white/50'
                }`}
                aria-label={`Ir para foto ${idx + 1}`}
              />
            ))}
          </div>
        </>
      )}

      {/* Contador numérico de fotos */}
      <div className="absolute top-3 right-3 px-2.5 py-1 rounded-full bg-black/60 backdrop-blur-xl border border-white/[0.1] text-[11px] font-medium text-white/80 flex items-center gap-1.5 shadow-sm">
        <ImageIcon className="w-3.5 h-3.5 text-white/70" />
        <span>
          {currentIndex + 1} / {listaFotos.length}
        </span>
      </div>
    </div>
  );
};
