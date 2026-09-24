import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { quadraApi, agendamentoApi, getAssetUrl, DashboardMetricas } from '../api/apiClient';
import { Quadra, Agendamento } from '../types';
import { FeedbackBanner, ConfirmModal, LoadingOverlay, CourtDetailsModal } from '../components/ui';
import { useAdminNotifications, useCourtBlocks, useCourtForm } from '../hooks';
import {
  AdminMetricsGrid,
  CalendarOccupancy,
  AdminScheduleToolbar,
  UpcomingMatchesBar,
  AdminDailyTimelineGrid,
  DayAgendaModal,
  CourtBlockModal,
  CourtHistoryModal,
  CourtFormModal,
  CourtManagementList,
  UserManagementList,
  UserFormModal,
  AuditLogsPanel,
  NotificationBellPopover,
} from '../components/admin';
import { usuarioApi } from '../api/apiClient';
import { Usuario, Role } from '../types';
import { parseDataHoraLocal, getHojeLocalIso, getAgoraBrasilia } from '../utils/dateUtils';
import { filtrarProximasPartidas } from '../utils/proximasPartidas';

interface AdminDashboardProps {
  activeTab?: 'dashboard' | 'quadras' | 'usuarios' | 'auditoria';
}

export const AdminDashboard: React.FC<AdminDashboardProps> = ({
  activeTab: controlledActiveTab,
}) => {
  const { user, isMasterAdmin } = useAuth();
  const [minhasQuadras, setMinhasQuadras] = useState<Quadra[]>([]);
  const [metricas, setMetricas] = useState<DashboardMetricas>({
    totalQuadras: 0,
    quadrasAtivas: 0,
    totalReservas: 0,
    faturamentoTotal: 0,
    reservasHoje: 0,
  });
  const [agendamentosHoje, setAgendamentosHoje] = useState<Agendamento[]>([]);
  const [agendamentosTimeline, setAgendamentosTimeline] = useState<Agendamento[]>([]);
  const [quadraDetalhes, setQuadraDetalhes] = useState<Quadra | null>(null);
  
  // Controle de Abas
  const activeTab = controlledActiveTab || 'dashboard';

  // Gestão de Usuários (Exclusivo Master Admin)
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [loadingUsuarios, setLoadingUsuarios] = useState(false);
  const [userModalOpen, setUserModalOpen] = useState(false);
  const [usuarioEditando, setUsuarioEditando] = useState<Usuario | null>(null);

  // Histórico por Quadra
  const [quadraHistoricoModal, setQuadraHistoricoModal] = useState<Quadra | null>(null);

  // Controle do Calendário Mensal
  // Visualização e Filtros da Agenda / Toolbar
  const [viewMode, setViewMode] = useState<'TIMELINE' | 'CALENDAR'>('TIMELINE');
  const [buscaTermoSchedule, setBuscaTermoSchedule] = useState('');
  const [statusFiltroSchedule, setStatusFiltroSchedule] = useState<'TODOS' | 'CONFIRMADOS' | 'REALIZADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES'>('TODOS');

  const [currentMonthDate, setCurrentMonthDate] = useState<Date>(() => new Date());
  const [dataSelecionada, setDataSelecionada] = useState<string>(() => getHojeLocalIso());
  const [quadraFiltroCalendarId, setQuadraFiltroCalendarId] = useState<number | 'TODAS'>('TODAS');
  const [modalAgendaDiaOpen, setModalAgendaDiaOpen] = useState(false);
  const [quadraSelecionadaAgendaId, setQuadraSelecionadaAgendaId] = useState<number | 'TODAS'>('TODAS');
  const [statusFiltroModal, setStatusFiltroModal] = useState<'TODOS' | 'LIVRES' | 'AGENDADOS' | 'BLOQUEADOS'>('TODOS');
  const [loadingHorariosModal, setLoadingHorariosModal] = useState(false);
  const [horariosDisponiveisPorQuadra, setHorariosDisponiveisPorQuadra] = useState<Record<number, import('../types').HorarioDisponivel[]>>({});
  const [visualizacaoAgendaAba, setVisualizacaoAgendaAba] = useState<'GRADE_HORARIOS' | 'LISTA_RESERVAS'>('GRADE_HORARIOS');
  const [highlightedAgendamentoId, setHighlightedAgendamentoId] = useState<number | null>(null);

  // Intervalo de tick para atualizar contadores de tempo real
  const [agora, setAgora] = useState(() => Date.now());
  useEffect(() => {
    const hasPendente =
      agendamentosHoje.some((a) => a.status === 'PENDENTE') ||
      agendamentosTimeline.some((a) => a.status === 'PENDENTE');
    // 1s para o contador do Pix; 60s para a lista de partidas acompanhar o relógio
    const timer = setInterval(() => setAgora(Date.now()), hasPendente ? 1000 : 60000);
    return () => clearInterval(timer);
  }, [agendamentosHoje, agendamentosTimeline]);

  const proximasPartidas = useMemo(() => {
    const { agora: agoraBrasilia, hojeIso } = getAgoraBrasilia();
    return filtrarProximasPartidas(agendamentosHoje, agoraBrasilia, hojeIso);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [agendamentosHoje, agora]);

  const getTempoRestantePix = (criadoEm: string) => {
    const criadoMs = new Date(criadoEm).getTime();
    const expiraMs = criadoMs + 15 * 60 * 1000;
    const diff = Math.floor((expiraMs - agora) / 1000);
    if (diff <= 0) return null;
    const min = Math.floor(diff / 60);
    const seg = diff % 60;
    return `${String(min).padStart(2, '0')}:${String(seg).padStart(2, '0')}`;
  };

  const [loading, setLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState('Processando dados...');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Modal de Confirmação Estilizado
  const [confirmModal, setConfirmModal] = useState<{
    isOpen: boolean;
    title: string;
    description: string;
    isDestructive: boolean;
    confirmLabel?: string;
    cancelLabel?: string;
    onConfirm: () => void;
  }>({
    isOpen: false,
    title: '',
    description: '',
    isDestructive: false,
    confirmLabel: undefined,
    cancelLabel: undefined,
    onConfirm: () => {},
  });

  const carregarDadosRef = React.useRef<() => void>();

  const handleNotificationReceived = React.useCallback((novaNotificacao: import('../types').Notificacao) => {
    setFeedback({ type: 'success', message: novaNotificacao.mensagem });
    carregarDadosRef.current?.();
  }, []);

  const {
    notificacoes,
    notificacoesPage,
    notificacoesTotalPages,
    unreadCount,
    carregarNotificacoes,
    lerNotificacao,
    marcarTodasComoLidas,
    excluirTodasNotificacoes,
  } = useAdminNotifications({
    user,
    onNotificationReceived: handleNotificationReceived,
  });

  const {
    bloqueioModalQuadra,
    setBloqueioModalQuadra,
    bloqueiosQuadra,
    loadingBloqueios,
    mapaBloqueiosPorQuadra,
    bloqueioData,
    setBloqueioData,
    bloqueioHoraInicio,
    setBloqueioHoraInicio,
    bloqueioHoraFim,
    setBloqueioHoraFim,
    bloqueioMotivo,
    setBloqueioMotivo,
    submittingBloqueio,
    carregarMapaBloqueios,
    abrirGerenciamentoBloqueios,
    handleBloquearSlot,
    handleDesbloquearSlot,
    handleCriarBloqueio,
    handleRemoverBloqueio,
  } = useCourtBlocks({
    minhasQuadras,
    onSuccessAction: () => carregarDados(),
    setFeedback,
    setConfirmModal,
  });

  const {
    isModalOpen: modalOpen,
    editandoId,
    nome,
    setNome,
    tipoEsporte,
    setTipoEsporte,
    valorHora,
    descricao,
    setDescricao,
    dataLimiteAgendamento,
    setDataLimiteAgendamento,
    cep,
    logradouro,
    setLogradouro,
    bairro,
    setBairro,
    cidade,
    setCidade,
    estado,
    setEstado,
    loadingCep,
    fotosExistentes,
    novasFotosPreviews,
    horarios,
    abrirModalCriacao,
    abrirModalEdicao,
    fecharModal,
    handleCepChange,
    handleValorChange,
    handleDiaToggle,
    handleHorarioChange,
    copiarSegParaTodos,
    aplicarPadraoTodos,
    handleFileChange,
    removerNovaFoto,
    removerFotoExistente,
    handleSalvarQuadra,
  } = useCourtForm({
    user,
    onSuccessAction: () => carregarDados(),
    setFeedback,
    setLoading,
    setLoadingMessage,
    setConfirmModal,
  });

  useEffect(() => {
    carregarDados();
  }, [user]);

  const confirmarExcluirTodasNotificacoes = () => {
    setConfirmModal({
      isOpen: true,
      title: 'Excluir Todas as Notificações',
      description: 'Deseja realmente excluir todo o histórico de notificações?\n\nEsta ação não poderá ser desfeita.',
      isDestructive: true,
      confirmLabel: 'Excluir Todas',
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        try {
          await excluirTodasNotificacoes();
          setFeedback({ type: 'success', message: 'Notificações excluídas com sucesso.' });
        } catch (err) {
          console.error('Erro ao excluir notificações:', err);
          setFeedback({ type: 'error', message: 'Erro ao excluir notificações.' });
        }
      },
    });
  };

  const carregarTimelineDia = React.useCallback(async (dataIso: string) => {
    try {
      const reservas = await agendamentoApi.listarAgendaCompleta({ data: dataIso });
      setAgendamentosTimeline(reservas);
    } catch (err) {
      console.error('Erro ao carregar agenda completa da timeline:', err);
    }
  }, []);

  const carregarDados = async () => {
    if (!user) return;
    try {
      const [quadras, metricasRes, agendaHojeRes] = await Promise.all([
        quadraApi.listar(),
        agendamentoApi.obterMetricasDashboard(),
        agendamentoApi.listarAgendaCompleta({ data: getHojeLocalIso() }),
      ]);
      setMinhasQuadras(quadras);
      setMetricas(metricasRes);
      setAgendamentosHoje(agendaHojeRes);

      // Carregar todos os bloqueios do admin consolidado
      await carregarMapaBloqueios();

      // Atualizar timeline do dia atual selecionado
      if (dataSelecionada) {
        if (dataSelecionada === getHojeLocalIso()) {
          // Mesma data ja buscada acima para "hoje": reaproveita a resposta
          // em vez de refazer a mesma requisicao de agenda completa.
          setAgendamentosTimeline(agendaHojeRes);
        } else {
          await carregarTimelineDia(dataSelecionada);
        }
      }
    } catch (err: any) {
      console.error(err);
    }
  };
  carregarDadosRef.current = carregarDados;

  useEffect(() => {
    if (user && dataSelecionada) {
      carregarTimelineDia(dataSelecionada);
    }
  }, [user, dataSelecionada, carregarTimelineDia]);

  const carregarUsuarios = async () => {
    if (!isMasterAdmin) return;
    setLoadingUsuarios(true);
    try {
      const lista = await usuarioApi.listar();
      setUsuarios(lista);
    } catch (err: any) {
      console.error('Erro ao listar usuários:', err);
      setFeedback({ type: 'error', message: err.message || 'Falha ao carregar lista de usuários.' });
    } finally {
      setLoadingUsuarios(false);
    }
  };

  // Lazy load para Usuários (Master Admin): só carrega quando entrar na aba de usuários
  useEffect(() => {
    if (activeTab === 'usuarios' && isMasterAdmin && usuarios.length === 0 && !loadingUsuarios) {
      carregarUsuarios();
    }
  }, [activeTab, isMasterAdmin, usuarios.length, loadingUsuarios]);

  const handleSalvarUsuario = async (dados: {
    nome_usuario: string;
    email_usuario: string;
    phone_usuario: string;
    role: Role;
    senha_usuario?: string;
    nova_senha?: string;
  }) => {
    if (usuarioEditando) {
      await usuarioApi.editar(usuarioEditando.id_usuario, {
        nome_usuario: dados.nome_usuario,
        email_usuario: dados.email_usuario,
        phone_usuario: dados.phone_usuario,
        role: dados.role,
        nova_senha: dados.nova_senha,
      });
      setFeedback({
        type: 'success',
        message: `Usuário "${dados.nome_usuario}" atualizado com sucesso!`,
      });
    } else {
      await usuarioApi.cadastrar({
        nome_usuario: dados.nome_usuario,
        email_usuario: dados.email_usuario,
        senha_usuario: dados.senha_usuario || '123456',
        phone_usuario: dados.phone_usuario,
        role: dados.role,
      });
      setFeedback({
        type: 'success',
        message: `Usuário "${dados.nome_usuario}" criado com sucesso!`,
      });
    }
    await carregarUsuarios();
  };

  const handleExcluirUsuario = async (u: Usuario) => {
    try {
      await usuarioApi.excluir(u.id_usuario);
      setFeedback({
        type: 'success',
        message: `Usuário "${u.nome_usuario}" excluído com sucesso!`,
      });
      await carregarUsuarios();
    } catch (err: any) {
      setFeedback({
        type: 'error',
        message: err.message || 'Falha ao excluir usuário.',
      });
    }
  };





  const abrirAgendaDoDia = async (dataIso: string) => {
    setDataSelecionada(dataIso);
    setQuadraSelecionadaAgendaId(quadraFiltroCalendarId);
    const modalStatus: 'TODOS' | 'LIVRES' | 'AGENDADOS' | 'BLOQUEADOS' =
      (statusFiltroSchedule === 'CONFIRMADOS' || statusFiltroSchedule === 'PENDENTES' || statusFiltroSchedule === 'REALIZADOS')
        ? 'AGENDADOS'
        : (statusFiltroSchedule === 'BLOQUEADOS' || statusFiltroSchedule === 'LIVRES')
          ? statusFiltroSchedule
          : 'TODOS';
    setStatusFiltroModal(modalStatus);
    if (statusFiltroSchedule === 'REALIZADOS') {
      setFiltroAgendaAdmin('REALIZADOS');
    } else if (statusFiltroSchedule === 'CONFIRMADOS' || statusFiltroSchedule === 'PENDENTES') {
      setFiltroAgendaAdmin('ATIVOS');
    }

    setHighlightedAgendamentoId(null);
    setModalAgendaDiaOpen(true);
    setLoadingHorariosModal(true);
    try {
      // Buscar slots consolidados de todas as quadras do admin para o dia em 1 requisição única
      const slotsPorQuadra = await agendamentoApi.listarHorariosDoDiaAdmin(dataIso);
      setHorariosDisponiveisPorQuadra(slotsPorQuadra || {});
    } catch (err) {
      console.error('Erro ao carregar horários consolidados do dia:', err);
      setHorariosDisponiveisPorQuadra({});
    } finally {
      setLoadingHorariosModal(false);
    }
  };



  const handleAbrirAgendamentoDetalhe = (ag: Agendamento) => {
    const dataIso = ag.dataHoraInicio.split('T')[0];
    setDataSelecionada(dataIso);
    setQuadraSelecionadaAgendaId(ag.quadraId);
    setStatusFiltroModal('TODOS');
    setVisualizacaoAgendaAba('LISTA_RESERVAS');

    const agoraLocal = getAgoraBrasilia().agora;
    const dataFim = parseDataHoraLocal(ag.dataHoraFim);
    if (ag.status === 'CANCELADO') {
      setFiltroAgendaAdmin('CANCELADOS');
    } else if (dataFim < agoraLocal) {
      setFiltroAgendaAdmin('REALIZADOS');
    } else {
      setFiltroAgendaAdmin('ATIVOS');
    }

    setHighlightedAgendamentoId(ag.id_agendamento);
    setModalAgendaDiaOpen(true);
    setLoadingHorariosModal(true);

    agendamentoApi.listarHorariosDoDiaAdmin(dataIso)
      .then((slots) => setHorariosDisponiveisPorQuadra(slots || {}))
      .catch((err) => {
        console.error('Erro ao carregar horários consolidados:', err);
        setHorariosDisponiveisPorQuadra({});
      })
      .finally(() => setLoadingHorariosModal(false));
  };





  const handleExcluirQuadra = (q: Quadra) => {
    if (!user) return;
    setConfirmModal({
      isOpen: true,
      title: 'Excluir Quadra',
      description: `Tem certeza que deseja excluir a quadra "${q.nome}"? Esta ação é irreversível e removerá todos os registros associados.`,
      isDestructive: true,
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        setLoadingMessage(`Excluindo quadra "${q.nome}"...`);
        setLoading(true);
        try {
          await quadraApi.excluir(q.id_quadra);
          setFeedback({ type: 'success', message: `Quadra "${q.nome}" excluída com sucesso.` });
          await carregarDados();
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Falha ao excluir quadra.' });
        } finally {
          setLoading(false);
        }
      },
    });
  };

  const handleAlternarStatus = async (quadra: Quadra) => {
    if (!user) return;
    const novoStatus = !quadra.ativa ? 'Ativar' : 'Inativar';
    const novoStatusMsg = !quadra.ativa ? 'Ativa' : 'Inativa';

    setConfirmModal({
      isOpen: true,
      title: `${novoStatus} Quadra`,
      description: `Deseja realmente ${novoStatus.toLowerCase()} a quadra "${quadra.nome}"? ${
        quadra.ativa
          ? 'Ela deixará de aparecer para novas reservas no aplicativo dos clientes.'
          : 'Ela voltará a ficar disponível para reservas imediatas.'
      }`,
      isDestructive: quadra.ativa,
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        setLoadingMessage(`Alterando disponibilidade da quadra...`);
        setLoading(true);
        try {
          await quadraApi.alternarStatus(quadra.id_quadra, !quadra.ativa);
          setFeedback({
            type: 'success',
            message: `Status da quadra "${quadra.nome}" alterado para ${novoStatusMsg} com sucesso.`,
          });
          await carregarDados();
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Falha ao alternar status.' });
        } finally {
          setLoading(false);
        }
      },
    });
  };

  const cancelarAgendamento = (id: number, onSuccess?: () => void) => {
    if (!user) return;
    const ag =
      agendamentosTimeline.find((a) => a.id_agendamento === id) ||
      agendamentosHoje.find((a) => a.id_agendamento === id);
    if (ag && parseDataHoraLocal(ag.dataHoraInicio) <= getAgoraBrasilia().agora) {
      setFeedback({
        type: 'error',
        message: 'Não é possível cancelar um agendamento que está em andamento ou retroativo.',
      });
      return;
    }
    setConfirmModal({
      isOpen: true,
      title: 'Cancelar Agendamento',
      description: 'Deseja realmente cancelar este agendamento? O horário voltará a ficar disponível para outros usuários.',
      isDestructive: true,
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        setLoadingMessage('Cancelando agendamento...');
        setLoading(true);
        try {
          await agendamentoApi.cancelar(id);
          setFeedback({ type: 'success', message: 'Agendamento cancelado com sucesso.' });
          await carregarDados();
          onSuccess?.();
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Falha ao cancelar agendamento.' });
        } finally {
          setLoading(false);
        }
      },
    });
  };



  const [filtroAgendaAdmin, setFiltroAgendaAdmin] = useState<'ATIVOS' | 'CANCELADOS' | 'REALIZADOS'>('ATIVOS');

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Toast Feedback Unificado */}
      <FeedbackBanner feedback={feedback} onClose={() => setFeedback(null)} />

      {/* Header & Navegação por Abas */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/[0.08] pb-5">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-white flex items-center gap-3">
            <span>Painel Administrativo</span>
            {isMasterAdmin && (
              <span className="text-[10px] uppercase font-mono font-medium tracking-wider px-2.5 py-0.5 rounded-full bg-[#0A84FF]/15 border border-[#0A84FF]/30 text-[#0A84FF]">
                Master Admin
              </span>
            )}
          </h1>
          <p className="text-xs text-white/50 mt-1">
            Gestão operacional em tempo real de quadras, reservas e agenda.
          </p>
        </div>

        {/* Abas e Notificações */}
        <div className="flex items-center gap-3">
          
          {/* Sino e Popover de Notificações */}
          <NotificationBellPopover
            notificacoes={notificacoes}
            unreadCount={unreadCount}
            notificacoesPage={notificacoesPage}
            notificacoesTotalPages={notificacoesTotalPages}
            onCarregarNotificacoes={carregarNotificacoes}
            onLerNotificacao={lerNotificacao}
            onMarcarTodasComoLidas={marcarTodasComoLidas}
            onConfirmarExcluirTodas={confirmarExcluirTodasNotificacoes}
          />
        </div>
      </div>

      {activeTab === 'dashboard' ? (
        <div className="space-y-8">
          {/* Métricas / KPIs */}
          <AdminMetricsGrid metricas={metricas} />

          {/* Próximas Partidas de Hoje (Próximas 4 horas) */}
          <UpcomingMatchesBar
            proximosJogos={proximasPartidas}
            minhasQuadras={minhasQuadras}
            onAbrirAgendamento={handleAbrirAgendamentoDetalhe}
          />

          {/* Toolbar da Agenda */}
          <AdminScheduleToolbar
            dataSelecionada={dataSelecionada}
            viewMode={viewMode}
            minhasQuadras={minhasQuadras}
            quadraFiltroId={quadraFiltroCalendarId}
            statusFiltro={statusFiltroSchedule}
            buscaTermo={buscaTermoSchedule}
            onDataChange={(novaData) => {
              setDataSelecionada(novaData);
              const [ano, mes] = novaData.split('-').map(Number);
              setCurrentMonthDate(new Date(ano, mes - 1, 1));
            }}
            onViewModeChange={setViewMode}
            onQuadraFiltroChange={setQuadraFiltroCalendarId}
            onStatusFiltroChange={setStatusFiltroSchedule}
            onBuscaTermoChange={setBuscaTermoSchedule}
            onHojeClick={() => {
              const hoje = getHojeLocalIso();
              setDataSelecionada(hoje);
              setCurrentMonthDate(new Date());
            }}
          />

          {/* Alternância entre Grade Diária e Calendário Mensal */}
          {viewMode === 'TIMELINE' ? (
            <AdminDailyTimelineGrid
              dataSelecionada={dataSelecionada}
              minhasQuadras={minhasQuadras}
              agendamentosAdmin={agendamentosTimeline}
              mapaBloqueiosPorQuadra={mapaBloqueiosPorQuadra}
              quadraFiltroId={quadraFiltroCalendarId}
              statusFiltro={statusFiltroSchedule}
              buscaTermo={buscaTermoSchedule}
              onAbrirAgendamento={handleAbrirAgendamentoDetalhe}
              onBloquearSlot={handleBloquearSlot}
              onDesbloquear={handleDesbloquearSlot}
              onQuadraFiltroChange={setQuadraFiltroCalendarId}
            />
          ) : (
            <CalendarOccupancy
              currentMonthDate={currentMonthDate}
              dataSelecionada={dataSelecionada}
              quadraFiltroCalendarId={quadraFiltroCalendarId}
              statusFiltroCalendar={statusFiltroSchedule}
              minhasQuadras={minhasQuadras}
              agendamentosAdmin={agendamentosTimeline}
              mapaBloqueiosPorQuadra={mapaBloqueiosPorQuadra}
              onMudarMes={(offset) =>
                setCurrentMonthDate((prev) => new Date(prev.getFullYear(), prev.getMonth() + offset, 1))
              }
              onAbrirAgendaDoDia={abrirAgendaDoDia}
            />
          )}
        </div>
      ) : activeTab === 'quadras' ? (
        /* Gestão de Quadras */
        <CourtManagementList
          minhasQuadras={minhasQuadras}
          mapaBloqueiosPorQuadra={mapaBloqueiosPorQuadra}
          onAbrirCriacao={abrirModalCriacao}
          onAlternarStatus={handleAlternarStatus}
          onAbrirBloqueios={abrirGerenciamentoBloqueios}
          onAbrirEdicao={abrirModalEdicao}
          onExcluirQuadra={handleExcluirQuadra}
          onAbrirHistorico={setQuadraHistoricoModal}
          getAssetUrl={getAssetUrl}
        />
      ) : activeTab === 'usuarios' ? (
        /* Gestão de Usuários (Apenas Master Admin) */
        <UserManagementList
          usuarios={usuarios}
          loading={loadingUsuarios}
          onRefresh={carregarUsuarios}
          onNovoUsuario={() => {
            setUsuarioEditando(null);
            setUserModalOpen(true);
          }}
          onEditarUsuario={(u) => {
            setUsuarioEditando(u);
            setUserModalOpen(true);
          }}
          onExcluirUsuario={handleExcluirUsuario}
        />
      ) : (
        /* Trilha de Auditoria (Exclusivo Master Admin) */
        <AuditLogsPanel />
      )}

      {/* Modal de Criação / Edição de Quadra */}
      <CourtFormModal
        isOpen={modalOpen}
        editandoId={editandoId}
        nome={nome}
        tipoEsporte={tipoEsporte}
        valorHora={valorHora}
        descricao={descricao}
        dataLimiteAgendamento={dataLimiteAgendamento}
        horarios={horarios}
        fotosExistentes={fotosExistentes}
        novasFotosPreviews={novasFotosPreviews}
        cep={cep}
        logradouro={logradouro}
        bairro={bairro}
        cidade={cidade}
        estado={estado}
        loading={loading || loadingCep}
        onClose={fecharModal}
        onNomeChange={setNome}
        onTipoEsporteChange={setTipoEsporte}
        onValorHoraChange={handleValorChange}
        onDescricaoChange={setDescricao}
        onDataLimiteChange={setDataLimiteAgendamento}
        onHorarioChange={handleHorarioChange}
        onDiaToggle={handleDiaToggle}
        onCopiarSegParaTodos={copiarSegParaTodos}
        onAplicarPadraoTodos={aplicarPadraoTodos}
        onFileChange={handleFileChange}
        onRemoverFotoExistente={removerFotoExistente}
        onRemoverNovaFoto={removerNovaFoto}
        onCepChange={handleCepChange}
        onLogradouroChange={setLogradouro}
        onBairroChange={setBairro}
        setCidadeChange={setCidade}
        onEstadoChange={setEstado}
        onSubmit={handleSalvarQuadra}
        getAssetUrl={getAssetUrl}
      />

      {/* Modal de Bloqueios */}
      <CourtBlockModal
        quadra={bloqueioModalQuadra}
        bloqueios={bloqueiosQuadra}
        loadingBloqueios={loadingBloqueios}
        bloqueioData={bloqueioData}
        bloqueioHoraInicio={bloqueioHoraInicio}
        bloqueioHoraFim={bloqueioHoraFim}
        bloqueioMotivo={bloqueioMotivo}
        isSubmitting={submittingBloqueio}
        onClose={() => setBloqueioModalQuadra(null)}
        onDataChange={setBloqueioData}
        onHoraInicioChange={setBloqueioHoraInicio}
        onHoraFimChange={setBloqueioHoraFim}
        onMotivoChange={setBloqueioMotivo}
        onSubmit={handleCriarBloqueio}
        onRemoverBloqueio={handleRemoverBloqueio}
      />

      {/* Modal de Histórico de Agendas da Quadra */}
      <CourtHistoryModal
        isOpen={!!quadraHistoricoModal}
        quadra={quadraHistoricoModal}
        onClose={() => setQuadraHistoricoModal(null)}
      />

      {/* Modal de Agenda do Dia */}
      <DayAgendaModal
        isOpen={modalAgendaDiaOpen}
        dataSelecionada={dataSelecionada}
        minhasQuadras={minhasQuadras}
        agendamentosAdmin={agendamentosTimeline}
        mapaBloqueiosPorQuadra={mapaBloqueiosPorQuadra}
        horariosDisponiveisPorQuadra={horariosDisponiveisPorQuadra}
        loadingHorariosModal={loadingHorariosModal}
        quadraSelecionadaAgendaId={quadraSelecionadaAgendaId}
        statusFiltroModal={statusFiltroModal}
        visualizacaoAgendaAba={visualizacaoAgendaAba}
        filtroAgendaAdmin={filtroAgendaAdmin}
        highlightedAgendamentoId={highlightedAgendamentoId}
        onClose={() => setModalAgendaDiaOpen(false)}
        onQuadraChange={setQuadraSelecionadaAgendaId}
        onStatusFiltroChange={setStatusFiltroModal}
        onVisualizacaoAbaChange={setVisualizacaoAgendaAba}
        onFiltroAgendaAdminChange={setFiltroAgendaAdmin}
        onSelectHighlightedAgendamento={setHighlightedAgendamentoId}
        onCancelarAgendamento={cancelarAgendamento}
        onVerQuadra={(q) => {
          setModalAgendaDiaOpen(false);
          setQuadraDetalhes(q);
        }}
        getTempoRestantePix={getTempoRestantePix}
      />

      {/* Modal de Cadastro / Edição de Usuários (Master Admin) */}
      <UserFormModal
        isOpen={userModalOpen}
        usuarioParaEditar={usuarioEditando}
        onClose={() => {
          setUserModalOpen(false);
          setUsuarioEditando(null);
        }}
        onSalvar={handleSalvarUsuario}
      />

      {/* Modal de Detalhes da Quadra com Carrossel */}
      <CourtDetailsModal
        isOpen={!!quadraDetalhes}
        quadra={quadraDetalhes}
        onClose={() => setQuadraDetalhes(null)}
      />

      {/* Modal de Confirmação Estilizado */}
      <ConfirmModal
        isOpen={confirmModal.isOpen}
        title={confirmModal.title}
        description={confirmModal.description}
        isDestructive={confirmModal.isDestructive}
        confirmLabel={confirmModal.confirmLabel || (confirmModal.isDestructive ? 'Sim, Confirmar' : 'Confirmar')}
        cancelLabel={confirmModal.cancelLabel || 'Voltar'}
        onConfirm={confirmModal.onConfirm}
        onCancel={() => setConfirmModal((prev) => ({ ...prev, isOpen: false }))}
      />

      {/* Loading Overlay Global Esmaecido */}
      <LoadingOverlay isLoading={loading} message={loadingMessage} />
    </div>
  );
};
