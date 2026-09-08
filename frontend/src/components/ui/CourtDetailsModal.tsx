import React from 'react';
import { X, MapPin, Activity, Calendar, ShieldCheck, Info, ExternalLink } from 'lucide-react';
import { Quadra } from '../../types';
import { CourtCarousel } from './CourtCarousel';
import { Badge } from './Badge';
import { Button } from './Button';

interface CourtDetailsModalProps {
  quadra: Quadra | null;
  isOpen: boolean;
  onClose: () => void;
  onSelectForBooking?: (quadraId: number) => void;
}

export const CourtDetailsModal: React.FC<CourtDetailsModalProps> = ({
  quadra,
  isOpen,
  onClose,
  onSelectForBooking,
}) => {
  if (!isOpen || !quadra) return null;

  const handleBookingClick = () => {
    if (onSelectForBooking) {
      onSelectForBooking(quadra.id_quadra);
    }
    onClose();
  };

  const enderecoCompleto = [
    quadra.logradouro,
    quadra.bairro,
    quadra.cidade && quadra.estado ? `${quadra.cidade} - ${quadra.estado}` : quadra.cidade,
    quadra.cep ? `CEP ${quadra.cep}` : null,
  ]
    .filter(Boolean)
    .join(', ');

  const googleMapsUrl =
    quadra.latitude && quadra.longitude
      ? `https://www.google.com/maps/search/?api=1&query=${quadra.latitude},${quadra.longitude}`
      : enderecoCompleto
      ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(enderecoCompleto)}`
      : null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6"
    >
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/85 backdrop-blur-md transition-opacity animate-in fade-in duration-200"
      />

      {/* Modal Card */}
      <div className="relative w-full max-w-2xl bg-surface-900 border border-surface-800 rounded-3xl shadow-2xl shadow-black/90 overflow-hidden z-10 flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200">
        {/* Header com botão fechar */}
        <div className="absolute top-4 right-4 z-20">
          <button
            onClick={onClose}
            aria-label="Fechar modal de detalhes"
            className="p-2 rounded-full bg-surface-950/80 hover:bg-surface-900 text-surface-400 hover:text-white border border-surface-750 backdrop-blur-md transition active:scale-95 shadow-lg cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Conteúdo com Scroll */}
        <div className="overflow-y-auto p-6 space-y-6 scrollbar-thin">
          {/* Carrossel de Imagens */}
          <CourtCarousel
            fotos={quadra.fotos}
            nomeQuadra={quadra.nome}
            aspectRatio="video"
          />

          {/* Cabeçalho de Detalhes da Quadra */}
          <div className="space-y-3.5 border-b border-surface-800/80 pb-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2.5">
                <Badge variant="neutral" className="text-xs">
                  {quadra.tipoEsporte.replace('_', ' ')}
                </Badge>
                <Badge variant={quadra.ativa ? 'success' : 'neutral'} withDot>
                  {quadra.ativa ? 'Disponível' : 'Indisponível'}
                </Badge>
              </div>

              <div className="flex items-baseline gap-1.5 font-mono">
                <span className="text-xs text-surface-400 font-semibold uppercase">Valor:</span>
                <span className="text-2xl font-bold text-white tracking-tight">
                  R$ {quadra.valorHora.toFixed(2)}
                </span>
                <span className="text-xs text-surface-400">/ hora</span>
              </div>
            </div>

            <h2 className="text-2xl sm:text-3xl font-bold text-white tracking-tight">
              {quadra.nome}
            </h2>

            {/* Endereço & Mapa */}
            {enderecoCompleto && (
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 p-3.5 rounded-2xl bg-surface-950/70 border border-surface-800">
                <div className="flex items-start gap-2.5 text-xs text-surface-300">
                  <MapPin className="w-4 h-4 text-brand-400 shrink-0 mt-0.5" />
                  <span>{enderecoCompleto}</span>
                </div>
                {googleMapsUrl && (
                  <a
                    href={googleMapsUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex items-center gap-1.5 text-xs text-brand-400 hover:text-brand-300 font-semibold transition"
                  >
                    <span>Ver no Maps</span>
                    <ExternalLink className="w-3.5 h-3.5" />
                  </a>
                )}
              </div>
            )}
          </div>

          {/* Descrição e Especificações */}
          <div className="space-y-2.5">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Info className="w-4 h-4 text-brand-400" />
              <span>Sobre o Espaço Esportivo</span>
            </h3>

            <div className="p-4 rounded-2xl bg-surface-950/60 border border-surface-800/80 text-xs sm:text-sm text-surface-300 leading-relaxed whitespace-pre-line">
              {quadra.descricao ? (
                quadra.descricao
              ) : (
                <span className="text-surface-500 italic">
                  Esta quadra possui infraestrutura completa para a prática esportiva, iluminação esportiva e ambiente preparado para atletas e visitantes.
                </span>
              )}
            </div>
          </div>

          {/* Destaques da Estrutura */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 pt-1">
            <div className="p-3.5 rounded-2xl bg-surface-950/60 border border-surface-800 flex items-center gap-3">
              <Activity className="w-5 h-5 text-brand-400 shrink-0" />
              <div>
                <span className="text-[10px] text-surface-500 block uppercase font-semibold">Piso</span>
                <span className="text-xs text-white font-medium">Oficial / Padrão</span>
              </div>
            </div>

            <div className="p-3.5 rounded-2xl bg-surface-950/60 border border-surface-800 flex items-center gap-3">
              <ShieldCheck className="w-5 h-5 text-brand-400 shrink-0" />
              <div>
                <span className="text-[10px] text-surface-500 block uppercase font-semibold">Segurança</span>
                <span className="text-xs text-white font-medium">Ambiente Monitorado</span>
              </div>
            </div>

            <div className="p-3.5 rounded-2xl bg-surface-950/60 border border-surface-800 flex items-center gap-3 col-span-2 sm:col-span-1">
              <Calendar className="w-5 h-5 text-brand-400 shrink-0" />
              <div>
                <span className="text-[10px] text-surface-500 block uppercase font-semibold">Agendamento</span>
                <span className="text-xs text-white font-medium">Instantâneo via Pix</span>
              </div>
            </div>
          </div>
        </div>

        {/* Rodapé / Botão de Ação */}
        {onSelectForBooking && (
          <div className="p-4 sm:p-5 bg-surface-950 border-t border-surface-800 flex items-center justify-between gap-4">
            <Button
              type="button"
              variant="outline"
              size="md"
              onClick={onClose}
            >
              Fechar
            </Button>

            <Button
              type="button"
              variant="secondary"
              size="md"
              onClick={handleBookingClick}
              leftIcon={<Calendar className="w-4 h-4" />}
            >
              Ver Horários e Agendar
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};
