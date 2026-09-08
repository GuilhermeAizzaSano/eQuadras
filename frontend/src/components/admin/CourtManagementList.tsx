import React from 'react';
import { Quadra, BloqueioHorario } from '../../types';
import { Badge, EmptyState, Button } from '../ui';
import {
  ShieldCheck,
  Ban,
  Clock,
  Power,
  Edit2,
  Trash2,
  PlusCircle,
  MapPin
} from 'lucide-react';

interface CourtManagementListProps {
  minhasQuadras: Quadra[];
  mapaBloqueiosPorQuadra: Record<number, BloqueioHorario[]>;
  onAbrirCriacao: () => void;
  onAlternarStatus: (quadra: Quadra) => void;
  onAbrirBloqueios: (quadra: Quadra) => void;
  onAbrirEdicao: (quadra: Quadra) => void;
  onExcluirQuadra: (quadra: Quadra) => void;
  getAssetUrl: (path: string) => string;
}

export const CourtManagementList: React.FC<CourtManagementListProps> = ({
  minhasQuadras,
  mapaBloqueiosPorQuadra,
  onAbrirCriacao,
  onAlternarStatus,
  onAbrirBloqueios,
  onAbrirEdicao,
  onExcluirQuadra,
  getAssetUrl,
}) => {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-white tracking-tight">Minhas Quadras Cadastradas</h2>
          <p className="text-xs text-zinc-400 mt-1">
            Cadastre novas arenas esportivas, defina valores por hora, edite fotos e gerencie status de disponibilidade.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={onAbrirCriacao}
          className="cursor-pointer"
        >
          <PlusCircle className="w-4 h-4" />
          Cadastrar Nova Quadra
        </Button>
      </div>

      {minhasQuadras.length === 0 ? (
        <EmptyState
          icon={ShieldCheck}
          title="Nenhuma quadra cadastrada"
          description="Você ainda não cadastrou nenhuma quadra esportiva. Adicione a sua primeira quadra para começar a receber reservas."
          actionLabel="Cadastrar Primeira Quadra"
          onAction={onAbrirCriacao}
          className="py-16"
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {minhasQuadras.map((q) => {
            const bloqueiosDesta = mapaBloqueiosPorQuadra[q.id_quadra] || [];
            const temBloqueiosAtivos = bloqueiosDesta.length > 0;

            return (
              <div
                key={q.id_quadra}
                className="bg-surface-900 border border-surface-800 rounded-3xl overflow-hidden shadow-xl hover:border-surface-700 transition-all flex flex-col justify-between group relative hover:-translate-y-0.5"
              >
                {/* Imagem de Capa com Badges */}
                <div className="relative aspect-video bg-surface-950 overflow-hidden">
                  {q.fotos && q.fotos.length > 0 ? (
                    <img
                      src={getAssetUrl(q.fotos[0])}
                      alt={q.nome}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-zinc-700">
                      <ShieldCheck className="w-12 h-12 stroke-1" />
                    </div>
                  )}

                  <div className="absolute inset-0 bg-gradient-to-t from-surface-950/80 via-transparent to-black/30 pointer-events-none" />

                  <div className="absolute top-3 right-3 flex items-center gap-1.5 z-10">
                    <Badge variant={q.ativa ? 'success' : 'neutral'} withDot>
                      {q.ativa ? 'ATIVA' : 'INATIVA'}
                    </Badge>
                  </div>

                  {temBloqueiosAtivos && (
                    <div className="absolute top-3 left-3 bg-amber-500/90 backdrop-blur-md text-surface-950 text-[10px] font-bold px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-lg font-mono">
                      <Ban className="w-3 h-3" />
                      <span>{bloqueiosDesta.length} {bloqueiosDesta.length === 1 ? 'bloqueio' : 'bloqueios'}</span>
                    </div>
                  )}

                  <div className="absolute bottom-3 left-3 bg-surface-950/80 backdrop-blur-md px-3 py-1 rounded-xl border border-surface-700/60 text-white font-mono text-xs font-bold shadow-lg">
                    R$ {q.valorHora.toFixed(2)} <span className="text-zinc-400 font-normal">/ hora</span>
                  </div>
                </div>

                {/* Informações da Quadra */}
                <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                  <div className="space-y-2.5">
                    <div className="flex items-center justify-between gap-2">
                      <h3 className="text-base font-bold text-white truncate group-hover:text-brand-300 transition-colors">{q.nome}</h3>
                      <span className="text-[10px] font-semibold text-zinc-400 uppercase px-2 py-0.5 bg-surface-950 rounded-lg border border-surface-800 shrink-0 font-mono">
                        {q.tipoEsporte.replace('_', ' ')}
                      </span>
                    </div>

                    <p className="text-xs text-zinc-400 line-clamp-2 leading-relaxed">
                      {q.descricao || 'Sem descrição cadastrada.'}
                    </p>

                    {q.dataLimiteAgendamento && (
                      <div className="text-[11px] text-amber-400/90 font-mono flex items-center gap-1.5 bg-amber-500/10 border border-amber-500/20 px-2.5 py-1 rounded-xl">
                        <Clock className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                        <span>Limite reservas: <strong>{q.dataLimiteAgendamento.split('-').reverse().join('/')}</strong></span>
                      </div>
                    )}

                    <div className="text-xs text-zinc-500 truncate pt-1 flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-zinc-600 shrink-0" />
                      <span className="truncate">{q.logradouro}, {q.bairro} - {q.cidade}/{q.estado}</span>
                    </div>
                  </div>

                  {/* Ações Rápidas */}
                  <div className="flex items-center justify-between gap-2 pt-4 border-t border-surface-800/80">
                    <button
                      onClick={() => onAlternarStatus(q)}
                      className={`flex-1 py-2 px-2.5 rounded-xl border text-xs font-semibold flex items-center justify-center gap-1.5 transition-all cursor-pointer active:scale-[0.98] ${
                        q.ativa
                          ? 'border-surface-800 text-zinc-400 hover:text-amber-400 hover:border-amber-500/30 bg-surface-950/40'
                          : 'border-brand-500/30 text-brand-400 bg-brand-500/10 hover:bg-brand-500/20'
                      }`}
                      title={q.ativa ? 'Desativar quadra' : 'Ativar quadra'}
                    >
                      <Power className="w-3.5 h-3.5" />
                      <span>{q.ativa ? 'Desativar' : 'Ativar'}</span>
                    </button>

                    <button
                      onClick={() => onAbrirBloqueios(q)}
                      className={`px-3 py-2 rounded-xl border text-xs transition-all cursor-pointer active:scale-[0.98] flex items-center gap-1.5 font-medium ${
                        temBloqueiosAtivos
                          ? 'bg-amber-500/15 border-amber-500/30 text-amber-300 hover:bg-amber-500/25'
                          : 'bg-surface-950 hover:bg-surface-800 border-surface-800 text-zinc-400 hover:text-white'
                      }`}
                      title="Gerenciar bloqueios de horários e datas desta quadra"
                    >
                      <Ban className="w-3.5 h-3.5" />
                      <span className="text-[11px]">Bloqueios</span>
                    </button>

                    <button
                      onClick={() => onAbrirEdicao(q)}
                      className="p-2 rounded-xl bg-surface-950 hover:bg-surface-800 border border-surface-800 text-zinc-300 hover:text-white text-xs transition-all cursor-pointer active:scale-[0.98]"
                      title="Editar quadra"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>

                    <button
                      onClick={() => onExcluirQuadra(q)}
                      className="p-2 rounded-xl bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/20 text-rose-400 hover:text-rose-300 text-xs transition-all cursor-pointer active:scale-[0.98]"
                      title="Excluir quadra"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
