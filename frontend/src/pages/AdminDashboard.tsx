import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { quadraApi, agendamentoApi, notificacaoApi, bloqueioApi, getBaseUrl, getAssetUrl } from '../api/apiClient';
import { Quadra, Agendamento, TipoEsporte, Notificacao, DiaSemana, DisponibilidadeDia, BloqueioHorario } from '../types';
import { FeedbackBanner, ConfirmModal, LoadingOverlay, CourtDetailsModal } from '../components/ui';
import {
  AdminMetricsGrid,
  CalendarOccupancy,
  AdminScheduleToolbar,
  UpcomingMatchesBar,
  AdminDailyTimelineGrid,
  DayAgendaModal,
  CourtBlockModal,
  CourtFormModal,
  CourtManagementList,
  UserManagementList,
  UserFormModal,
  HorariosPorDia,
  DEFAULT_HORARIOS,
  DIAS_SEMANA
} from '../components/admin';
import {
  LayoutDashboard,
  Settings2,
  Bell,
  X,
  Users
} from 'lucide-react';
import { usuarioApi } from '../api/apiClient';
import { Usuario, Role } from '../types';

export const AdminDashboard: React.FC = () => {
  const { user, token, isMasterAdmin } = useAuth();
  const [minhasQuadras, setMinhasQuadras] = useState<Quadra[]>([]);
  const [agendamentosAdmin, setAgendamentosAdmin] = useState<Agendamento[]>([]);
  const [quadraDetalhes, setQuadraDetalhes] = useState<Quadra | null>(null);
  
  // Notificações SSE
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);
  
  // Controle de Abas
  const [activeTab, setActiveTab] = useState<'dashboard' | 'quadras' | 'usuarios'>('dashboard');

  // Gestão de Usuários (Exclusivo Master Admin)
  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [loadingUsuarios, setLoadingUsuarios] = useState(false);
  const [userModalOpen, setUserModalOpen] = useState(false);
  const [usuarioEditando, setUsuarioEditando] = useState<Usuario | null>(null);

  // Modal de Criar / Editar Quadra
  const [modalOpen, setModalOpen] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [nome, setNome] = useState('');
  const [tipoEsporte, setTipoEsporte] = useState<TipoEsporte>('FUTEBOL');
  const [valorHora, setValorHora] = useState('');
  const [descricao, setDescricao] = useState('');
  const [dataLimiteAgendamento, setDataLimiteAgendamento] = useState('');
  const [fotosExistentes, setFotosExistentes] = useState<string[]>([]);
  const [novasFotos, setNovasFotos] = useState<File[]>([]);
  const [novasFotosPreviews, setNovasFotosPreviews] = useState<string[]>([]);
  const [horarios, setHorarios] = useState<HorariosPorDia>(DEFAULT_HORARIOS);
  const previewsRef = useRef<string[]>([]);
  previewsRef.current = novasFotosPreviews;

  // Gerenciamento de Bloqueios
  const [bloqueioModalQuadra, setBloqueioModalQuadra] = useState<Quadra | null>(null);
  const [bloqueiosQuadra, setBloqueiosQuadra] = useState<BloqueioHorario[]>([]);
  const [loadingBloqueios, setLoadingBloqueios] = useState(false);
  const [mapaBloqueiosPorQuadra, setMapaBloqueiosPorQuadra] = useState<Record<number, BloqueioHorario[]>>({});
  const [bloqueioData, setBloqueioData] = useState('');
  const [bloqueioHoraInicio, setBloqueioHoraInicio] = useState('');
  const [bloqueioHoraFim, setBloqueioHoraFim] = useState('');
  const [bloqueioMotivo, setBloqueioMotivo] = useState('');

  // Revogar ObjectURLs criadas para previews ao desmontar o componente
  useEffect(() => {
    return () => {
      previewsRef.current.forEach((url) => URL.revokeObjectURL(url));
    };
  }, []);
  
  // Endereço
  const [cep, setCep] = useState('');
  const [logradouro, setLogradouro] = useState('');
  const [bairro, setBairro] = useState('');
  const [cidade, setCidade] = useState('');
  const [estado, setEstado] = useState('');
  const [latitude, setLatitude] = useState<number | undefined>();
  const [longitude, setLongitude] = useState<number | undefined>();

  // Controle do Calendário Mensal
  // Visualização e Filtros da Agenda / Toolbar
  const [viewMode, setViewMode] = useState<'TIMELINE' | 'CALENDAR'>('TIMELINE');
  const [buscaTermoSchedule, setBuscaTermoSchedule] = useState('');
  const [statusFiltroSchedule, setStatusFiltroSchedule] = useState<'TODOS' | 'CONFIRMADOS' | 'PENDENTES' | 'BLOQUEADOS' | 'LIVRES'>('TODOS');

  const [currentMonthDate, setCurrentMonthDate] = useState<Date>(() => new Date());
  const [dataSelecionada, setDataSelecionada] = useState<string>(() => {
    return new Date().toISOString().split('T')[0];
  });
  const [quadraFiltroCalendarId, setQuadraFiltroCalendarId] = useState<number | 'TODAS'>('TODAS');
  const [statusFiltroCalendar, setStatusFiltroCalendar] = useState<'TODOS' | 'LIVRES' | 'AGENDADOS' | 'BLOQUEADOS'>('TODOS');
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
    const hasPendente = agendamentosAdmin.some((a) => a.status === 'PENDENTE');
    if (!hasPendente) return;
    const timer = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [agendamentosAdmin]);

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

  useEffect(() => {
    carregarDados();
    carregarNotificacoes();

    if (user) {
      // Setup SSE for real-time notifications
      const streamUrl = token
        ? `${getBaseUrl()}/notificacoes/stream?token=${token}`
        : `${getBaseUrl()}/notificacoes/stream`;
      eventSourceRef.current = new EventSource(streamUrl);
      
      eventSourceRef.current.addEventListener('notificacao', (event) => {
        const novaNotificacao: Notificacao = JSON.parse(event.data);
        
        setNotificacoes((prev) => [novaNotificacao, ...prev]);
        setFeedback({ type: 'success', message: novaNotificacao.mensagem });
        
        // Recarregar os agendamentos para refletir no calendário instantaneamente
        carregarAgendamentos();
      });

      return () => {
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
        }
      };
    }
  }, [user, token]);

  const formatarDataHora = (dataIso?: string) => {
    if (!dataIso) return '';
    const d = new Date(dataIso);
    if (isNaN(d.getTime())) return dataIso;
    const dia = String(d.getDate()).padStart(2, '0');
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const ano = d.getFullYear();
    const horas = String(d.getHours()).padStart(2, '0');
    const minutos = String(d.getMinutes()).padStart(2, '0');
    return `${dia}/${mes}/${ano} ${horas}:${minutos}`;
  };

  const carregarAgendamentos = async () => {
    try {
      const agendamentos = await agendamentoApi.listar(true);
      setAgendamentosAdmin(agendamentos);
    } catch (err) {
      console.error(err);
    }
  };

  const carregarNotificacoes = async () => {
    if (!user) return;
    try {
      const data = await notificacaoApi.listarPorAdmin();
      setNotificacoes(data);
    } catch (err) {
      console.error(err);
    }
  };

  const lerNotificacao = async (id: number) => {
    try {
      await notificacaoApi.marcarComoLida(id);
      setNotificacoes(notificacoes.map(n => n.id === id ? { ...n, lida: true } : n));
    } catch (err) {
      console.error(err);
    }
  };

  const carregarDados = async () => {
    if (!user) return;
    try {
      const [quadras, agendamentos] = await Promise.all([
        quadraApi.listar(),
        agendamentoApi.listar(true),
      ]);
      setMinhasQuadras(quadras);
      setAgendamentosAdmin(agendamentos);

      // Carregar todos os bloqueios do admin em uma única requisição HTTP consolidada
      try {
        const todosBloqueios = await bloqueioApi.listarTodosAdmin();
        const novoMapa: Record<number, BloqueioHorario[]> = {};
        todosBloqueios.forEach((b) => {
          if (b.quadraId) {
            if (!novoMapa[b.quadraId]) {
              novoMapa[b.quadraId] = [];
            }
            novoMapa[b.quadraId].push(b);
          }
        });
        setMapaBloqueiosPorQuadra(novoMapa);
      } catch (bErr) {
        console.error('Erro ao carregar mapa de bloqueios consolidado:', bErr);
        setMapaBloqueiosPorQuadra({});
      }

      // Se for Master Admin, carregar usuários
      if (isMasterAdmin) {
        carregarUsuarios();
      }
    } catch (err: any) {
      console.error(err);
    }
  };

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

  // Métricas / KPIs
  const metricas = useMemo(() => {
    const agendamentosValidos = agendamentosAdmin.filter((a) => a.status !== 'CANCELADO');
    const faturamentoTotal = agendamentosValidos.reduce((acc, a) => acc + Number(a.valorTotal || 0), 0);
    
    const hoje = new Date().toISOString().split('T')[0];
    const agendamentosHoje = agendamentosValidos.filter((a) => a.dataHoraInicio.startsWith(hoje));

    return {
      totalQuadras: minhasQuadras.length,
      quadrasAtivas: minhasQuadras.filter((q) => q.ativa).length,
      totalReservas: agendamentosValidos.length,
      faturamentoTotal,
      reservasHoje: agendamentosHoje.length,
    };
  }, [minhasQuadras, agendamentosAdmin]);

  const buscarCep = async (cepBuscado: string) => {
    const cepNumerico = cepBuscado.replace(/\D/g, '');
    if (cepNumerico.length !== 8) return;

    try {
      const response = await fetch(`https://viacep.com.br/ws/${cepNumerico}/json/`);
      const data = await response.json();
      
      if (!data.erro) {
        setLogradouro(data.logradouro);
        setBairro(data.bairro);
        setCidade(data.localidade);
        setEstado(data.uf);

        // Fetch Coordinates via Nominatim OpenStreetMap
        const query = encodeURIComponent(`${data.logradouro}, ${data.localidade}, ${data.uf}`);
        const nominatimRes = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${query}`);
        const nominatimData = await nominatimRes.json();
        
        if (nominatimData && nominatimData.length > 0) {
          setLatitude(parseFloat(nominatimData[0].lat));
          setLongitude(parseFloat(nominatimData[0].lon));
        }
      }
    } catch (err) {
      console.error('Erro ao buscar CEP', err);
    }
  };

  const abrirModalCriacao = () => {
    fecharModal();
    setHorarios(DEFAULT_HORARIOS);
    setModalOpen(true);
  };

  const abrirModalEdicao = (q: Quadra) => {
    novasFotosPreviews.forEach((url) => URL.revokeObjectURL(url));
    setEditandoId(q.id_quadra);
    setNome(q.nome);
    setTipoEsporte(q.tipoEsporte);
    setValorHora(q.valorHora.toString());
    setDescricao(q.descricao || '');
    setDataLimiteAgendamento(q.dataLimiteAgendamento || '');
    setFotosExistentes(q.fotos || []);
    setNovasFotos([]);
    setNovasFotosPreviews([]);
    setCep(q.cep || '');
    setLogradouro(q.logradouro || '');
    setBairro(q.bairro || '');
    setCidade(q.cidade || '');
    setEstado(q.estado || '');
    setLatitude(q.latitude);
    setLongitude(q.longitude);

    if (q.disponibilidades && q.disponibilidades.length > 0) {
      const novosHorarios: HorariosPorDia = {
        MONDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        TUESDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        WEDNESDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        THURSDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        FRIDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        SATURDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
        SUNDAY: { ativo: false, horaInicio: '06:00', horaFim: '23:00' },
      };
      q.disponibilidades.forEach((d) => {
        if (novosHorarios[d.diaSemana]) {
          novosHorarios[d.diaSemana] = {
            ativo: true,
            horaInicio: d.horaInicio ? d.horaInicio.slice(0, 5) : '06:00',
            horaFim: d.horaFim ? d.horaFim.slice(0, 5) : '23:00',
          };
        }
      });
      setHorarios(novosHorarios);
    } else {
      setHorarios(DEFAULT_HORARIOS);
    }

    setModalOpen(true);
  };

  const fecharModal = () => {
    novasFotosPreviews.forEach((url) => URL.revokeObjectURL(url));
    setModalOpen(false);
    setEditandoId(null);
    setNome('');
    setValorHora('');
    setDescricao('');
    setDataLimiteAgendamento('');
    setFotosExistentes([]);
    setNovasFotos([]);
    setNovasFotosPreviews([]);
    setCep('');
    setLogradouro('');
    setBairro('');
    setCidade('');
    setEstado('');
    setLatitude(undefined);
    setLongitude(undefined);
    setHorarios(DEFAULT_HORARIOS);
  };

  const handleDiaToggle = (dia: DiaSemana) => {
    setHorarios((prev) => ({
      ...prev,
      [dia]: {
        ...prev[dia],
        ativo: !prev[dia].ativo,
      },
    }));
  };

  const handleHorarioChange = (dia: DiaSemana, field: 'horaInicio' | 'horaFim', value: string) => {
    setHorarios((prev) => ({
      ...prev,
      [dia]: {
        ...prev[dia],
        [field]: value,
      },
    }));
  };

  const copiarSegParaTodos = () => {
    const seg = horarios.MONDAY;
    setHorarios({
      MONDAY: { ...seg },
      TUESDAY: { ...seg },
      WEDNESDAY: { ...seg },
      THURSDAY: { ...seg },
      FRIDAY: { ...seg },
      SATURDAY: { ...seg },
      SUNDAY: { ...seg },
    });
  };

  const aplicarPadraoTodos = () => {
    setHorarios(DEFAULT_HORARIOS);
  };

  const abrirAgendaDoDia = async (dataIso: string) => {
    setDataSelecionada(dataIso);
    setQuadraSelecionadaAgendaId(quadraFiltroCalendarId);
    setStatusFiltroModal(statusFiltroCalendar);
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

  const handleBloquearSlot = (quadraId: number, data: string, horaInicio: string, horaFim: string) => {
    const quadra = minhasQuadras.find((q) => q.id_quadra === quadraId);
    if (!quadra) return;
    setBloqueioModalQuadra(quadra);
    setBloqueioData(data);
    setBloqueioHoraInicio(horaInicio);
    setBloqueioHoraFim(horaFim);
    setBloqueioMotivo('');
    carregarBloqueios(quadraId);
  };

  const handleDesbloquearSlot = async (bloqueioId: number) => {
    setConfirmModal({
      isOpen: true,
      title: 'Desbloquear Horário',
      description: 'Deseja realmente remover este bloqueio de horário?',
      isDestructive: true,
      confirmLabel: 'Sim, desbloquear',
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        try {
          let quadraIdEncontrada: number | null = null;
          for (const [qIdStr, bList] of Object.entries(mapaBloqueiosPorQuadra)) {
            if (bList.some((b) => b.id === bloqueioId)) {
              quadraIdEncontrada = Number(qIdStr);
              break;
            }
          }
          if (!quadraIdEncontrada) return;
          await bloqueioApi.remover(quadraIdEncontrada, bloqueioId);
          setFeedback({ type: 'success', message: 'Horário desbloqueado com sucesso!' });
          await carregarDados();
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Erro ao remover bloqueio.' });
        }
      },
    });
  };

  const handleAbrirAgendamentoDetalhe = (ag: Agendamento) => {
    const dataIso = ag.dataHoraInicio.split('T')[0];
    abrirAgendaDoDia(dataIso);
    setQuadraSelecionadaAgendaId(ag.quadraId);
    setHighlightedAgendamentoId(ag.id_agendamento);
  };

  const abrirGerenciamentoBloqueios = async (q: Quadra) => {
    setBloqueioModalQuadra(q);
    setBloqueioData(new Date().toISOString().split('T')[0]);
    setBloqueioHoraInicio('');
    setBloqueioHoraFim('');
    setBloqueioMotivo('');
    await carregarBloqueios(q.id_quadra);
  };

  const carregarBloqueios = async (quadraId: number) => {
    setLoadingBloqueios(true);
    try {
      const data = await bloqueioApi.listar(quadraId);
      setBloqueiosQuadra(data);
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Erro ao carregar bloqueios.' });
    } finally {
      setLoadingBloqueios(false);
    }
  };

  const executarCriacaoBloqueio = async (substituirDiaInteiro: boolean = false) => {
    if (!bloqueioModalQuadra) return;

    try {
      await bloqueioApi.criar(bloqueioModalQuadra.id_quadra, {
        data: bloqueioData,
        horaInicio: bloqueioHoraInicio ? `${bloqueioHoraInicio}:00` : undefined,
        horaFim: bloqueioHoraFim ? `${bloqueioHoraFim}:00` : undefined,
        motivo: bloqueioMotivo || undefined,
        substituirDiaInteiro,
      });

      setFeedback({
        type: 'success',
        message: substituirDiaInteiro
          ? 'Bloqueio do dia todo substituído pelo horário específico com sucesso!'
          : 'Bloqueio adicionado com sucesso!',
      });
      setBloqueioHoraInicio('');
      setBloqueioHoraFim('');
      setBloqueioMotivo('');
      await carregarBloqueios(bloqueioModalQuadra.id_quadra);
      await carregarDados();
    } catch (err: any) {
      if (err.message && err.message.includes('DIA_INTEIRO_BLOQUEADO')) {
        // Exibir modal para o usuário confirmar a substituição
        setConfirmModal({
          isOpen: true,
          title: 'Substituir Bloqueio do Dia Todo',
          description: `A quadra "${bloqueioModalQuadra.nome}" já está bloqueada o dia todo em ${bloqueioData.split('-').reverse().join('/')}. Deseja desbloquear o restante do dia e manter bloqueado apenas o horário das ${bloqueioHoraInicio} às ${bloqueioHoraFim}?`,
          isDestructive: false,
          confirmLabel: 'Sim, desbloquear dia e bloquear horário',
          onConfirm: async () => {
            setConfirmModal((prev) => ({ ...prev, isOpen: false }));
            await executarCriacaoBloqueio(true);
          },
        });
        return;
      }
      setFeedback({ type: 'error', message: err.message || 'Erro ao adicionar bloqueio.' });
    }
  };

  const handleCriarBloqueio = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bloqueioModalQuadra) return;

    if (bloqueioHoraInicio && !bloqueioHoraFim) {
      setFeedback({ type: 'error', message: 'Informe também o horário de término do bloqueio.' });
      return;
    }
    if (!bloqueioHoraInicio && bloqueioHoraFim) {
      setFeedback({ type: 'error', message: 'Informe também o horário de início do bloqueio.' });
      return;
    }

    // Verificar previamente se já existe bloqueio de dia inteiro cadastrado na quadra nesta data
    const bloqueiosDaData = bloqueiosQuadra.filter((b) => b.data === bloqueioData);
    const temBloqueioDiaInteiro = bloqueiosDaData.some((b) => !b.horaInicio || !b.horaFim);

    if (bloqueioHoraInicio && bloqueioHoraFim && temBloqueioDiaInteiro) {
      setConfirmModal({
        isOpen: true,
        title: 'Substituir Bloqueio do Dia Todo',
        description: `A quadra "${bloqueioModalQuadra.nome}" já está bloqueada o dia todo em ${bloqueioData.split('-').reverse().join('/')}. Deseja desbloquear o restante do dia e manter bloqueado apenas o horário das ${bloqueioHoraInicio} às ${bloqueioHoraFim}?`,
        isDestructive: false,
        confirmLabel: 'Sim, desbloquear dia e bloquear horário',
        onConfirm: async () => {
          setConfirmModal((prev) => ({ ...prev, isOpen: false }));
          await executarCriacaoBloqueio(true);
        },
      });
      return;
    }

    await executarCriacaoBloqueio(false);
  };

  const handleRemoverBloqueio = async (bloqueioId: number) => {
    if (!bloqueioModalQuadra) return;
    try {
      await bloqueioApi.remover(bloqueioModalQuadra.id_quadra, bloqueioId);
      setFeedback({ type: 'success', message: 'Bloqueio removido com sucesso!' });
      await carregarBloqueios(bloqueioModalQuadra.id_quadra);
      await carregarDados();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Erro ao remover bloqueio.' });
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const selectedFiles = Array.from(e.target.files);
      const totalFotos = fotosExistentes.length + novasFotos.length + selectedFiles.length;
      if (totalFotos > 5) {
        setFeedback({ type: 'error', message: 'Você pode ter no máximo 5 fotos por quadra.' });
        return;
      }
      setNovasFotos((prev) => [...prev, ...selectedFiles]);
      const previews = selectedFiles.map((file) => URL.createObjectURL(file));
      setNovasFotosPreviews((prev) => [...prev, ...previews]);
    }
  };

  const removerNovaFoto = (index: number) => {
    const urlToRemove = novasFotosPreviews[index];
    if (urlToRemove) {
      URL.revokeObjectURL(urlToRemove);
    }
    setNovasFotos((prev) => prev.filter((_, i) => i !== index));
    setNovasFotosPreviews((prev) => prev.filter((_, i) => i !== index));
  };

  const removerFotoExistente = async (fotoUrl: string) => {
    if (!user || !editandoId) {
      setFotosExistentes((prev) => prev.filter((f) => f !== fotoUrl));
      return;
    }
    try {
      await quadraApi.removerFoto(editandoId, fotoUrl);
      setFotosExistentes((prev) => prev.filter((f) => f !== fotoUrl));
      setFeedback({ type: 'success', message: 'Foto removida com sucesso!' });
      await carregarDados();
    } catch (err: any) {
      setFeedback({ type: 'error', message: err.message || 'Erro ao remover foto.' });
    }
  };

  const handleSalvarQuadra = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    const acaoTexto = editandoId ? 'salvar as alterações da quadra' : 'cadastrar a nova quadra';
    const acaoTitulo = editandoId ? 'Confirmar Edição de Quadra' : 'Confirmar Cadastro de Quadra';

    setConfirmModal({
      isOpen: true,
      title: acaoTitulo,
      description: `Deseja realmente ${acaoTexto} "${nome}" com valor de R$ ${parseFloat(valorHora || '0').toFixed(2)}/hora?`,
      isDestructive: false,
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        setLoadingMessage(editandoId ? 'Atualizando dados e fotos da quadra...' : 'Cadastrando nova quadra e enviando fotos...');
        setLoading(true);
        setFeedback(null);

        try {
          const disponibilidades: DisponibilidadeDia[] = DIAS_SEMANA
            .filter((dia) => horarios[dia.key].ativo)
            .map((dia) => ({
              diaSemana: dia.key,
              horaInicio: `${horarios[dia.key].horaInicio}:00`,
              horaFim: `${horarios[dia.key].horaFim}:00`,
            }));

          const payload = {
            nome,
            tipoEsporte,
            valorHora: parseFloat(valorHora),
            descricao,
            dataLimiteAgendamento: dataLimiteAgendamento || undefined,
            fotos: fotosExistentes,
            cep,
            logradouro,
            bairro,
            cidade,
            estado,
            latitude,
            longitude,
            disponibilidades,
          };

          let quadraSalva: Quadra;
          if (editandoId) {
            quadraSalva = await quadraApi.editar(editandoId, payload);
          } else {
            quadraSalva = await quadraApi.cadastrar(payload);
          }

          // Se tiver fotos novas para enviar
          if (novasFotos.length > 0 && quadraSalva.id_quadra) {
            await quadraApi.uploadFotos(quadraSalva.id_quadra, novasFotos);
          }

          setFeedback({
            type: 'success',
            message: `Quadra "${nome}" ${editandoId ? 'atualizada' : 'cadastrada'} com sucesso!`,
          });

          fecharModal();
          await carregarDados();
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Falha ao salvar quadra.' });
        } finally {
          setLoading(false);
        }
      },
    });
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

  const cancelarAgendamento = (id: number) => {
    if (!user) return;
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
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Falha ao cancelar agendamento.' });
        } finally {
          setLoading(false);
        }
      },
    });
  };

  const handleValorChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.replace(/\D/g, '');
    if (value === '') {
      setValorHora('');
      return;
    }
    const decimalValue = (parseInt(value, 10) / 100).toFixed(2);
    setValorHora(decimalValue);
  };

  const [filtroAgendaAdmin, setFiltroAgendaAdmin] = useState<'ATIVOS' | 'CANCELADOS' | 'REALIZADOS'>('ATIVOS');

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Toast Feedback Unificado */}
      <FeedbackBanner feedback={feedback} onClose={() => setFeedback(null)} />

      {/* Header & Navegação por Abas */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-800 pb-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-white">Painel Administrativo</h1>
        </div>

        {/* Abas e Notificações */}
        <div className="flex items-center gap-4">
          
          {/* Sino de Notificações */}
          <div className="relative">
            <button
              onClick={() => setShowNotifications(!showNotifications)}
              className="relative p-2 text-zinc-400 hover:text-white transition rounded-full hover:bg-zinc-800"
            >
              <Bell className="w-5 h-5" />
              {notificacoes.filter(n => !n.lida).length > 0 && (
                <span className="absolute top-0 right-0 w-2.5 h-2.5 bg-white rounded-full ring-2 ring-zinc-950" />
              )}
            </button>

            {/* Popover de Notificações */}
            {showNotifications && (
              <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-zinc-950 border border-zinc-800 rounded-2xl shadow-2xl z-50 overflow-hidden flex flex-col">
                <div className="p-4 border-b border-zinc-850 flex justify-between items-center bg-zinc-950">
                  <div className="flex items-center gap-2">
                    <Bell className="w-4 h-4 text-emerald-400" />
                    <h3 className="text-sm font-bold text-white">Notificações</h3>
                  </div>
                  <button 
                    onClick={() => setShowNotifications(false)}
                    className="p-1 rounded-lg text-zinc-500 hover:text-white hover:bg-zinc-900 transition"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
                <div className="max-h-88 overflow-y-auto divide-y divide-zinc-850/60 scrollbar-thin">
                  {notificacoes.length === 0 ? (
                    <div className="p-8 text-center text-zinc-500 text-xs">
                      Nenhuma notificação por enquanto.
                    </div>
                  ) : (
                    notificacoes.map((notif) => (
                      <div 
                        key={notif.id} 
                        className={`p-4 flex flex-col gap-2.5 transition ${
                          notif.lida ? 'bg-zinc-950/40 opacity-60' : 'bg-zinc-900/40 hover:bg-zinc-900/70'
                        }`}
                      >
                        <p className="text-xs text-zinc-200 leading-relaxed whitespace-normal break-words">
                          {notif.mensagem}
                        </p>
                        <div className="flex justify-between items-center pt-1">
                          <span className="text-[10px] text-zinc-500 font-mono">
                            {formatarDataHora(notif.dataCriacao)}
                          </span>
                          {!notif.lida && (
                            <button 
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                lerNotificacao(notif.id);
                              }}
                              className="text-[10px] font-semibold text-emerald-400 hover:text-emerald-300 underline underline-offset-2 transition active:scale-95"
                            >
                              Marcar como lida
                            </button>
                          )}
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="flex bg-zinc-900/90 p-1 rounded-xl border border-zinc-800">
            <button
              onClick={() => setActiveTab('dashboard')}
              className={`flex items-center gap-2 px-3.5 py-2 text-xs font-semibold rounded-lg transition active:scale-[0.98] ${
                activeTab === 'dashboard'
                  ? 'bg-white text-zinc-950 shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              <LayoutDashboard className="w-4 h-4" />
              <span>Agenda & Métricas</span>
            </button>
            <button
              onClick={() => setActiveTab('quadras')}
              className={`flex items-center gap-2 px-3.5 py-2 text-xs font-semibold rounded-lg transition active:scale-[0.98] ${
                activeTab === 'quadras'
                  ? 'bg-white text-zinc-950 shadow-sm'
                  : 'text-zinc-400 hover:text-white'
              }`}
            >
              <Settings2 className="w-4 h-4" />
              <span>Gestão de Quadras</span>
            </button>
            {isMasterAdmin && (
              <button
                onClick={() => setActiveTab('usuarios')}
                className={`flex items-center gap-2 px-3.5 py-2 text-xs font-semibold rounded-lg transition active:scale-[0.98] ${
                  activeTab === 'usuarios'
                    ? 'bg-white text-zinc-950 shadow-sm'
                    : 'text-zinc-400 hover:text-white'
                }`}
              >
                <Users className="w-4 h-4" />
                <span>Gestão de Usuários</span>
              </button>
            )}
          </div>
        </div>
      </div>

      {activeTab === 'dashboard' ? (
        <div className="space-y-8">
          {/* Métricas / KPIs */}
          <AdminMetricsGrid metricas={metricas} />

          {/* Próximas Partidas de Hoje (Próximas 4 horas) */}
          <UpcomingMatchesBar
            agendamentosAdmin={agendamentosAdmin}
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
              const hoje = new Date().toISOString().split('T')[0];
              setDataSelecionada(hoje);
              setCurrentMonthDate(new Date());
            }}
          />

          {/* Alternância entre Grade Diária e Calendário Mensal */}
          {viewMode === 'TIMELINE' ? (
            <AdminDailyTimelineGrid
              dataSelecionada={dataSelecionada}
              minhasQuadras={minhasQuadras}
              agendamentosAdmin={agendamentosAdmin}
              mapaBloqueiosPorQuadra={mapaBloqueiosPorQuadra}
              quadraFiltroId={quadraFiltroCalendarId}
              statusFiltro={statusFiltroSchedule}
              buscaTermo={buscaTermoSchedule}
              onAbrirAgendamento={handleAbrirAgendamentoDetalhe}
              onBloquearSlot={handleBloquearSlot}
              onDesbloquear={handleDesbloquearSlot}
            />
          ) : (
            <CalendarOccupancy
              currentMonthDate={currentMonthDate}
              dataSelecionada={dataSelecionada}
              quadraFiltroCalendarId={quadraFiltroCalendarId}
              statusFiltroCalendar={
                statusFiltroSchedule === 'CONFIRMADOS'
                  ? 'AGENDADOS'
                  : statusFiltroSchedule === 'PENDENTES'
                  ? 'AGENDADOS'
                  : statusFiltroSchedule === 'BLOQUEADOS'
                  ? 'BLOQUEADOS'
                  : statusFiltroSchedule === 'LIVRES'
                  ? 'LIVRES'
                  : 'TODOS'
              }
              minhasQuadras={minhasQuadras}
              agendamentosAdmin={agendamentosAdmin}
              mapaBloqueiosPorQuadra={mapaBloqueiosPorQuadra}
              onQuadraFiltroChange={setQuadraFiltroCalendarId}
              onStatusFiltroChange={(novoStatus) => {
                setStatusFiltroCalendar(novoStatus);
                if (novoStatus === 'AGENDADOS') setStatusFiltroSchedule('CONFIRMADOS');
                else if (novoStatus === 'BLOQUEADOS') setStatusFiltroSchedule('BLOQUEADOS');
                else if (novoStatus === 'LIVRES') setStatusFiltroSchedule('LIVRES');
                else setStatusFiltroSchedule('TODOS');
              }}
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
          getAssetUrl={getAssetUrl}
        />
      ) : (
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
        loading={loading}
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
        onCepChange={(v) => {
          const formatted = v.replace(/\D/g, '').replace(/(\d{5})(\d)/, '$1-$2').substring(0, 9);
          setCep(formatted);
          if (formatted.length === 9) buscarCep(formatted);
        }}
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
        onClose={() => setBloqueioModalQuadra(null)}
        onDataChange={setBloqueioData}
        onHoraInicioChange={setBloqueioHoraInicio}
        onHoraFimChange={setBloqueioHoraFim}
        onMotivoChange={setBloqueioMotivo}
        onSubmit={handleCriarBloqueio}
        onRemoverBloqueio={handleRemoverBloqueio}
      />

      {/* Modal de Agenda do Dia */}
      <DayAgendaModal
        isOpen={modalAgendaDiaOpen}
        dataSelecionada={dataSelecionada}
        minhasQuadras={minhasQuadras}
        agendamentosAdmin={agendamentosAdmin}
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
