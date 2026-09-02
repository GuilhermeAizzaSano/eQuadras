import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { quadraApi, agendamentoApi, notificacaoApi } from '../api/apiClient';
import { Quadra, Agendamento, TipoEsporte, Notificacao } from '../types';
import { FeedbackBanner, EmptyState, Badge, ConfirmModal, LoadingOverlay, CourtDetailsModal } from '../components/ui';
import {
  PlusCircle,
  Power,
  Calendar as CalendarIcon,
  ShieldCheck,
  Edit2,
  Trash2,
  TrendingUp,
  DollarSign,
  Users,
  Clock,
  LayoutDashboard,
  Settings2,
  ChevronLeft,
  ChevronRight,
  X,
  Bell,
  Phone,
  Info,
  Upload
} from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const { user, token } = useAuth();
  const [minhasQuadras, setMinhasQuadras] = useState<Quadra[]>([]);
  const [agendamentosAdmin, setAgendamentosAdmin] = useState<Agendamento[]>([]);
  const [quadraDetalhes, setQuadraDetalhes] = useState<Quadra | null>(null);
  
  // Notificações SSE
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const eventSourceRef = useRef<EventSource | null>(null);
  
  // Controle de Abas
  const [activeTab, setActiveTab] = useState<'dashboard' | 'quadras'>('dashboard');

  // Modal de Criar / Editar Quadra
  const [modalOpen, setModalOpen] = useState(false);
  const [editandoId, setEditandoId] = useState<number | null>(null);
  const [nome, setNome] = useState('');
  const [tipoEsporte, setTipoEsporte] = useState<TipoEsporte>('FUTEBOL');
  const [valorHora, setValorHora] = useState('');
  const [descricao, setDescricao] = useState('');
  const [fotosExistentes, setFotosExistentes] = useState<string[]>([]);
  const [novasFotos, setNovasFotos] = useState<File[]>([]);
  const [novasFotosPreviews, setNovasFotosPreviews] = useState<string[]>([]);
  
  // Endereço
  const [cep, setCep] = useState('');
  const [logradouro, setLogradouro] = useState('');
  const [bairro, setBairro] = useState('');
  const [cidade, setCidade] = useState('');
  const [estado, setEstado] = useState('');
  const [latitude, setLatitude] = useState<number | undefined>();
  const [longitude, setLongitude] = useState<number | undefined>();

  // Controle do Calendário Mensal
  const [currentMonthDate, setCurrentMonthDate] = useState<Date>(() => new Date());
  const [dataSelecionada, setDataSelecionada] = useState<string>(() => {
    return new Date().toISOString().split('T')[0];
  });

  // Intervalo de tick para atualizar contadores de tempo real
  const [agora, setAgora] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);

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
    onConfirm: () => void;
  }>({
    isOpen: false,
    title: '',
    description: '',
    isDestructive: false,
    onConfirm: () => {},
  });

  useEffect(() => {
    carregarDados();
    carregarNotificacoes();

    if (user) {
      // Setup SSE for real-time notifications
      const streamUrl = token
        ? `http://localhost:8080/notificacoes/stream?token=${token}`
        : `http://localhost:8080/notificacoes/stream`;
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
      const agendamentos = await agendamentoApi.listar();
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
        agendamentoApi.listar(),
      ]);
      setMinhasQuadras(quadras);
      setAgendamentosAdmin(agendamentos);
    } catch (err: any) {
      console.error(err);
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

  // Geração dos Dias do Mês para o Calendário
  const calendarDays = useMemo(() => {
    const year = currentMonthDate.getFullYear();
    const month = currentMonthDate.getMonth();

    const firstDayIndex = new Date(year, month, 1).getDay(); // 0 = Domingo
    const totalDaysInMonth = new Date(year, month + 1, 0).getDate();
    const totalDaysPrevMonth = new Date(year, month, 0).getDate();

    const days = [];

    // Dias do mês anterior para preencher a primeira semana
    for (let i = firstDayIndex - 1; i >= 0; i--) {
      const dayNum = totalDaysPrevMonth - i;
      const d = new Date(year, month - 1, dayNum);
      const iso = d.toISOString().split('T')[0];
      days.push({
        dayNum,
        iso,
        isCurrentMonth: false,
        isToday: false,
        count: 0,
      });
    }

    const hojeIso = new Date().toISOString().split('T')[0];

    // Dias do mês atual
    for (let d = 1; d <= totalDaysInMonth; d++) {
      const iso = `${year}-${String(month + 1).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
      
      const count = agendamentosAdmin.filter(
        (a) => a.dataHoraInicio.startsWith(iso) && a.status !== 'CANCELADO'
      ).length;

      days.push({
        dayNum: d,
        iso,
        isCurrentMonth: true,
        isToday: iso === hojeIso,
        count,
      });
    }

    // Dias do próximo mês para completar a grade
    const remaining = (7 - (days.length % 7)) % 7;
    for (let d = 1; d <= remaining; d++) {
      const nextDate = new Date(year, month + 1, d);
      const iso = nextDate.toISOString().split('T')[0];
      days.push({
        dayNum: d,
        iso,
        isCurrentMonth: false,
        isToday: false,
        count: 0,
      });
    }

    return days;
  }, [currentMonthDate, agendamentosAdmin]);

  const mesAnoExtenso = useMemo(() => {
    const meses = [
      'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
      'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
    ];
    return `${meses[currentMonthDate.getMonth()]} de ${currentMonthDate.getFullYear()}`;
  }, [currentMonthDate]);

  const mudarMes = (offset: number) => {
    setCurrentMonthDate((prev) => new Date(prev.getFullYear(), prev.getMonth() + offset, 1));
  };

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

  const abrirModalCadastro = () => {
    fecharModal();
    setModalOpen(true);
  };

  const iniciarEdicao = (q: Quadra) => {
    setEditandoId(q.id_quadra);
    setNome(q.nome);
    setTipoEsporte(q.tipoEsporte);
    setValorHora(q.valorHora.toString());
    setDescricao(q.descricao || '');
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
    setModalOpen(true);
  };

  const fecharModal = () => {
    setModalOpen(false);
    setEditandoId(null);
    setNome('');
    setValorHora('');
    setDescricao('');
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
          const payload = {
            nome,
            tipoEsporte,
            valorHora: parseFloat(valorHora),
            descricao,
            fotos: fotosExistentes,
            cep,
            logradouro,
            bairro,
            cidade,
            estado,
            latitude,
            longitude
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

  const toggleStatus = async (quadra: Quadra) => {
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

  // Todos os agendamentos do dia selecionado
  const agendamentosDoDia = useMemo(() => {
    return agendamentosAdmin
      .filter((a) => a.dataHoraInicio.startsWith(dataSelecionada))
      .sort((a, b) => a.dataHoraInicio.localeCompare(b.dataHoraInicio));
  }, [agendamentosAdmin, dataSelecionada]);

  // Agendamentos filtrados pela aba de status do dia
  const agendamentosDoDiaFiltrados = useMemo(() => {
    const agoraLocal = new Date();
    return agendamentosDoDia.filter((ag) => {
      const dataFim = new Date(ag.dataHoraFim);
      const isCancelado = ag.status === 'CANCELADO';
      const isPassado = dataFim < agoraLocal;

      if (filtroAgendaAdmin === 'CANCELADOS') {
        return isCancelado;
      }
      if (filtroAgendaAdmin === 'REALIZADOS') {
        return !isCancelado && isPassado;
      }
      // 'ATIVOS' (Futuros / Em andamento não cancelados)
      return !isCancelado && !isPassado;
    });
  }, [agendamentosDoDia, filtroAgendaAdmin]);

  // Contadores para as abas da agenda do dia
  const contadoresAgendaDia = useMemo(() => {
    const agoraLocal = new Date();
    let ativos = 0;
    let cancelados = 0;
    let realizados = 0;

    agendamentosDoDia.forEach((ag) => {
      const dataFim = new Date(ag.dataHoraFim);
      const isCancelado = ag.status === 'CANCELADO';
      const isPassado = dataFim < agoraLocal;

      if (isCancelado) {
        cancelados++;
      } else if (isPassado) {
        realizados++;
      } else {
        ativos++;
      }
    });

    return { ativos, cancelados, realizados };
  }, [agendamentosDoDia]);

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
          </div>
        </div>
      </div>

      {activeTab === 'dashboard' ? (
        /* ABA DASHBOARD & CALENDÁRIO MENSAL */
        <div className="space-y-8">
          {/* Métricas / KPIs */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="bg-zinc-950 border border-zinc-850 p-5 rounded-2xl shadow-xl space-y-2">
              <div className="flex items-center justify-between text-zinc-400 text-[11px] uppercase tracking-wider font-semibold">
                <span>Receita Acumulada</span>
                <DollarSign className="w-4 h-4 text-emerald-400" />
              </div>
              <div className="text-2xl font-extrabold text-white font-mono">
                R$ {metricas.faturamentoTotal.toFixed(2)}
              </div>
            </div>

            <div className="bg-zinc-950 border border-zinc-850 p-5 rounded-2xl shadow-xl space-y-2">
              <div className="flex items-center justify-between text-zinc-400 text-[11px] uppercase tracking-wider font-semibold">
                <span>Jogos de Hoje</span>
                <TrendingUp className="w-4 h-4 text-emerald-400" />
              </div>
              <div className="text-2xl font-extrabold text-white">
                {metricas.reservasHoje}
              </div>
            </div>

            <div className="bg-zinc-950 border border-zinc-850 p-5 rounded-2xl shadow-xl space-y-2">
              <div className="flex items-center justify-between text-zinc-400 text-[11px] uppercase tracking-wider font-semibold">
                <span>Total de Reservas</span>
                <Users className="w-4 h-4 text-zinc-400" />
              </div>
              <div className="text-2xl font-extrabold text-white">
                {metricas.totalReservas}
              </div>
            </div>

            <div className="bg-zinc-950 border border-zinc-850 p-5 rounded-2xl shadow-xl space-y-2">
              <div className="flex items-center justify-between text-zinc-400 text-[11px] uppercase tracking-wider font-semibold">
                <span>Quadras Ativas</span>
                <ShieldCheck className="w-4 h-4 text-zinc-300" />
              </div>
              <div className="text-2xl font-extrabold text-white">
                {metricas.quadrasAtivas} <span className="text-xs font-normal text-zinc-500">/ {metricas.totalQuadras}</span>
              </div>
            </div>
          </div>

          {/* Grid: Calendário Mensal (Esquerda) + Agenda do Dia (Direita) */}
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
            {/* Coluna Calendário Mensal */}
            <div className="lg:col-span-7 bg-zinc-950 border border-zinc-850 rounded-2xl p-6 shadow-xl space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <CalendarIcon className="w-5 h-5 text-zinc-300" />
                    Calendário Mensal
                  </h2>
                </div>

                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-white px-2">{mesAnoExtenso}</span>
                  <div className="flex items-center bg-zinc-900 border border-zinc-800 rounded-xl p-0.5">
                    <button
                      onClick={() => mudarMes(-1)}
                      className="p-1.5 hover:bg-zinc-800 rounded-lg text-zinc-400 hover:text-white transition"
                      title="Mês anterior"
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button
                      onClick={() => mudarMes(1)}
                      className="p-1.5 hover:bg-zinc-800 rounded-lg text-zinc-400 hover:text-white transition"
                      title="Próximo mês"
                    >
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>

              {/* Cabeçalho dos Dias da Semana */}
              <div className="grid grid-cols-7 gap-1 text-center text-[11px] font-semibold text-zinc-400 uppercase tracking-wider pb-2 border-b border-zinc-850">
                <span>Dom</span>
                <span>Seg</span>
                <span>Ter</span>
                <span>Qua</span>
                <span>Qui</span>
                <span>Sex</span>
                <span>Sáb</span>
              </div>

              {/* Grade de Dias do Mês */}
              <div className="grid grid-cols-7 gap-1.5">
                {calendarDays.map((day, idx) => {
                  const isSelected = dataSelecionada === day.iso;
                  return (
                    <button
                      key={idx}
                      onClick={() => setDataSelecionada(day.iso)}
                      className={`min-h-[64px] p-2 rounded-xl border flex flex-col justify-between items-start transition-all active:scale-[0.98] ${
                        !day.isCurrentMonth
                          ? 'opacity-25 bg-zinc-950 border-zinc-900 text-zinc-600 cursor-default'
                          : isSelected
                          ? 'bg-white text-zinc-950 border-white shadow-xl ring-2 ring-white z-10'
                          : 'bg-zinc-900/50 border-zinc-850 text-zinc-300 hover:border-zinc-700 hover:bg-zinc-900'
                      }`}
                    >
                      <div className="w-full flex items-center justify-between">
                        <span
                          className={`text-xs font-bold ${
                            day.isToday && !isSelected
                              ? 'w-5 h-5 rounded-full bg-white text-zinc-950 flex items-center justify-center font-extrabold'
                              : ''
                          }`}
                        >
                          {day.dayNum}
                        </span>

                        {day.count > 0 && (
                          <Badge variant={isSelected ? 'active' : 'success'}>
                            {day.count}
                          </Badge>
                        )}
                      </div>

                      {day.count > 0 && day.isCurrentMonth && (
                        <div
                          className={`text-[11px] font-mono truncate w-full mt-1 ${
                            isSelected ? 'text-zinc-900 font-bold' : 'text-emerald-400'
                          }`}
                        >
                          {day.count === 1 ? '1 reserva' : `${day.count} reservas`}
                        </div>
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Coluna Agenda do Dia Selecionado */}
            <div className="lg:col-span-5 bg-zinc-950 border border-zinc-850 rounded-2xl p-6 shadow-xl space-y-4 flex flex-col justify-between">
              <div className="space-y-4">
                <div className="flex items-center justify-between border-b border-zinc-850 pb-3">
                  <div>
                    <h3 className="text-base font-bold text-white flex items-center gap-2">
                      <Clock className="w-4 h-4 text-zinc-300" />
                      Agenda do Dia
                    </h3>
                    <p className="text-xs text-zinc-400 mt-0.5">
                      Data: <strong className="text-white font-mono">{dataSelecionada.split('-').reverse().join('/')}</strong>
                    </p>
                  </div>
                  <Badge variant="neutral">
                    {agendamentosDoDia.length} {agendamentosDoDia.length === 1 ? 'total' : 'totais'}
                  </Badge>
                </div>

                {/* Abas de Filtro: Ativos / Realizados / Cancelados */}
                <div className="flex bg-zinc-900/90 p-1 rounded-xl border border-zinc-800 text-xs">
                  <button
                    onClick={() => setFiltroAgendaAdmin('ATIVOS')}
                    className={`flex-1 py-1.5 px-2 font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 active:scale-[0.98] ${
                      filtroAgendaAdmin === 'ATIVOS'
                        ? 'bg-white text-zinc-950 shadow-sm'
                        : 'text-zinc-400 hover:text-white'
                    }`}
                  >
                    <span>Ativos</span>
                    <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold ${
                      filtroAgendaAdmin === 'ATIVOS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-400'
                    }`}>
                      {contadoresAgendaDia.ativos}
                    </span>
                  </button>

                  <button
                    onClick={() => setFiltroAgendaAdmin('REALIZADOS')}
                    className={`flex-1 py-1.5 px-2 font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 active:scale-[0.98] ${
                      filtroAgendaAdmin === 'REALIZADOS'
                        ? 'bg-white text-zinc-950 shadow-sm'
                        : 'text-zinc-400 hover:text-white'
                    }`}
                  >
                    <span>Realizados</span>
                    <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold ${
                      filtroAgendaAdmin === 'REALIZADOS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-400'
                    }`}>
                      {contadoresAgendaDia.realizados}
                    </span>
                  </button>

                  <button
                    onClick={() => setFiltroAgendaAdmin('CANCELADOS')}
                    className={`flex-1 py-1.5 px-2 font-semibold rounded-lg transition-all flex items-center justify-center gap-1.5 active:scale-[0.98] ${
                      filtroAgendaAdmin === 'CANCELADOS'
                        ? 'bg-white text-zinc-950 shadow-sm'
                        : 'text-zinc-400 hover:text-white'
                    }`}
                  >
                    <span>Cancelados</span>
                    <span className={`text-[10px] px-1.5 py-0.2 rounded-full font-mono font-bold ${
                      filtroAgendaAdmin === 'CANCELADOS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-400'
                    }`}>
                      {contadoresAgendaDia.cancelados}
                    </span>
                  </button>
                </div>

                {agendamentosDoDiaFiltrados.length === 0 ? (
                  <EmptyState
                    icon={CalendarIcon}
                    title={
                      filtroAgendaAdmin === 'ATIVOS'
                        ? 'Nenhum jogo ativo agendado para este dia'
                        : filtroAgendaAdmin === 'REALIZADOS'
                        ? 'Nenhum jogo finalizado para este dia'
                        : 'Nenhum jogo cancelado para este dia'
                    }
                    description="Selecione outro dia no calendário ou alterne a aba de status acima."
                    className="py-14"
                  />
                ) : (
                  <div className="space-y-3 max-h-[460px] overflow-y-auto pr-1 scrollbar-thin">
                    {agendamentosDoDiaFiltrados.map((ag) => {
                      const isCancelado = ag.status === 'CANCELADO';
                      const isPassado = new Date(ag.dataHoraFim) < new Date();
                      const horaInicio = ag.dataHoraInicio.split('T')[1]?.substring(0, 5);
                      const horaFim = ag.dataHoraFim.split('T')[1]?.substring(0, 5);
                      const quadraCorrespondente = minhasQuadras.find((q) => q.id_quadra === ag.quadraId);

                      return (
                        <div
                          key={ag.id_agendamento}
                          className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-850 space-y-2.5 transition hover:border-zinc-700"
                        >
                          <div className="flex items-start justify-between gap-3">
                            <div className="space-y-1">
                              {/* Quadra Alugada */}
                              <div className="text-sm font-bold text-white flex items-center gap-1.5">
                                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                                {ag.nomeQuadra}
                              </div>

                              <div className="text-xs text-zinc-400">
                                Atleta: <strong className="text-zinc-200">{ag.nomeUsuario}</strong>
                              </div>

                              {ag.telefoneUsuario && (
                                <div className="text-xs text-zinc-400 flex items-center gap-1.5 font-mono">
                                  <Phone className="w-3 h-3 text-zinc-500" />
                                  <span>{ag.telefoneUsuario}</span>
                                </div>
                              )}

                              {ag.status === 'PENDENTE' && !isPassado && (() => {
                                const tempo = getTempoRestantePix(ag.criadoEm);
                                return tempo ? (
                                  <div className="text-[11px] text-amber-400 font-mono flex items-center gap-1.5 mt-1 font-semibold">
                                    <Clock className="w-3 h-3 animate-pulse text-amber-400" />
                                    <span>Aguardando Pix ({tempo})</span>
                                  </div>
                                ) : (
                                  <div className="text-[11px] text-red-400 font-mono flex items-center gap-1.5 mt-1 font-semibold">
                                    <Clock className="w-3 h-3 text-red-400" />
                                    <span>Pix expirado</span>
                                  </div>
                                );
                              })()}
                            </div>

                            {/* Status e Botão Ver Quadra lado a lado fixos à direita */}
                            <div className="flex items-center gap-2 shrink-0">
                              {quadraCorrespondente && (
                                <button
                                  type="button"
                                  onClick={() => setQuadraDetalhes(quadraCorrespondente)}
                                  title="Ver fotos e informações completas desta quadra"
                                  className="p-1 rounded-lg bg-zinc-900 hover:bg-zinc-800 text-zinc-300 hover:text-white border border-zinc-800 hover:border-zinc-700 transition active:scale-95 flex items-center gap-1 text-[11px] font-semibold px-2.5 py-1"
                                >
                                  <Info className="w-3.5 h-3.5 text-emerald-400" />
                                  <span>Ver Quadra</span>
                                </button>
                              )}

                              <Badge variant={isCancelado ? 'outline' : isPassado ? 'neutral' : ag.status === 'PENDENTE' ? 'warning' : 'success'}>
                                {isCancelado ? 'CANCELADO' : isPassado ? 'REALIZADO' : ag.status}
                              </Badge>
                            </div>
                          </div>

                          <div className="flex items-center gap-2 text-xs text-zinc-300 font-mono">
                            <Clock className="w-3.5 h-3.5 text-zinc-400" />
                            <span>{horaInicio} às {horaFim}</span>
                          </div>

                          <div className="flex items-center justify-between pt-2 border-t border-zinc-850 text-xs">
                            <span className="text-zinc-300 font-mono font-semibold">R$ {ag.valorTotal.toFixed(2)}</span>
                            {!isCancelado && !isPassado && (
                              <button
                                onClick={() => cancelarAgendamento(ag.id_agendamento)}
                                className="text-xs text-red-400 hover:text-red-300 font-semibold transition underline underline-offset-2 active:scale-95"
                              >
                                Cancelar
                              </button>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* ABA GESTÃO DE QUADRAS */
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold tracking-tight text-white flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-zinc-300" />
                Minhas Quadras ({minhasQuadras.length})
              </h2>
            </div>

            {/* Botão primário com acento esmeralda */}
            <button
              onClick={abrirModalCadastro}
              className="flex items-center gap-2 bg-emerald-400 hover:bg-emerald-300 text-zinc-950 px-4 py-2.5 rounded-xl text-xs font-bold transition shadow-lg shadow-emerald-950/20 active:scale-[0.98]"
            >
              <PlusCircle className="w-4 h-4 text-zinc-950" />
              <span>Nova Quadra</span>
            </button>
          </div>

          {minhasQuadras.length === 0 ? (
            <EmptyState
              icon={ShieldCheck}
              title="Você ainda não possui quadras cadastradas"
              description="Cadastre sua estrutura esportiva para disponibilizá-la aos clientes."
              actionLabel="Cadastrar Primeira Quadra"
              onAction={abrirModalCadastro}
              className="py-16"
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {minhasQuadras.map((q) => (
                <div
                  key={q.id_quadra}
                  className="p-5 rounded-2xl bg-zinc-950 border border-zinc-850 shadow-xl flex flex-col justify-between space-y-4 hover:border-zinc-700 transition"
                >
                  <div className="space-y-2.5">
                    <div className="flex items-center justify-between">
                      <Badge variant="neutral">
                        {q.tipoEsporte.replace('_', ' ')}
                      </Badge>
                      <span
                        className={`w-2 h-2 rounded-full ${
                          q.ativa ? 'bg-emerald-400' : 'bg-zinc-700'
                        }`}
                      />
                    </div>

                    <h3 className="text-base font-bold text-white">{q.nome}</h3>
                    <div className="text-sm font-semibold text-zinc-300 font-mono">
                      R$ {q.valorHora.toFixed(2)} <span className="text-xs font-normal text-zinc-500">/ hora</span>
                    </div>
                  </div>

                  {/* Ações */}
                  <div className="pt-3.5 border-t border-zinc-850 flex items-center justify-between gap-2">
                    <button
                      onClick={() => toggleStatus(q)}
                      className={`px-3 py-1.5 rounded-lg border text-xs font-mono font-semibold flex items-center gap-1.5 transition active:scale-[0.98] ${
                        q.ativa
                          ? 'bg-emerald-950/40 border-emerald-500/30 text-emerald-400 hover:bg-emerald-900/40'
                          : 'bg-zinc-900 border-zinc-800 text-zinc-500 hover:text-white'
                      }`}
                      title={q.ativa ? 'Desativar quadra' : 'Ativar quadra'}
                    >
                      <Power className="w-3.5 h-3.5" />
                      <span>{q.ativa ? 'ATIVA' : 'INATIVA'}</span>
                    </button>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => setQuadraDetalhes(q)}
                        className="p-2 rounded-lg bg-zinc-900 hover:bg-zinc-850 border border-zinc-800 text-zinc-400 hover:text-white text-xs transition active:scale-[0.98]"
                        title="Visualizar detalhes e fotos da quadra"
                      >
                        <Info className="w-3.5 h-3.5 text-zinc-300" />
                      </button>

                      <button
                        onClick={() => iniciarEdicao(q)}
                        className="px-3 py-1.5 rounded-lg bg-zinc-900 hover:bg-zinc-850 border border-zinc-800 text-zinc-300 hover:text-white text-xs font-medium flex items-center gap-1.5 transition active:scale-[0.98]"
                        title="Editar dados da quadra"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                        <span>Editar</span>
                      </button>

                      <button
                        onClick={() => handleExcluirQuadra(q)}
                        className="p-2 rounded-lg bg-red-950/30 hover:bg-red-950/60 border border-red-900/40 text-red-400 hover:text-red-300 text-xs transition active:scale-[0.98]"
                        title="Excluir quadra"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* MODAL DE CADASTRO / EDIÇÃO DE QUADRA */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
          <div className="bg-zinc-950 border border-zinc-800 rounded-2xl w-full max-w-md p-6 shadow-2xl space-y-6">
            <div className="flex items-center justify-between border-b border-zinc-800 pb-3">
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                {editandoId ? (
                  <>
                    <Edit2 className="w-4 h-4 text-white" />
                    Editar Quadra
                  </>
                ) : (
                  <>
                    <PlusCircle className="w-4 h-4 text-white" />
                    Nova Quadra
                  </>
                )}
              </h2>
              <button
                onClick={fecharModal}
                className="p-1 rounded-lg text-zinc-500 hover:text-white transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSalvarQuadra} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                  Nome da Quadra
                </label>
                <input
                  type="text"
                  required
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  placeholder="Ex: Arena Beach 01"
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                  Modalidade Esportiva
                </label>
                <select
                  value={tipoEsporte}
                  onChange={(e) => setTipoEsporte(e.target.value as TipoEsporte)}
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
                >
                  <option value="FUTEBOL">FUTEBOL</option>
                  <option value="FUTSAL">FUTSAL</option>
                  <option value="VOLEI">VOLEI</option>
                  <option value="BEACH_TENNIS">BEACH_TENNIS</option>
                  <option value="BASQUETE">BASQUETE</option>
                  <option value="TENIS">TENIS</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                  Valor por Hora (R$)
                </label>
                <input
                  type="text"
                  required
                  value={valorHora}
                  onChange={handleValorChange}
                  placeholder="0.00"
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                  Descrição & Informações da Quadra
                </label>
                <textarea
                  rows={3}
                  value={descricao}
                  onChange={(e) => setDescricao(e.target.value)}
                  placeholder="Ex: Quadra de saibro coberta, com iluminação LED de alta potência, vestiários com ducha quente e arquibancada."
                  className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition resize-none"
                />
              </div>

              {/* Seção de Fotos da Quadra */}
              <div className="space-y-2.5">
                <div className="flex items-center justify-between">
                  <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400">
                    Fotos da Quadra (Máx. 5)
                  </label>
                  <span className="text-[11px] font-mono text-zinc-500">
                    {fotosExistentes.length + novasFotos.length} / 5 fotos
                  </span>
                </div>

                {/* Previews de Fotos Existentes e Novas */}
                <div className="grid grid-cols-5 gap-2">
                  {fotosExistentes.map((url, idx) => (
                    <div key={`existente-${idx}`} className="relative aspect-square rounded-xl overflow-hidden border border-zinc-800 bg-zinc-900 group">
                      <img
                        src={url.startsWith('http') ? url : `http://localhost:8080${url}`}
                        alt="Foto da quadra"
                        className="w-full h-full object-cover"
                      />
                      <button
                        type="button"
                        onClick={() => removerFotoExistente(url)}
                        title="Remover foto"
                        className="absolute inset-0 bg-red-950/80 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <Trash2 className="w-4 h-4 text-red-300" />
                      </button>
                    </div>
                  ))}

                  {novasFotosPreviews.map((preview, idx) => (
                    <div key={`nova-${idx}`} className="relative aspect-square rounded-xl overflow-hidden border border-emerald-500/50 bg-zinc-900 group">
                      <img
                        src={preview}
                        alt="Nova foto"
                        className="w-full h-full object-cover"
                      />
                      <button
                        type="button"
                        onClick={() => removerNovaFoto(idx)}
                        title="Remover foto selecionada"
                        className="absolute inset-0 bg-red-950/80 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        <Trash2 className="w-4 h-4 text-red-300" />
                      </button>
                    </div>
                  ))}

                  {/* Botão para adicionar foto */}
                  {fotosExistentes.length + novasFotos.length < 5 && (
                    <label className="aspect-square rounded-xl border border-dashed border-zinc-750 hover:border-emerald-400/80 bg-zinc-900/40 hover:bg-zinc-900 flex flex-col items-center justify-center cursor-pointer transition text-zinc-500 hover:text-emerald-400 group">
                      <Upload className="w-4 h-4 transition-transform group-hover:-translate-y-0.5" />
                      <span className="text-[10px] mt-1 font-medium">Adicionar</span>
                      <input
                        type="file"
                        multiple
                        accept="image/jpeg,image/png,image/webp"
                        className="hidden"
                        onChange={handleFileChange}
                      />
                    </label>
                  )}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                    CEP
                  </label>
                  <input
                    type="text"
                    required
                    value={cep}
                    onChange={(e) => {
                      const value = e.target.value.replace(/\D/g, '').replace(/(\d{5})(\d)/, '$1-$2').substring(0, 9);
                      setCep(value);
                      if (value.length === 9) buscarCep(value);
                    }}
                    placeholder="XXXXX-XXX"
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
                  />
                </div>

                <div className="col-span-2">
                  <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                    Logradouro
                  </label>
                  <input
                    type="text"
                    required
                    value={logradouro}
                    onChange={(e) => setLogradouro(e.target.value)}
                    placeholder="Rua, Avenida..."
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                    Bairro
                  </label>
                  <input
                    type="text"
                    required
                    value={bairro}
                    onChange={(e) => setBairro(e.target.value)}
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                    Cidade
                  </label>
                  <input
                    type="text"
                    required
                    value={cidade}
                    onChange={(e) => setCidade(e.target.value)}
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
                  />
                </div>

                <div className="col-span-2">
                  <label className="block text-xs font-semibold uppercase tracking-wider text-zinc-400 mb-1.5">
                    Estado (UF)
                  </label>
                  <input
                    type="text"
                    required
                    value={estado}
                    maxLength={2}
                    onChange={(e) => setEstado(e.target.value.toUpperCase())}
                    placeholder="SP"
                    className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-4 py-2.5 text-sm text-white focus:outline-none focus:border-white transition"
                  />
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="button"
                  onClick={fecharModal}
                  className="flex-1 py-2.5 rounded-xl border border-zinc-800 text-zinc-400 hover:text-white text-xs font-semibold transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 bg-white hover:bg-zinc-200 text-black font-semibold py-2.5 rounded-xl text-xs transition shadow-lg disabled:opacity-50"
                >
                  {loading ? 'Salvando...' : editandoId ? 'Salvar Alterações' : 'Cadastrar Quadra'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

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
        confirmLabel={confirmModal.isDestructive ? 'Sim, Confirmar' : 'Confirmar'}
        cancelLabel="Voltar"
        onConfirm={confirmModal.onConfirm}
        onCancel={() => setConfirmModal((prev) => ({ ...prev, isOpen: false }))}
      />

      {/* Loading Overlay Global Esmaecido */}
      <LoadingOverlay isLoading={loading} message={loadingMessage} />
    </div>
  );
};
