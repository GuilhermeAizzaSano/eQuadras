import React from 'react';
import { X, MapPin, Activity, Calendar, ShieldCheck, Info } from 'lucide-react';
import { Quadra } from '../../types';
import { CourtCarousel } from './CourtCarousel';
import { Badge } from './Badge';

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
  ].filter(Boolean).join(', ');

  const googleMapsUrl = quadra.latitude && quadra.longitude
    ? `https://www.google.com/maps/search/?api=1&query=${quadra.latitude},${quadra.longitude}`
    : enderecoCompleto
    ? `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(enderecoCompleto)}`
    : null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-black/85 backdrop-blur-md transition-opacity animate-in fade-in duration-200"
      />

      {/* Modal Card */}
      <div className="relative w-full max-w-2xl bg-zinc-950 border border-zinc-800 rounded-3xl shadow-2xl overflow-hidden z-10 flex flex-col max-h-[90vh] animate-in zoom-in-95 duration-200">
        
        {/* Header com botão fechar */}
        <div className="absolute top-4 right-4 z-20">
          <button
            onClick={onClose}
            className="p-2 rounded-full bg-zinc-950/80 hover:bg-zinc-900 text-zinc-400 hover:text-white border border-zinc-800 backdrop-blur-md transition active:scale-95 shadow-lg"
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
          <div className="space-y-3 border-b border-zinc-850 pb-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <Badge variant="neutral">
                  {quadra.tipoEsporte.replace('_', ' ')}
                </Badge>
                <span className="flex items-center gap-1.5 text-xs text-emerald-400 font-medium">
                  <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
                  {quadra.ativa ? 'Disponível para agendamento' : 'Indisponível no momento'}
                </span>
              </div>

              <div className="flex items-baseline gap-1 font-mono">
                <span className="text-xs text-zinc-500 font-semibold uppercase">Valor:</span>
                <span className="text-xl font-bold text-white">
                  R$ {quadra.valorHora.toFixed(2)}
                </span>
                <span className="text-xs text-zinc-400">/ hora</span>
              </div>
            </div>

            <h2 className="text-2xl font-bold text-white tracking-tight">
              {quadra.nome}
            </h2>

            {/* Endereço & Mapa */}
            {enderecoCompleto && (
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-3.5 rounded-xl bg-zinc-900/60 border border-zinc-850">
                <div className="flex items-start gap-2 text-xs text-zinc-300">
                  <MapPin className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                  <span>{enderecoCompleto}</span>
                </div>
                {googleMapsUrl && (
                  <a
                    href={googleMapsUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-emerald-400 hover:text-emerald-300 underline underline-offset-2 shrink-0 font-medium transition"
                  >
                    Abrir no Maps ↗
                  </a>
                )}
              </div>
            )}
          </div>

          {/* Descrição e Especificações */}
          <div className="space-y-3">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Info className="w-4 h-4 text-zinc-400" />
              Sobre o Local e Estrutura
            </h3>
            
            <div className="p-4 rounded-xl bg-zinc-900/40 border border-zinc-850 text-xs sm:text-sm text-zinc-300 leading-relaxed whitespace-pre-line">
              {quadra.descricao ? (
                quadra.descricao
              ) : (
                <span className="text-zinc-500 italic">
                  Esta quadra possui infraestrutura completa para a prática esportiva, iluminação de LED e ambiente preparado para atletas e visitantes.
                </span>
              )}
            </div>
          </div>

          {/* Destaques da Estrutura */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 pt-2">
            <div className="p-3 rounded-xl bg-zinc-900/50 border border-zinc-850 flex items-center gap-2.5">
              <Activity className="w-4 h-4 text-emerald-400" />
              <div>
                <span className="text-[10px] text-zinc-500 block uppercase font-semibold">Piso</span>
                <span className="text-xs text-white font-medium">Oficial / Padrão</span>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-zinc-900/50 border border-zinc-850 flex items-center gap-2.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <div>
                <span className="text-[10px] text-zinc-500 block uppercase font-semibold">Segurança</span>
                <span className="text-xs text-white font-medium">Ambiente Monitorado</span>
              </div>
            </div>

            <div className="p-3 rounded-xl bg-zinc-900/50 border border-zinc-850 flex items-center gap-2.5 col-span-2 sm:col-span-1">
              <Calendar className="w-4 h-4 text-emerald-400" />
              <div>
                <span className="text-[10px] text-zinc-500 block uppercase font-semibold">Agendamento</span>
                <span className="text-xs text-white font-medium">Instantâneo via Pix</span>
              </div>
            </div>
          </div>
        </div>

        {/* Rodapé / Botão de Ação */}
        {onSelectForBooking && (
          <div className="p-4 sm:p-5 bg-zinc-950 border-t border-zinc-850 flex items-center justify-between gap-4">
            <button
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-zinc-800 text-xs font-semibold text-zinc-400 hover:text-white hover:bg-zinc-900 transition active:scale-[0.98]"
            >
              Fechar
            </button>

            <button
              onClick={handleBookingClick}
              className="flex-1 sm:flex-none flex items-center justify-center gap-2 px-6 py-2.5 rounded-xl bg-emerald-400 hover:bg-emerald-300 text-zinc-950 text-xs font-bold transition shadow-lg shadow-emerald-950/20 active:scale-[0.98]"
            >
              <Calendar className="w-4 h-4 text-zinc-950" />
              <span>Ver Horários e Agendar</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
