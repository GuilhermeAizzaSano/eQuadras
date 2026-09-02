import React from 'react';
import { Quadra, BloqueioHorario } from '../../types';
import { Badge, EmptyState } from '../ui';
import {
  ShieldCheck,
  Ban,
  Clock,
  Power,
  Edit2,
  Trash2,
  PlusCircle
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
          <h2 className="text-xl font-bold text-white">Minhas Quadras Cadastradas</h2>
          <p className="text-xs text-zinc-400 mt-1">
            Cadastre novas arenas esportivas, defina valores por hora, edite fotos e ative ou inative quadras.
          </p>
        </div>
        <button
          onClick={onAbrirCriacao}
          className="bg-white hover:bg-zinc-200 text-black font-semibold px-4 py-2 rounded-xl text-xs flex items-center justify-center gap-2 transition shadow-lg active:scale-95"
        >
          <PlusCircle className="w-4 h-4" />
          Cadastrar Nova Quadra
        </button>
      </div>

      {minhasQuadras.length === 0 ? (
        <EmptyState
          icon={ShieldCheck}
          title="Nenhuma quadra cadastrada"
          description="Você ainda não cadastrou nenhuma quadra esportiva. Clique no botão acima para adicionar a sua primeira quadra."
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
                className="bg-zinc-950 border border-zinc-850 rounded-2xl overflow-hidden shadow-xl hover:border-zinc-700 transition flex flex-col justify-between group"
              >
                {/* Imagem de Capa com Badges */}
                <div className="relative aspect-video bg-zinc-900 overflow-hidden">
                  {q.fotos && q.fotos.length > 0 ? (
                    <img
                      src={getAssetUrl(q.fotos[0])}
                      alt={q.nome}
                      className="w-full h-full object-cover group-hover:scale-105 transition duration-500"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-zinc-600">
                      <ShieldCheck className="w-12 h-12 stroke-1" />
                    </div>
                  )}

                  <div className="absolute top-3 right-3 flex items-center gap-1.5">
                    <Badge variant={q.ativa ? 'success' : 'outline'}>
                      {q.ativa ? 'ATIVA' : 'INATIVA'}
                    </Badge>
                  </div>

                  {temBloqueiosAtivos && (
                    <div className="absolute top-3 left-3 bg-amber-500/90 backdrop-blur-sm text-zinc-950 text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 shadow-lg">
                      <Ban className="w-3 h-3" />
                      <span>{bloqueiosDesta.length} {bloqueiosDesta.length === 1 ? 'bloqueio' : 'bloqueios'}</span>
                    </div>
                  )}

                  <div className="absolute bottom-3 left-3 bg-black/60 backdrop-blur-md px-2.5 py-1 rounded-lg border border-white/10 text-white font-mono text-xs font-bold">
                    R$ {q.valorHora.toFixed(2)} / hora
                  </div>
                </div>

                {/* Informações da Quadra */}
                <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <h3 className="text-base font-bold text-white truncate">{q.nome}</h3>
                      <span className="text-[10px] font-semibold text-zinc-400 uppercase px-2 py-0.5 bg-zinc-900 rounded-md border border-zinc-800 shrink-0 font-mono">
                        {q.tipoEsporte.replace('_', ' ')}
                      </span>
                    </div>

                    <p className="text-xs text-zinc-400 line-clamp-2 leading-relaxed">
                      {q.descricao || 'Sem descrição cadastrada.'}
                    </p>

                    {q.dataLimiteAgendamento && (
                      <div className="text-[11px] text-amber-400/90 font-mono flex items-center gap-1.5 bg-amber-950/20 border border-amber-800/30 px-2 py-1 rounded-lg">
                        <Clock className="w-3 h-3 text-amber-400" />
                        <span>Limite de reservas: <strong>{q.dataLimiteAgendamento.split('-').reverse().join('/')}</strong></span>
                      </div>
                    )}

                    <div className="text-xs text-zinc-500 truncate pt-1">
                      {q.logradouro}, {q.bairro} - {q.cidade}/{q.estado}
                    </div>
                  </div>

                  {/* Ações Rápidas */}
                  <div className="flex items-center justify-between gap-2 pt-4 border-t border-zinc-850">
                    <button
                      onClick={() => onAlternarStatus(q)}
                      className={`flex-1 py-2 px-2.5 rounded-lg border text-xs font-semibold flex items-center justify-center gap-1.5 transition active:scale-[0.98] ${
                        q.ativa
                          ? 'border-zinc-800 text-zinc-400 hover:text-amber-400 hover:border-amber-900/50 bg-zinc-900/40'
                          : 'border-emerald-900/50 text-emerald-400 bg-emerald-950/30 hover:bg-emerald-950/60'
                      }`}
                      title={q.ativa ? 'Desativar quadra' : 'Ativar quadra'}
                    >
                      <Power className="w-3.5 h-3.5" />
                      <span>{q.ativa ? 'Desativar' : 'Ativar'}</span>
                    </button>

                    <button
                      onClick={() => onAbrirBloqueios(q)}
                      className={`p-2 rounded-lg border text-xs transition active:scale-[0.98] flex items-center gap-1.5 font-medium ${
                        temBloqueiosAtivos
                          ? 'bg-amber-950/40 border-amber-800/60 text-amber-300 hover:bg-amber-950/70'
                          : 'bg-zinc-900 hover:bg-zinc-800 border-zinc-800 text-zinc-400 hover:text-white'
                      }`}
                      title="Gerenciar bloqueios de horários e datas desta quadra"
                    >
                      <Ban className="w-3.5 h-3.5" />
                      <span className="text-[11px]">Bloqueios</span>
                    </button>

                    <button
                      onClick={() => onAbrirEdicao(q)}
                      className="p-2 rounded-lg bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-zinc-300 hover:text-white text-xs transition active:scale-[0.98]"
                      title="Editar quadra"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>

                    <button
                      onClick={() => onExcluirQuadra(q)}
                      className="p-2 rounded-lg bg-red-950/30 hover:bg-red-950/60 border border-red-900/40 text-red-400 hover:text-red-300 text-xs transition active:scale-[0.98]"
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
