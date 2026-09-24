import React, { useState } from 'react';
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
  MapPin,
  History
} from 'lucide-react';
import { Pagination } from '../../shared/pagination';

interface CourtManagementListProps {
  minhasQuadras: Quadra[];
  mapaBloqueiosPorQuadra: Record<number, BloqueioHorario[]>;
  onAbrirCriacao: () => void;
  onAlternarStatus: (quadra: Quadra) => void;
  onAbrirBloqueios: (quadra: Quadra) => void;
  onAbrirEdicao: (quadra: Quadra) => void;
  onExcluirQuadra: (quadra: Quadra) => void;
  onAbrirHistorico: (quadra: Quadra) => void;
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
  onAbrirHistorico,
  getAssetUrl,
}) => {
  const ITENS_POR_PAGINA = 6;
  const [paginaAtual, setPaginaAtual] = useState(0);

  const totalItens = minhasQuadras.length;
  const totalPaginas = Math.ceil(totalItens / ITENS_POR_PAGINA);
  const paginaValida = totalPaginas > 0 ? Math.min(paginaAtual, totalPaginas - 1) : 0;
  const indiceInicio = paginaValida * ITENS_POR_PAGINA;
  const quadrasPaginadas = minhasQuadras.slice(indiceInicio, indiceInicio + ITENS_POR_PAGINA);
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl sm:text-2xl font-semibold text-fg tracking-tight">Minhas Quadras Cadastradas</h2>
          <p className="text-xs text-fg/60 mt-1">
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
        <div className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {quadrasPaginadas.map((q) => {
            const bloqueiosDesta = mapaBloqueiosPorQuadra[q.id_quadra] || [];
            const temBloqueiosAtivos = bloqueiosDesta.length > 0;

            return (
              <div
                key={q.id_quadra}
                className="bg-surface-2 border border-fg/[0.1] rounded-2xl sm:rounded-3xl overflow-hidden shadow-[inset_0_1px_0_0_rgba(255,255,255,0.06)] hover:border-fg/20 transition-all flex flex-col justify-between group relative hover:-translate-y-0.5"
              >
                {/* Imagem de Capa com Badges */}
                <div className="relative aspect-video bg-surface-3 overflow-hidden">
                  {q.fotos && q.fotos.length > 0 ? (
                    <img
                      src={getAssetUrl(q.fotos[0])}
                      alt={q.nome}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-fg/60">
                      <ShieldCheck className="w-12 h-12 stroke-1" />
                    </div>
                  )}

                  <div className="absolute inset-0 bg-gradient-to-t from-surface-2 via-transparent to-black/40 pointer-events-none" />

                  <div className="absolute top-3 right-3 flex items-center gap-1.5 z-10">
                    <Badge variant={q.ativa ? 'success' : 'neutral'} withDot className="bg-surface-2/90 backdrop-blur-md">
                      {q.ativa ? 'ATIVA' : 'INATIVA'}
                    </Badge>
                  </div>

                  {temBloqueiosAtivos && (
                    <div className="absolute top-3 left-3 bg-surface-2/90 backdrop-blur-md border border-warning/30 text-warning text-xs font-medium px-2.5 py-0.5 rounded-full flex items-center gap-1 shadow-sm font-mono">
                      <Ban className="w-3 h-3" />
                      <span>{bloqueiosDesta.length} {bloqueiosDesta.length === 1 ? 'bloqueio' : 'bloqueios'}</span>
                    </div>
                  )}

                  <div className="absolute bottom-3 left-3 bg-black/60 backdrop-blur-md px-3 py-1 rounded-full border border-white/10 text-white font-mono text-xs font-semibold shadow-sm">
                    R$ {q.valorHora.toFixed(2)} <span className="text-white/60 font-normal">/ hora</span>
                  </div>
                </div>

                {/* Informações da Quadra */}
                <div className="p-5 flex-1 flex flex-col justify-between space-y-4">
                  <div className="space-y-2.5">
                    <div className="flex items-center justify-between gap-2">
                      <h3 className="text-base font-semibold text-fg truncate">{q.nome}</h3>
                      <span className="text-xs font-medium text-fg/60 uppercase px-2 py-0.5 bg-fg/[0.03] rounded-lg border border-fg/[0.1] shrink-0 font-mono">
                        {q.tipoEsporte.replace('_', ' ') || 'OUTRO'}
                      </span>
                    </div>

                    <p className="text-xs text-fg/60 line-clamp-2 leading-relaxed">
                      {q.descricao || 'Sem descrição cadastrada.'}
                    </p>

                    {q.dataLimiteAgendamento && (
                      <div className="text-xs text-warning font-mono flex items-center gap-1.5 bg-warning/10 border border-warning/20 px-2.5 py-1 rounded-xl">
                        <Clock className="w-3.5 h-3.5 text-warning shrink-0" />
                        <span>Limite reservas: <strong>{q.dataLimiteAgendamento.split('-').reverse().join('/')}</strong></span>
                      </div>
                    )}

                    <div className="text-xs text-fg/60 truncate pt-1 flex items-center gap-1.5">
                      <MapPin className="w-3.5 h-3.5 text-fg/60 shrink-0" />
                      <span className="truncate">{q.logradouro}, {q.bairro} - {q.cidade}/{q.estado}</span>
                    </div>
                  </div>

                  {/* Ações Rápidas */}
                  <div className="flex items-center justify-between gap-2 pt-4 border-t border-fg/[0.06]">
                    <button
                      onClick={() => onAlternarStatus(q)}
                      className={`flex-1 py-2 px-2.5 rounded-xl border text-xs font-medium flex items-center justify-center gap-1.5 transition-all cursor-pointer active:scale-[0.98] ${
                        q.ativa
                          ? 'border-fg/[0.1] text-fg/60 hover:text-warning hover:border-warning/30 bg-fg/[0.03]'
                          : 'border-success/30 text-success bg-success/10 hover:bg-success/20'
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
                          ? 'bg-warning/15 border-warning/30 text-warning hover:bg-warning/25'
                          : 'bg-fg/[0.03] hover:bg-fg/[0.1] border-fg/[0.1] text-fg/60 hover:text-fg'
                      }`}
                      title="Gerenciar bloqueios de horários e datas desta quadra"
                    >
                      <Ban className="w-3.5 h-3.5" />
                      <span className="text-xs">Bloqueios</span>
                    </button>

                    <button
                      onClick={() => onAbrirEdicao(q)}
                      className="p-2 rounded-xl bg-fg/[0.03] hover:bg-fg/[0.1] border border-fg/[0.1] text-fg/70 hover:text-fg text-xs transition-all cursor-pointer active:scale-[0.98]"
                      title="Editar quadra"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>

                    <button
                      onClick={() => onExcluirQuadra(q)}
                      className="p-2 rounded-xl bg-danger/10 hover:bg-danger/20 border border-danger/20 text-danger text-xs transition-all cursor-pointer active:scale-[0.98]"
                      title="Excluir quadra"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Botão de Histórico de Agendas */}
                  <button
                    type="button"
                    onClick={() => onAbrirHistorico(q)}
                    className="w-full mt-2.5 py-2 px-3 rounded-xl bg-fg/[0.03] hover:bg-fg/[0.1] border border-fg/[0.1] hover:border-fg/20 text-fg/70 hover:text-fg text-xs font-medium transition-all flex items-center justify-center gap-2 cursor-pointer active:scale-[0.98]"
                    title="Consultar histórico de agendamentos desta quadra"
                  >
                    <History className="w-3.5 h-3.5 text-fg/60" />
                    <span>Histórico de agendas</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>

        {/* Rodapé com Paginação Compartilhada */}
        <div className="pt-4 border-t border-fg/[0.1]">
          <Pagination
            page={paginaValida}
            totalPages={totalPaginas}
            totalElements={totalItens}
            onPageChange={setPaginaAtual}
          />
        </div>
      </div>
      )}
    </div>
  );
};
