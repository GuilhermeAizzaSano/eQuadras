import React, { useState, useEffect, useMemo } from 'react';
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
  ChevronLeft,
  ChevronRight,
  Loader2,
  AlertCircle
} from 'lucide-react';
import { parseDataHoraLocal } from '../../utils/dateUtils';
import { agendamentoApi } from '../../api/apiClient';

interface CourtHistoryModalProps {
  isOpen: boolean;
  quadra: Quadra | null;
  onClose: () => void;
}

type StatusFiltro = 'TODOS' | 'ATIVOS' | 'REALIZADOS' | 'CANCELADOS';

export const CourtHistoryModal: React.FC<CourtHistoryModalProps> = ({
  isOpen,
  quadra,
  onClose,
}) => {
  const ITENS_POR_PAGINA = 5;
  const [agendamentos, setAgendamentos] = useState<Agendamento[]>([]);
  const [loading, setLoading] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [filtroStatus, setFiltroStatus] = useState<StatusFiltro>('TODOS');
  const [paginaAtual, setPaginaAtual] = useState(1);

  // Carregamento rigorosamente sob demanda (Lazy Loading ao abrir a modal)
  useEffect(() => {
    if (isOpen && quadra) {
      setPaginaAtual(1);
      setFiltroStatus('TODOS');
      setErro(null);
      setLoading(true);

      agendamentoApi
        .listarPorQuadra(quadra.id_quadra)
        .then((dados) => {
          setAgendamentos(dados || []);
        })
        .catch((err: any) => {
          console.error('Erro ao buscar histórico da quadra:', err);
          setErro(err.message || 'Falha ao carregar o histórico de agendamentos da quadra.');
          setAgendamentos([]);
        })
        .finally(() => {
          setLoading(false);
        });
    } else if (!isOpen) {
      setAgendamentos([]);
      setErro(null);
    }
  }, [isOpen, quadra]);

  // Bloqueio de rolagem do body enquanto o modal estiver aberto
  useEffect(() => {
    if (isOpen) {
      const originalOverflow = document.body.style.overflow;
      document.body.style.overflow = 'hidden';
      return () => {
        document.body.style.overflow = originalOverflow;
      };
    }
  }, [isOpen]);

  // Resetar página atual ao alterar filtro
  const handleFiltroChange = (novoFiltro: StatusFiltro) => {
    setFiltroStatus(novoFiltro);
    setPaginaAtual(1);
  };

  const agora = useMemo(() => new Date(), [isOpen]);

  // Contadores por categoria
  const { contadores, agendamentosFiltrados } = useMemo(() => {
    let ativos = 0;
    let realizados = 0;
    let cancelados = 0;

    agendamentos.forEach((ag) => {
      const dataFim = parseDataHoraLocal(ag.dataHoraFim);
      const isCancelado = ag.status === 'CANCELADO';
      const isPassado = dataFim < agora;

      if (isCancelado) {
        cancelados++;
      } else if (isPassado) {
        realizados++;
      } else {
        ativos++;
      }
    });

    const filtrados = agendamentos.filter((ag) => {
      const dataFim = parseDataHoraLocal(ag.dataHoraFim);
      const isCancelado = ag.status === 'CANCELADO';
      const isPassado = dataFim < agora;

      if (filtroStatus === 'ATIVOS') return !isCancelado && !isPassado;
      if (filtroStatus === 'REALIZADOS') return !isCancelado && isPassado;
      if (filtroStatus === 'CANCELADOS') return isCancelado;
      return true;
    });

    return {
      contadores: {
        todos: agendamentos.length,
        ativos,
        realizados,
        cancelados,
      },
      agendamentosFiltrados: filtrados,
    };
  }, [agendamentos, filtroStatus, agora]);

  // Paginação de 5 em 5
  const totalItens = agendamentosFiltrados.length;
  const totalPaginas = Math.max(1, Math.ceil(totalItens / ITENS_POR_PAGINA));
  const paginaValida = Math.min(Math.max(1, paginaAtual), totalPaginas);
  const indiceInicio = (paginaValida - 1) * ITENS_POR_PAGINA;
  const agendamentosPaginados = agendamentosFiltrados.slice(
    indiceInicio,
    indiceInicio + ITENS_POR_PAGINA
  );

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
      <div className="bg-[#121214] border border-white/[0.1] rounded-2xl sm:rounded-3xl w-full max-w-2xl max-h-[90vh] flex flex-col shadow-2xl shadow-black/80 overflow-hidden animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-5 sm:p-6 border-b border-white/[0.06] flex items-center justify-between bg-white/[0.02]">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-white/[0.06] border border-white/[0.1] text-white flex items-center justify-center">
              <History className="w-5 h-5 text-white/80" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-semibold text-white tracking-tight">
                Histórico de Agendas
              </h3>
              <p className="text-xs text-white/50 mt-0.5">
                Quadra: <strong className="text-white font-medium">{quadra.nome}</strong>
                <span className="ml-2 font-mono text-[10px] text-white/40 uppercase px-1.5 py-0.5 bg-white/[0.04] rounded-md border border-white/[0.06]">
                  {quadra.tipoEsporte.replace('_', ' ')}
                </span>
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-white/50 hover:text-white border border-white/[0.08] transition cursor-pointer active:scale-95"
            title="Fechar histórico"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Abas de Filtros por Status */}
        <div className="px-5 sm:px-6 py-3 border-b border-white/[0.06] bg-white/[0.02] flex items-center">
          <div className="flex w-full bg-white/[0.04] p-1 rounded-xl border border-white/[0.08] text-xs">
            <button
              type="button"
              onClick={() => handleFiltroChange('TODOS')}
              className={`flex-1 py-1.5 px-2 font-medium rounded-lg transition-all flex items-center justify-center gap-1.5 cursor-pointer ${
                filtroStatus === 'TODOS'
                  ? 'bg-white text-black font-semibold shadow-sm'
                  : 'text-white/60 hover:text-white'
              }`}
            >
              <span>Todos</span>
              <span
                className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-medium ${
                  filtroStatus === 'TODOS' ? 'bg-black/10 text-black' : 'bg-white/[0.06] text-white/60'
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
                  ? 'bg-white text-black font-semibold shadow-sm'
                  : 'text-white/60 hover:text-white'
              }`}
            >
              <span>Ativos</span>
              <span
                className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-medium ${
                  filtroStatus === 'ATIVOS' ? 'bg-black/10 text-black' : 'bg-white/[0.06] text-white/60'
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
                  ? 'bg-white text-black font-semibold shadow-sm'
                  : 'text-white/60 hover:text-white'
              }`}
            >
              <span>Realizados</span>
              <span
                className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-medium ${
                  filtroStatus === 'REALIZADOS' ? 'bg-black/10 text-black' : 'bg-white/[0.06] text-white/60'
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
                  ? 'bg-white text-black font-semibold shadow-sm'
                  : 'text-white/60 hover:text-white'
              }`}
            >
              <span>Cancelados</span>
              <span
                className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-medium ${
                  filtroStatus === 'CANCELADOS' ? 'bg-black/10 text-black' : 'bg-white/[0.06] text-white/60'
                }`}
              >
                {contadores.cancelados}
              </span>
            </button>
          </div>
        </div>

        {/* Conteúdo da Modal */}
        <div className="p-5 sm:p-6 overflow-y-auto space-y-4 flex-1 scrollbar-thin">
          {loading ? (
            <div className="py-16 flex flex-col items-center justify-center gap-3 text-white/50 text-xs font-mono">
              <Loader2 className="w-6 h-6 animate-spin text-white/60" />
              <span>Carregando histórico de agendamentos...</span>
            </div>
          ) : erro ? (
            <div className="py-12 px-4 flex flex-col items-center justify-center text-center border border-[#FF453A]/20 rounded-2xl bg-[#FF453A]/5">
              <AlertCircle className="w-8 h-8 text-[#FF453A] mb-2" />
              <p className="text-xs text-[#FF453A] font-medium">{erro}</p>
            </div>
          ) : agendamentosFiltrados.length === 0 ? (
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
              {agendamentosPaginados.map((ag) => {
                const isCancelado = ag.status === 'CANCELADO';
                const isPassado = parseDataHoraLocal(ag.dataHoraFim) < agora;

                const statusVariant = isCancelado
                  ? 'outline'
                  : isPassado
                  ? 'neutral'
                  : ag.status === 'PENDENTE'
                  ? 'warning'
                  : 'success';

                const statusLabel = isCancelado
                  ? 'CANCELADO'
                  : isPassado
                  ? 'REALIZADO'
                  : ag.status;

                return (
                  <div
                    key={ag.id_agendamento}
                    className="p-4 rounded-2xl bg-white/[0.02] border border-white/[0.06] hover:border-white/20 transition-colors space-y-3"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="space-y-1">
                        <div className="text-sm font-semibold text-white flex items-center gap-1.5">
                          <User className="w-3.5 h-3.5 text-white/60" />
                          <span>{ag.nomeUsuario || 'Atleta'}</span>
                        </div>

                        {ag.telefoneUsuario && (
                          <div className="text-xs text-white/40 flex items-center gap-1.5 font-mono">
                            <Phone className="w-3 h-3 text-white/40" />
                            <span>{ag.telefoneUsuario}</span>
                          </div>
                        )}
                      </div>

                      <Badge variant={statusVariant} withDot>
                        {statusLabel}
                      </Badge>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs font-mono text-white/60">
                      <div className="flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-white/40" />
                        <span>{formatarData(ag.dataHoraInicio)}</span>
                      </div>

                      <div className="flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5 text-white/40" />
                        <span>{formatarHorario(ag.dataHoraInicio, ag.dataHoraFim)}</span>
                      </div>
                    </div>

                    <div className="flex items-center justify-between pt-2 border-t border-white/[0.06] text-xs">
                      <span className="text-white/50 font-mono">Valor Total:</span>
                      <span className="text-white font-mono font-semibold text-sm">
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
        <div className="p-4 border-t border-white/[0.06] bg-white/[0.02] flex flex-col sm:flex-row items-center justify-between gap-3">
          {totalPaginas > 1 ? (
            <div className="flex items-center justify-between w-full sm:w-auto gap-2">
              <button
                type="button"
                disabled={paginaValida <= 1}
                onClick={() => setPaginaAtual((p) => Math.max(1, p - 1))}
                className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-white/80 disabled:opacity-30 disabled:cursor-not-allowed transition border border-white/[0.06] cursor-pointer"
              >
                <ChevronLeft className="w-3.5 h-3.5" />
                <span>Anterior</span>
              </button>

              <span className="text-[11px] text-white/50 font-mono px-2">
                {paginaValida} / {totalPaginas} ({totalItens} {totalItens === 1 ? 'item' : 'itens'})
              </span>

              <button
                type="button"
                disabled={paginaValida >= totalPaginas}
                onClick={() => setPaginaAtual((p) => Math.min(totalPaginas, p + 1))}
                className="flex items-center gap-1 px-3 py-1.5 text-xs font-medium rounded-xl bg-white/[0.04] hover:bg-white/[0.08] text-white/80 disabled:opacity-30 disabled:cursor-not-allowed transition border border-white/[0.06] cursor-pointer"
              >
                <span>Próxima</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </button>
            </div>
          ) : (
            <span className="text-[11px] text-white/40 font-mono">
              Total de {totalItens} {totalItens === 1 ? 'reserva' : 'reservas'}
            </span>
          )}

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
