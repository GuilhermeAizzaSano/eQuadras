import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { createPortal } from 'react-dom';
import { Quadra, Agendamento } from '../../types';
import { Badge, EmptyState, Button } from '../ui';
import {
  History,
  X,
  Calendar,
  Clock,
  Phone,
  User,
  Loader2,
  AlertCircle
} from 'lucide-react';
import { parseDataHoraLocal, formatarDataHoraBr } from '../../utils/dateUtils';
import { agendamentoApi, AbaAgendamento } from '../../api/apiClient';
import { usePaginatedQuery, PageFetcher } from '../../shared/pagination';
import { Pagination } from '../../shared/pagination/Pagination';

interface CourtHistoryModalProps {
  isOpen: boolean;
  quadra: Quadra | null;
  onClose: () => void;
}

type StatusFiltro = 'TODOS' | 'ATIVOS' | 'REALIZADOS' | 'CANCELADOS';

const ITENS_POR_PAGINA = 5;

const fetchCourtAgendamentosPage: PageFetcher<Agendamento, { quadraId: number; aba?: AbaAgendamento }> = ({
  page,
  size,
  filters,
  signal,
}) => {
  if (!filters.quadraId) {
    return Promise.resolve({
      content: [],
      page: 0,
      size,
      totalElements: 0,
      totalPages: 0,
    });
  }
  return agendamentoApi.listarPorQuadraPaginado({
    quadraId: filters.quadraId,
    page,
    size,
    aba: filters.aba,
    signal,
  });
};

export const CourtHistoryModal: React.FC<CourtHistoryModalProps> = ({
  isOpen,
  quadra,
  onClose,
}) => {
  const [filtroStatus, setFiltroStatus] = useState<StatusFiltro>('TODOS');
  const [contadores, setContadores] = useState<{ todos: number; ativos: number; realizados: number; cancelados: number }>({
    todos: 0,
    ativos: 0,
    realizados: 0,
    cancelados: 0,
  });

  const carregarContadores = useCallback(async () => {
    if (!quadra) return;
    try {
      const res = await agendamentoApi.obterContadoresPorQuadra(quadra.id_quadra);
      setContadores({
        todos: res['TODOS'] ?? 0,
        ativos: res['ATIVOS'] ?? 0,
        realizados: res['REALIZADOS'] ?? 0,
        cancelados: res['CANCELADOS'] ?? 0,
      });
    } catch (err) {
      console.error('Erro ao carregar contadores da quadra:', err);
    }
  }, [quadra]);

  const filters = useMemo(() => ({
    quadraId: quadra?.id_quadra ?? 0,
    aba: filtroStatus === 'TODOS' ? undefined : (filtroStatus as AbaAgendamento),
  }), [quadra, filtroStatus]);

  const {
    items: agendamentos,
    page,
    setPage,
    totalPages,
    totalElements,
    status,
    error,
    isEmpty,
  } = usePaginatedQuery(fetchCourtAgendamentosPage, filters, { size: ITENS_POR_PAGINA, enabled: isOpen && !!quadra });

  useEffect(() => {
    if (isOpen && quadra) {
      carregarContadores();
    }
  }, [isOpen, quadra, carregarContadores]);

  // Bloqueio de rolagem do body enquanto o modal estiver aberto e fechar com Escape
  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';

      const handleKeyDown = (e: KeyboardEvent) => {
        if (e.key === 'Escape' && status !== 'loading') {
          onClose();
        }
      };
      window.addEventListener('keydown', handleKeyDown);

      return () => {
        document.body.style.overflow = originalOverflow;
        window.removeEventListener('keydown', handleKeyDown);
      };
    }
  }, [isOpen, status, onClose]);

  const handleFiltroChange = (novoFiltro: StatusFiltro) => {
    setFiltroStatus(novoFiltro);
  };

  const agora = useMemo(() => new Date(), [isOpen]);

  const formatarData = (dataHoraIso: string) => {
    const dataParte = dataHoraIso.split('T')[0];
    if (!dataParte) return '';
    const [ano, mes, dia] = dataParte.split('-');
    return `${dia}/${mes}/${ano}`;
  };

  const formatarHorario = (inicioIso: string, fimIso: string) => {
    const horaInicio = inicioIso.split('T')[1]?.substring(0, 5) || '';
    const horaFim = fimIso.split('T')[1]?.substring(0, 5) || '';
    return `${horaInicio} às ${horaFim}`;
  };

  if (!isOpen || !quadra) return null;

  return createPortal(
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/70 backdrop-blur-2xl animate-in fade-in duration-200"
    >
      <div className="bg-surface-2 border border-fg/[0.1] rounded-2xl sm:rounded-3xl w-full max-w-2xl h-[650px] max-h-[90vh] flex flex-col shadow-apple-elevated overflow-hidden animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-fg/[0.06] flex items-center justify-between bg-fg/[0.03]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-fg/[0.06] border border-fg/[0.1] text-fg flex items-center justify-center">
              <History className="w-5 h-5 text-fg/80" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-semibold text-fg tracking-tight">
                Histórico de Agendas
              </h3>
              <p className="text-xs text-fg/60 mt-0.5">
                Quadra: <strong className="text-fg font-medium">{quadra.nome}</strong>
                <span className="ml-2 font-mono text-xs text-fg/60 uppercase px-1.5 py-0.5 bg-fg/[0.03] rounded-md border border-fg/[0.06]">
                  {quadra.tipoEsporte.replace('_', ' ')}
                </span>
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl bg-fg/[0.03] hover:bg-fg/[0.1] text-fg/60 hover:text-fg border border-fg/[0.1] transition cursor-pointer active:scale-95"
            title="Fechar histórico" aria-label="Fechar histórico"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Abas de Filtros por Status */}
        <div className="px-5 sm:px-6 py-3 border-b border-fg/[0.06] bg-fg/[0.03] flex items-center">
          <div className="flex w-full bg-fg/[0.03] p-1 rounded-xl border border-fg/[0.1] text-xs">
            <button
              type="button"
              onClick={() => handleFiltroChange('TODOS')}
              className={`flex-1 py-1.5 px-2 font-medium rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                filtroStatus === 'TODOS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg'
              }`}
            >
              <span>Todos</span>
              <span
                className={`text-xs px-1.5 py-0.2 rounded-full font-mono font-medium ${
                  filtroStatus === 'TODOS' ? 'bg-on-accent/10 text-on-accent' : 'bg-fg/[0.06] text-fg/60'
                }`}
              >
                {contadores.todos}
              </span>
            </button>

            <button
              type="button"
              onClick={() => handleFiltroChange('ATIVOS')}
              className={`flex-1 py-1.5 px-2 font-medium rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                filtroStatus === 'ATIVOS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg'
              }`}
            >
              <span>Ativos</span>
              <span
                className={`text-xs px-1.5 py-0.2 rounded-full font-mono font-medium ${
                  filtroStatus === 'ATIVOS' ? 'bg-on-accent/10 text-on-accent' : 'bg-fg/[0.06] text-fg/60'
                }`}
              >
                {contadores.ativos}
              </span>
            </button>

            <button
              type="button"
              onClick={() => handleFiltroChange('REALIZADOS')}
              className={`flex-1 py-1.5 px-2 font-medium rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                filtroStatus === 'REALIZADOS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg'
              }`}
            >
              <span>Realizados</span>
              <span
                className={`text-xs px-1.5 py-0.2 rounded-full font-mono font-medium ${
                  filtroStatus === 'REALIZADOS' ? 'bg-on-accent/10 text-on-accent' : 'bg-fg/[0.06] text-fg/60'
                }`}
              >
                {contadores.realizados}
              </span>
            </button>

            <button
              type="button"
              onClick={() => handleFiltroChange('CANCELADOS')}
              className={`flex-1 py-1.5 px-2 font-medium rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                filtroStatus === 'CANCELADOS'
                  ? 'bg-fg text-on-accent font-semibold shadow-sm'
                  : 'text-fg/60 hover:text-fg'
              }`}
            >
              <span>Cancelados</span>
              <span
                className={`text-xs px-1.5 py-0.2 rounded-full font-mono font-medium ${
                  filtroStatus === 'CANCELADOS' ? 'bg-on-accent/10 text-on-accent' : 'bg-fg/[0.06] text-fg/60'
                }`}
              >
                {contadores.cancelados}
              </span>
            </button>
          </div>
        </div>

        {/* Conteúdo da Modal */}
        <div className="p-5 sm:p-6 overflow-y-auto space-y-4 flex-1 scrollbar-thin">
          {status === 'loading' && agendamentos.length === 0 ? (
            <div className="py-16 flex flex-col items-center justify-center gap-3 text-fg/60 text-xs font-mono">
              <Loader2 className="w-6 h-6 animate-spin text-fg/60" />
              <span>Carregando histórico de agendamentos...</span>
            </div>
          ) : status === 'error' && agendamentos.length === 0 ? (
            <div className="py-12 px-4 flex flex-col items-center justify-center text-center border border-danger/20 rounded-2xl bg-danger/5">
              <AlertCircle className="w-8 h-8 text-danger mb-2" />
              <p className="text-xs text-danger font-medium">
                {error instanceof Error ? error.message : 'Falha ao carregar histórico de agendamentos.'}
              </p>
            </div>
          ) : isEmpty ? (
            <EmptyState
              icon={Calendar}
              title={
                filtroStatus === 'TODOS'
                  ? 'Nenhum agendamento encontrado'
                  : filtroStatus === 'ATIVOS'
                  ? 'Nenhum agendamento ativo'
                  : filtroStatus === 'REALIZADOS'
                  ? 'Nenhum agendamento realizado'
                  : 'Nenhum agendamento cancelado'
              }
              description={
                filtroStatus === 'TODOS'
                  ? 'Esta quadra ainda não possui registros de agendamento.'
                  : 'Não há registros nesta categoria para a quadra selecionada.'
              }
              className="py-12"
            />
          ) : (
            <div className="space-y-3">
              {agendamentos.map((ag) => {
                const isCancelado = ag.status === 'CANCELADO';
                const isPassado = parseDataHoraLocal(ag.dataHoraFim) < agora;

                const statusVariant = isCancelado
                  ? 'danger'
                  : isPassado
                  ? 'neutral'
                  : ag.status === 'PENDENTE'
                  ? 'warning'
                  : 'success';

                const statusLabel = isCancelado
                  ? 'Cancelado'
                  : isPassado
                  ? 'Realizado'
                  : ag.status === 'PENDENTE'
                  ? 'Pendente'
                  : 'Confirmado';

                return (
                  <div
                    key={ag.id_agendamento}
                    className="p-4 rounded-2xl bg-fg/[0.03] border border-fg/[0.06] hover:border-fg/[0.15] transition-colors space-y-3"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-semibold text-fg">
                            #{ag.id_agendamento}
                          </span>
                          <Badge
                            variant={statusVariant}
                            className="uppercase tracking-wider text-xs font-mono px-2 py-0.5"
                          >
                            {statusLabel}
                          </Badge>
                        </div>
                        <div className="flex items-center gap-2 mt-2 text-xs text-fg/80">
                          <User className="w-3.5 h-3.5 text-fg/60" />
                          <span className="font-medium text-fg">{ag.nomeUsuario}</span>
                        </div>
                        {ag.telefoneUsuario && (
                          <div className="flex items-center gap-2 mt-1 text-xs text-fg/60">
                            <Phone className="w-3.5 h-3.5 text-fg/60" />
                            <span className="font-mono text-xs">{ag.telefoneUsuario}</span>
                          </div>
                        )}
                      </div>

                      <div className="text-right space-y-1">
                        <div className="flex items-center justify-end gap-1.5 text-xs text-fg/90 font-medium">
                          <Calendar className="w-3.5 h-3.5 text-fg/60" />
                          <span>{formatarData(ag.dataHoraInicio)}</span>
                        </div>
                        <div className="flex items-center justify-end gap-1.5 text-xs text-fg/60 font-mono">
                          <Clock className="w-3 h-3 text-fg/60" />
                          <span>{formatarHorario(ag.dataHoraInicio, ag.dataHoraFim)}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-wrap items-center justify-between pt-2 border-t border-fg/[0.03] text-xs text-fg/60 font-mono gap-2">
                      <span>Criado em: {formatarDataHoraBr(ag.criadoEm)}</span>
                      {isCancelado && (
                        <span className="text-danger/90">
                          Cancelado em: <strong className="text-danger font-medium">{ag.canceladoEm ? formatarDataHoraBr(ag.canceladoEm) : '—'}</strong>
                        </span>
                      )}
                    </div>

                    <div className="flex items-center justify-between pt-2 border-t border-fg/[0.06] text-xs">
                      <span className="text-fg/60 font-mono">Valor Total:</span>
                      <span className="text-fg font-mono font-semibold text-sm">
                        R$ {ag.valorTotal.toFixed(2)}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Rodapé com Paginação e Botão Fechar */}
        <div className="p-4 border-t border-fg/[0.06] bg-fg/[0.03] flex flex-col sm:flex-row items-center justify-between gap-3">
          <Pagination
            page={page}
            totalPages={totalPages}
            totalElements={totalElements}
            isLoading={status === 'loading'}
            onPageChange={setPage}
          />

          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            className="w-full sm:w-auto cursor-pointer"
          >
            Fechar
          </Button>
        </div>
      </div>
    </div>,
    document.body
  );
};
