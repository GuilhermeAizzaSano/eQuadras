import React, { useState, useEffect, useMemo } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { quadraApi, agendamentoApi, bloqueioApi, getAssetUrl } from '../api/apiClient';
import { Quadra, HorarioDisponivel, Agendamento, DiaSemana, BloqueioHorario } from '../types';
import { FeedbackBanner, EmptyState, Badge, ConfirmModal, ModalPix, LoadingOverlay, CourtDetailsModal, BookingModal } from '../components/ui';
import { Calendar as CalendarIcon, Clock, MapPin, QrCode, Info, ChevronDown, History, Loader2 } from 'lucide-react';

const ESPORTES = ['TODOS', 'FUTEBOL', 'BEACH_TENNIS', 'TENIS', 'FUTSAL', 'VOLEI', 'BASQUETE'] as const;

const DIA_SEMANA_MAP: DiaSemana[] = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
];

export const ClientDashboard: React.FC = () => {
  const { user } = useAuth();
  const [abaPrincipal, setAbaPrincipal] = useState<'QUADRAS' | 'RESERVAS'>('QUADRAS');
  const [quadras, setQuadras] = useState<Quadra[]>([]);
  const [selectedQuadra, setSelectedQuadra] = useState<number | null>(null);
  const [isBookingModalOpen, setIsBookingModalOpen] = useState(false);
  const [quadraDetalhes, setQuadraDetalhes] = useState<Quadra | null>(null);
  const [filtroEsporte, setFiltroEsporte] = useState<string>('TODOS');
  
  const getHojeIsoLocal = () => {
    const d = new Date();
    const ano = d.getFullYear();
    const mes = String(d.getMonth() + 1).padStart(2, '0');
    const dia = String(d.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  };

  const [dataSelecionada, setDataSelecionada] = useState<string>(() => getHojeIsoLocal());
  
  const [horarios, setHorarios] = useState<HorarioDisponivel[]>([]);
  const [slotsSelecionados, setSlotsSelecionados] = useState<HorarioDisponivel[]>([]);
  const [meusAgendamentos, setMeusAgendamentos] = useState<Agendamento[]>([]);
  const [historicoCarregado, setHistoricoCarregado] = useState(false);
  const [carregandoHistorico, setCarregandoHistorico] = useState(false);
  const [bloqueiosQuadra, setBloqueiosQuadra] = useState<BloqueioHorario[]>([]);
  // Aba de filtro de status das reservas
  const [filtroStatusReservas, setFiltroStatusReservas] = useState<'ATIVOS' | 'CANCELADOS' | 'REALIZADOS'>('ATIVOS');

  const quadraAtual = useMemo(() => quadras.find((q) => q.id_quadra === selectedQuadra), [quadras, selectedQuadra]);

  useEffect(() => {
    if (selectedQuadra) {
      bloqueioApi.listar(selectedQuadra)
        .then(setBloqueiosQuadra)
        .catch(() => setBloqueiosQuadra([]));
    } else {
      setBloqueiosQuadra([]);
    }
  }, [selectedQuadra]);

  // Filtragem dinâmica de agendamentos do cliente
  const agendamentosFiltrados = useMemo(() => {
    const agoraLocal = new Date();
    return meusAgendamentos.filter((ag) => {
      const dataFim = new Date(ag.dataHoraFim);
      const isCancelado = ag.status === 'CANCELADO';
      const isPassado = dataFim < agoraLocal;

      if (filtroStatusReservas === 'CANCELADOS') {
        return isCancelado;
      }
      if (filtroStatusReservas === 'REALIZADOS') {
        return !isCancelado && isPassado;
      }
      // 'ATIVOS' (Futuros/Em andamento que não foram cancelados)
      return !isCancelado && !isPassado;
    });
  }, [meusAgendamentos, filtroStatusReservas]);

  // Contadores para as abas
  const contadoresReservas = useMemo(() => {
    const agoraLocal = new Date();
    let ativos = 0;
    let cancelados = 0;
    let realizados = 0;

    meusAgendamentos.forEach((ag) => {
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
  }, [meusAgendamentos]);

  const [loading, setLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState('Processando dados...');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Modal Pix
  const [agendamentoPixModal, setAgendamentoPixModal] = useState<Agendamento | null>(null);

  // Intervalo de tick para atualizar contadores de tempo real
  const [agora, setAgora] = useState(() => Date.now());
  useEffect(() => {
    const hasPendente = meusAgendamentos.some((a) => a.status === 'PENDENTE');
    if (!hasPendente) return;
    const timer = setInterval(() => setAgora(Date.now()), 1000);
    return () => clearInterval(timer);
  }, [meusAgendamentos]);

  const getTempoRestantePix = (criadoEm: string) => {
    const criadoMs = new Date(criadoEm).getTime();
    const expiraMs = criadoMs + 15 * 60 * 1000;
    const diff = Math.floor((expiraMs - agora) / 1000);
    if (diff <= 0) return null;
    const min = Math.floor(diff / 60);
    const seg = diff % 60;
    return `${String(min).padStart(2, '0')}:${String(seg).padStart(2, '0')}`;
  };

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

  const [cepBusca, setCepBusca] = useState('');

  // Gera os próximos 14 dias para o seletor visual considerando as disponibilidades da quadra selecionada
  const diasDisponiveis = useMemo(() => {
    const dias = [];
    const nomesDias = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];
    const nomesMeses = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
    
    for (let i = 0; i < 14; i++) {
      const d = new Date();
      d.setDate(d.getDate() + i);
      const ano = d.getFullYear();
      const mes = String(d.getMonth() + 1).padStart(2, '0');
      const dia = String(d.getDate()).padStart(2, '0');
      const iso = `${ano}-${mes}-${dia}`;
      const diaSemanaEnum = DIA_SEMANA_MAP[d.getDay()];

      let disponivel = true;
      let motivoIndisponibilidade: 'Encerrado' | 'Bloqueado' | 'Fechado' | undefined = undefined;

      // 1. Verifica funcionamento do dia da semana
      if (quadraAtual?.disponibilidades && quadraAtual.disponibilidades.length > 0) {
        const diaAberto = quadraAtual.disponibilidades.some((disp) => disp.diaSemana === diaSemanaEnum);
        if (!diaAberto) {
          disponivel = false;
          motivoIndisponibilidade = 'Fechado';
        }
      }

      // 2. Verifica dataLimiteAgendamento
      if (disponivel && quadraAtual?.dataLimiteAgendamento) {
        if (iso > quadraAtual.dataLimiteAgendamento) {
          disponivel = false;
          motivoIndisponibilidade = 'Encerrado';
        }
      }

      // 3. Verifica bloqueio de dia inteiro
      if (disponivel && bloqueiosQuadra && bloqueiosQuadra.length > 0) {
        const temBloqueioDiaInteiro = bloqueiosQuadra.some(
          (b) => b.data === iso && (!b.horaInicio || !b.horaFim)
        );
        if (temBloqueioDiaInteiro) {
          disponivel = false;
          motivoIndisponibilidade = 'Bloqueado';
        }
      }

      dias.push({
        iso,
        diaSemana: nomesDias[d.getDay()],
        diaMes: d.getDate(),
        mes: nomesMeses[d.getMonth()],
        isHoje: i === 0,
        disponivel,
        motivoIndisponibilidade,
      });
    }
    return dias;
  }, [quadraAtual, bloqueiosQuadra]);

  // Se o modal de agendamento estiver aberto e a data selecionada não for disponível, seleciona a primeira data válida
  useEffect(() => {
    if (isBookingModalOpen && diasDisponiveis.length > 0) {
      const diaAtualValido = diasDisponiveis.find((d) => d.iso === dataSelecionada)?.disponivel;
      if (diaAtualValido === false) {
        const primeiroValido = diasDisponiveis.find((d) => d.disponivel);
        if (primeiroValido) {
          setDataSelecionada(primeiroValido.iso);
        }
      }
    }
  }, [isBookingModalOpen, diasDisponiveis, dataSelecionada]);

  const buscarQuadrasPorLocalizacao = async () => {
    const cepNumerico = cepBusca.replace(/\D/g, '');
    if (cepNumerico.length !== 8) {
      carregarQuadras(); // carrega sem filtro de local
      return;
    }

    setLoadingMessage('Buscando quadras próximas ao CEP informado...');
    setLoading(true);
    try {
      const response = await fetch(`https://viacep.com.br/ws/${cepNumerico}/json/`);
      const data = await response.json();
      
      if (!data.erro) {
        const query = encodeURIComponent(`${data.logradouro || ''}, ${data.localidade || 'Jales'}, ${data.uf || 'SP'}`);
        const nominatimRes = await fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${query}`);
        const nominatimData = await nominatimRes.json();
        
        if (nominatimData && nominatimData.length > 0) {
          const lat = parseFloat(nominatimData[0].lat);
          const lon = parseFloat(nominatimData[0].lon);
          
          const q = await quadraApi.listar(lat, lon, 10.0);
          const filtradas = q.filter(quadra => quadra.ativa);
          setQuadras(filtradas);
          
          if (filtradas.length === 0) {
            setFeedback({ type: 'error', message: `Nenhuma quadra ativa encontrada em um raio de até 10 km do CEP ${cepBusca}.` });
          } else {
            setFeedback({ type: 'success', message: `${filtradas.length} quadra(s) encontrada(s) no raio do seu CEP!` });
          }
          return;
        }
      }
      
      setFeedback({ type: 'error', message: 'Não foi possível obter a localização exata do CEP digitado.' });
      carregarQuadras();
    } catch (err) {
      console.error(err);
      carregarQuadras();
    } finally {
      setLoading(false);
      setLoadingMessage('Processando dados...');
    }
  };

  useEffect(() => {
    carregarQuadras();
    carregarMeusAgendamentos();
  }, [user]);

  useEffect(() => {
    if (selectedQuadra && dataSelecionada) {
      carregarHorarios(selectedQuadra, dataSelecionada);
    } else {
      setHorarios([]);
      setSlotsSelecionados([]);
    }
  }, [selectedQuadra, dataSelecionada]);

  const carregarQuadras = async () => {
    try {
      const data = await quadraApi.listar();
      setQuadras(data);
      if (data.length > 0 && !selectedQuadra) {
        setSelectedQuadra(data[0].id_quadra);
      }
    } catch (err: any) {
      console.error(err);
    }
  };

  const carregarMeusAgendamentos = async (buscarHistorico?: boolean) => {
    if (!user) return;
    const deveBuscarHistorico = buscarHistorico ?? historicoCarregado;
    try {
      const data = await agendamentoApi.listar(deveBuscarHistorico);
      setMeusAgendamentos(data);
      if (deveBuscarHistorico) {
        setHistoricoCarregado(true);
      }
    } catch (err: any) {
      console.error(err);
    }
  };

  const carregarHistorico = async () => {
    if (carregandoHistorico) return;
    setCarregandoHistorico(true);
    try {
      const data = await agendamentoApi.listar(true);
      setMeusAgendamentos(data);
      setHistoricoCarregado(true);
    } catch (err: any) {
      console.error(err);
      setFeedback({ type: 'error', message: 'Falha ao carregar o histórico de reservas.' });
    } finally {
      setCarregandoHistorico(false);
    }
  };

  const carregarHorarios = async (quadraId: number, dataIso: string) => {
    setLoading(true);
    setSlotsSelecionados([]);
    try {
      const slots = await agendamentoApi.listarHorariosDisponiveis(quadraId, dataIso);
      const now = new Date();
      
      const slotsValidados = slots.map(slot => {
        const slotDataHora = new Date(`${dataIso}T${slot.inicio}`);
        if (slotDataHora <= now) {
          return {
            ...slot,
            disponivel: false,
            motivo: 'Horário já passou'
          };
        }
        return slot;
      });

      setHorarios(slotsValidados);
    } catch (err: any) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const toggleSlotSelection = (slot: HorarioDisponivel) => {
    setSlotsSelecionados((prev) => {
      const exists = prev.some((s) => s.inicio === slot.inicio);
      if (exists) {
        return prev.filter((s) => s.inicio !== slot.inicio);
      } else {
        return [...prev, slot];
      }
    });
  };

  // Helper para ordernar e validar slots
  const getSlotsOrdenados = () => [...slotsSelecionados].sort((a, b) => a.inicio.localeCompare(b.inicio));
  
  const isSelecaoContigua = () => {
    if (slotsSelecionados.length <= 1) return true;
    const ordenados = getSlotsOrdenados();
    for (let i = 0; i < ordenados.length - 1; i++) {
      if (ordenados[i].fim !== ordenados[i + 1].inicio) {
        return false;
      }
    }
    return true;
  };

  const confirmarAgendamento = async () => {
    if (!user || !selectedQuadra || slotsSelecionados.length === 0 || !quadraAtual) return;
    
    if (!isSelecaoContigua()) {
      setFeedback({ type: 'error', message: 'Selecione apenas horários consecutivos (sem pular espaços vazios).' });
      return;
    }

    const ordenados = getSlotsOrdenados();
    const dataHoraInicio = `${dataSelecionada}T${ordenados[0].inicio}`;
    const dataHoraFim = `${dataSelecionada}T${ordenados[ordenados.length - 1].fim}`;
    const inicioStr = ordenados[0].inicio.substring(0, 5);
    const fimStr = ordenados[ordenados.length - 1].fim.substring(0, 5);
    const dataFormatada = dataSelecionada.split('-').reverse().join('/');
    const totalPagar = quadraAtual.valorHora * ordenados.length;

    // Etapa 1: Abrir Modal de Confirmação
    setConfirmModal({
      isOpen: true,
      title: 'Confirmar Agendamento',
      description: `Deseja reservar a quadra "${quadraAtual.nome}" no dia ${dataFormatada}, das ${inicioStr} às ${fimStr} (${ordenados.length}h) pelo valor total de R$ ${totalPagar.toFixed(2)}?`,
      isDestructive: false,
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        setLoadingMessage('Gerando reserva e chave Pix segura...');
        setLoading(true);
        try {
          const novoAgendamento = await agendamentoApi.agendar({
            quadraId: selectedQuadra,
            dataHoraInicio,
            dataHoraFim,
          });

          setSlotsSelecionados([]);
          setIsBookingModalOpen(false);
          await Promise.all([
            carregarHorarios(selectedQuadra, dataSelecionada),
            carregarMeusAgendamentos(),
          ]);

          // Abre modal do Pix para pagamento
          setAgendamentoPixModal(novoAgendamento);
          setFeedback({ type: 'success', message: 'Reserva pré-agendada com sucesso! Conclua o pagamento Pix para confirmar seu jogo.' });
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Falha ao agendar.' });
        } finally {
          setLoading(false);
          setLoadingMessage('Processando dados...');
        }
      },
    });
  };

  const cancelarAgendamento = (id: number) => {
    if (!user) return;
    setConfirmModal({
      isOpen: true,
      title: 'Cancelar Agendamento',
      description: 'Deseja realmente cancelar esta reserva? O horário voltará a ficar disponível para outros praticantes.',
      isDestructive: true,
      onConfirm: async () => {
        setConfirmModal((prev) => ({ ...prev, isOpen: false }));
        setLoadingMessage('Cancelando reserva...');
        setLoading(true);
        try {
          await agendamentoApi.cancelar(id);
          setFeedback({ type: 'success', message: 'Agendamento cancelado com sucesso.' });
          await carregarMeusAgendamentos();
          if (selectedQuadra && dataSelecionada) {
            await carregarHorarios(selectedQuadra, dataSelecionada);
          }
        } catch (err: any) {
          setFeedback({ type: 'error', message: err.message || 'Falha ao cancelar.' });
        } finally {
          setLoading(false);
          setLoadingMessage('Processando dados...');
        }
      },
    });
  };

  const quadrasFiltradas = quadras.filter(
    (q) => filtroEsporte === 'TODOS' || q.tipoEsporte === filtroEsporte
  );

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 space-y-6">
      {/* Feedback Unificado */}
      <FeedbackBanner feedback={feedback} onClose={() => setFeedback(null)} />

      {/* Top Header com Abas Principais: Explorar Quadras vs Minhas Reservas */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-zinc-800 pb-5">
        <div>
          <h1 className="text-xl sm:text-2xl md:text-3xl font-bold tracking-tight text-white">
            {abaPrincipal === 'QUADRAS' ? 'Escolha a sua Quadra' : 'Minhas Reservas'}
          </h1>
          <p className="text-xs text-zinc-400 mt-1">
            {abaPrincipal === 'QUADRAS'
              ? 'Encontre e reserve os melhores horários nas quadras esportivas'
              : 'Acompanhe o status dos seus jogos e pagamentos Pix'}
          </p>
        </div>

        {/* Segmented Control / Botões de Abas */}
        <div className="flex bg-zinc-900/90 p-1.5 rounded-2xl border border-zinc-800 w-full sm:w-auto shrink-0 shadow-lg">
          <button
            onClick={() => setAbaPrincipal('QUADRAS')}
            className={`flex-1 sm:flex-initial px-4 py-2 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-2 active:scale-95 ${
              abaPrincipal === 'QUADRAS'
                ? 'bg-white text-zinc-950 shadow-sm'
                : 'text-zinc-400 hover:text-white'
            }`}
          >
            <MapPin className="w-4 h-4 text-emerald-500" />
            <span>Explorar Quadras</span>
            <span className={`text-[10px] font-mono px-2 py-0.5 rounded-full font-bold ${
              abaPrincipal === 'QUADRAS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-300'
            }`}>
              {quadrasFiltradas.length}
            </span>
          </button>

          <button
            onClick={() => setAbaPrincipal('RESERVAS')}
            className={`flex-1 sm:flex-initial px-4 py-2 text-xs font-bold rounded-xl transition-all flex items-center justify-center gap-2 active:scale-95 ${
              abaPrincipal === 'RESERVAS'
                ? 'bg-white text-zinc-950 shadow-sm'
                : 'text-zinc-400 hover:text-white'
            }`}
          >
            <Clock className="w-4 h-4 text-sky-400" />
            <span>Minhas Reservas</span>
            {contadoresReservas.ativos > 0 && (
              <span className="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full bg-emerald-500 text-zinc-950 animate-pulse">
                {contadoresReservas.ativos}
              </span>
            )}
          </button>
        </div>
      </div>

      {/* ABA 1: EXPLORAR QUADRAS */}
      {abaPrincipal === 'QUADRAS' && (
        <div className="space-y-6">
          {/* Barra de Filtros e Busca */}
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 bg-zinc-900/50 p-3 sm:p-4 rounded-2xl border border-zinc-850">
            {/* Dropdown de Esportes no Mobile */}
            <div className="relative block sm:hidden w-full">
              <select
                value={filtroEsporte}
                onChange={(e) => setFiltroEsporte(e.target.value)}
                className="w-full bg-zinc-900 border border-zinc-800 rounded-xl px-3.5 py-2.5 text-xs font-semibold text-white focus:outline-none focus:border-zinc-500 transition appearance-none cursor-pointer pr-10 shadow-sm"
              >
                {ESPORTES.map((esp) => (
                  <option key={esp} value={esp} className="bg-zinc-900 text-white">
                    {esp === 'TODOS' ? '🏟️ Todos os Esportes' : `⚡ ${esp.replace('_', ' ')}`}
                  </option>
                ))}
              </select>
              <ChevronDown className="w-4 h-4 text-zinc-400 absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            </div>

            {/* Filtro por Esporte (Pills no Desktop) */}
            <div className="hidden sm:flex items-center gap-1.5 overflow-x-auto pb-1 max-w-full scrollbar-none">
              {ESPORTES.map((esp) => (
                <button
                  key={esp}
                  onClick={() => setFiltroEsporte(esp)}
                  className={`text-xs px-3.5 py-2 rounded-xl border whitespace-nowrap transition-all active:scale-[0.98] ${
                    filtroEsporte === esp
                      ? 'bg-white text-zinc-950 font-bold border-white shadow-sm'
                      : 'bg-zinc-900/80 text-zinc-400 border-zinc-800 hover:border-zinc-700 hover:text-white'
                  }`}
                >
                  {esp === 'TODOS' ? 'Todos os Esportes' : esp.replace('_', ' ')}
                </button>
              ))}
            </div>

            {/* Filtro por CEP */}
            <div className="flex items-center gap-2 w-full md:w-auto">
              <input
                type="text"
                placeholder="Filtrar por CEP (ex: 15700-010)"
                value={cepBusca}
                onChange={(e) => {
                  const val = e.target.value.replace(/\D/g, '').replace(/(\d{5})(\d)/, '$1-$2').substring(0, 9);
                  setCepBusca(val);
                }}
                onKeyDown={(e) => e.key === 'Enter' && buscarQuadrasPorLocalizacao()}
                className="bg-zinc-900 border border-zinc-800 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-zinc-500 transition w-full md:w-52"
              />
              <button
                onClick={buscarQuadrasPorLocalizacao}
                disabled={loading}
                className="bg-zinc-850 hover:bg-zinc-800 text-white px-4 py-2.5 rounded-xl text-xs font-semibold transition border border-zinc-750 disabled:opacity-50 shrink-0 active:scale-[0.98]"
              >
                Buscar
              </button>
            </div>
          </div>

          {/* Grade de Quadras */}
          {quadrasFiltradas.length === 0 ? (
            <EmptyState
              icon={MapPin}
              title="Nenhuma quadra encontrada"
              description="Tente alternar a categoria esportiva ou buscar sem restrição de CEP."
              className="py-14"
            />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {quadrasFiltradas.map((q) => {
                const primeiraFoto = q.fotos && q.fotos.length > 0
                  ? getAssetUrl(q.fotos[0])
                  : 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=800&q=80';

                return (
                  <div
                    key={q.id_quadra}
                    onClick={() => setQuadraDetalhes(q)}
                    className="group relative rounded-2xl border border-zinc-850 hover:border-zinc-700 bg-zinc-900/80 hover:bg-zinc-900 overflow-hidden transition-all duration-300 cursor-pointer flex flex-col justify-between shadow-xl active:scale-[0.99]"
                  >
                    {/* Foto de Capa da Quadra */}
                    <div className="relative aspect-video w-full overflow-hidden bg-zinc-950">
                      <img
                        src={primeiraFoto}
                        alt={q.nome}
                        className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-105"
                        onError={(e) => {
                          e.currentTarget.src = 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?auto=format&fit=crop&w=800&q=80';
                        }}
                      />
                      <div className="absolute inset-0 bg-gradient-to-t from-zinc-950 via-zinc-950/20 to-transparent" />
                      
                      {/* Badge de Esporte e Status sobre a imagem */}
                      <div className="absolute top-3 left-3 right-3 flex items-center justify-between pointer-events-none">
                        <Badge variant="neutral" className="bg-zinc-950/80 backdrop-blur-md border border-zinc-800 text-white text-[10px]">
                          {q.tipoEsporte.replace('_', ' ')}
                        </Badge>
                        
                        <span className="p-1.5 rounded-full bg-zinc-950/80 backdrop-blur-md border border-zinc-800 text-zinc-300 group-hover:text-white transition">
                          <Info className="w-3.5 h-3.5 text-zinc-300" />
                        </span>
                      </div>

                      <div className="absolute bottom-2.5 left-3.5 right-3.5 flex items-end justify-between">
                        <div className="w-2 h-2 rounded-full bg-emerald-400 shadow-[0_0_8px_#34d399]" title="Quadra Ativa" />
                      </div>
                    </div>

                    {/* Informações da Quadra */}
                    <div className="p-4 space-y-2.5 flex-1 flex flex-col justify-between">
                      <div>
                        <div className="text-base font-bold text-white group-hover:text-emerald-400 transition-colors">
                          {q.nome}
                        </div>

                        {q.cidade && q.estado && (
                          <div className="text-xs text-zinc-400 flex items-center gap-1.5 mt-1">
                            <MapPin className="w-3.5 h-3.5 text-zinc-500 shrink-0" />
                            <span className="truncate">{q.bairro ? `${q.bairro}, ` : ''}{q.cidade} - {q.estado}</span>
                          </div>
                        )}
                      </div>

                      <div className="pt-3 border-t border-zinc-850 flex items-center justify-between gap-2 mt-auto">
                        <div>
                          <span className="text-[10px] text-zinc-500 uppercase tracking-wider font-semibold block">Valor / hora</span>
                          <span className="text-sm font-semibold text-white font-mono">
                            R$ {q.valorHora.toFixed(2)}
                          </span>
                        </div>

                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            setSelectedQuadra(q.id_quadra);
                            setIsBookingModalOpen(true);
                          }}
                          className="text-xs px-3.5 py-2 rounded-xl transition-all font-semibold flex items-center gap-1.5 active:scale-95 text-zinc-200 hover:text-white bg-zinc-850 border border-zinc-800 hover:border-zinc-700 hover:bg-zinc-800 shadow-sm"
                        >
                          <CalendarIcon className="w-3.5 h-3.5 text-emerald-400" />
                          <span>Ver Horários</span>
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* ABA 2: MINHAS RESERVAS */}
      {abaPrincipal === 'RESERVAS' && (
        <div className="max-w-4xl mx-auto space-y-6">
          <div className="bg-zinc-900/90 border border-zinc-850 rounded-2xl p-5 sm:p-6 shadow-xl space-y-5">
            {/* Sub-abas de Filtro de Reservas */}
            <div className="flex bg-zinc-950 p-1.5 rounded-xl border border-zinc-800 text-xs">
              <button
                onClick={() => setFiltroStatusReservas('ATIVOS')}
                className={`flex-1 py-2 px-3 font-semibold rounded-lg transition-all flex items-center justify-center gap-2 active:scale-[0.98] ${
                  filtroStatusReservas === 'ATIVOS'
                    ? 'bg-white text-zinc-950 shadow-sm'
                    : 'text-zinc-400 hover:text-white'
                }`}
              >
                <span>Ativos</span>
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-mono font-bold ${
                  filtroStatusReservas === 'ATIVOS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-400'
                }`}>
                  {contadoresReservas.ativos}
                </span>
              </button>

              <button
                onClick={() => setFiltroStatusReservas('REALIZADOS')}
                className={`flex-1 py-2 px-3 font-semibold rounded-lg transition-all flex items-center justify-center gap-2 active:scale-[0.98] ${
                  filtroStatusReservas === 'REALIZADOS'
                    ? 'bg-white text-zinc-950 shadow-sm'
                    : 'text-zinc-400 hover:text-white'
                }`}
              >
                <span>Realizados</span>
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-mono font-bold ${
                  filtroStatusReservas === 'REALIZADOS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-400'
                }`}>
                  {historicoCarregado ? contadoresReservas.realizados : '—'}
                </span>
              </button>

              <button
                onClick={() => setFiltroStatusReservas('CANCELADOS')}
                className={`flex-1 py-2 px-3 font-semibold rounded-lg transition-all flex items-center justify-center gap-2 active:scale-[0.98] ${
                  filtroStatusReservas === 'CANCELADOS'
                    ? 'bg-white text-zinc-950 shadow-sm'
                    : 'text-zinc-400 hover:text-white'
                }`}
              >
                <span>Cancelados</span>
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-mono font-bold ${
                  filtroStatusReservas === 'CANCELADOS' ? 'bg-zinc-200 text-zinc-950' : 'bg-zinc-800 text-zinc-400'
                }`}>
                  {historicoCarregado ? contadoresReservas.cancelados : '—'}
                </span>
              </button>
            </div>

            {/* Listagem de Reservas ou Botão de Carregar Histórico */}
            {filtroStatusReservas !== 'ATIVOS' && !historicoCarregado ? (
              <div className="py-12 px-6 flex flex-col items-center justify-center text-center bg-zinc-950/60 border border-zinc-800/80 rounded-2xl space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-400">
                  <History className="w-6 h-6" />
                </div>
                <div className="space-y-1.5 max-w-md">
                  <h4 className="text-sm font-semibold text-white">
                    Histórico não carregado
                  </h4>
                  <p className="text-xs text-zinc-400">
                    Por padrão carregamos apenas suas reservas ativas para maior rapidez. Clique abaixo para carregar todo o seu histórico.
                  </p>
                </div>
                <button
                  onClick={carregarHistorico}
                  disabled={carregandoHistorico}
                  className="mt-2 inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-primary-600 hover:bg-primary-500 disabled:opacity-60 text-white font-semibold text-xs shadow-lg shadow-primary-950/30 transition active:scale-[0.98]"
                >
                  {carregandoHistorico ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      <span>Carregando histórico...</span>
                    </>
                  ) : (
                    <>
                      <History className="w-4 h-4" />
                      <span>Carregar histórico de reservas</span>
                    </>
                  )}
                </button>
              </div>
            ) : agendamentosFiltrados.length === 0 ? (
              <EmptyState
                icon={Clock}
                title={
                  filtroStatusReservas === 'ATIVOS'
                    ? 'Nenhuma reserva ativa no momento'
                    : filtroStatusReservas === 'REALIZADOS'
                    ? 'Nenhuma reserva realizada no histórico'
                    : 'Nenhuma reserva cancelada'
                }
                description={
                  filtroStatusReservas === 'ATIVOS'
                    ? 'Clique na aba "Explorar Quadras" para encontrar uma quadra e agendar seu jogo.'
                    : 'Seus registros aparecerão aqui conforme as partidas forem finalizadas ou canceladas.'
                }
                className="py-14"
              />
            ) : (
              <div className="space-y-3.5">
                {agendamentosFiltrados.map((ag) => {
                  const isCancelado = ag.status === 'CANCELADO';
                  const isPassado = new Date(ag.dataHoraFim) < new Date();
                  const [data, tempoInicio] = ag.dataHoraInicio.split('T');
                  const [, tempoFim] = ag.dataHoraFim.split('T');
                  const horaInicio = tempoInicio ? tempoInicio.substring(0, 5) : '';
                  const horaFim = tempoFim ? tempoFim.substring(0, 5) : '';

                  return (
                    <div
                      key={ag.id_agendamento}
                      className="p-4 sm:p-5 rounded-2xl bg-zinc-950 border border-zinc-850 space-y-3 transition hover:border-zinc-700 shadow-md"
                    >
                      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3">
                        <div>
                          <div className="text-base font-bold text-white">
                            {ag.nomeQuadra}
                          </div>
                          <div className="text-xs text-zinc-400 flex items-center gap-1.5 mt-1">
                            <CalendarIcon className="w-3.5 h-3.5 text-zinc-500" />
                            <span>{data.split('-').reverse().join('/')}</span>
                            <span className="text-zinc-600">•</span>
                            <span>{horaInicio} às {horaFim}</span>
                          </div>
                          {ag.status === 'PENDENTE' && !isPassado && (() => {
                            const tempo = getTempoRestantePix(ag.criadoEm);
                            return tempo ? (
                              <div className="text-xs text-amber-400 font-mono flex items-center gap-1.5 mt-2 font-semibold bg-amber-950/30 border border-amber-500/20 px-2.5 py-1 rounded-lg w-fit">
                                <Clock className="w-3.5 h-3.5 animate-pulse text-amber-400" />
                                <span>Pague via Pix em até {tempo}</span>
                              </div>
                            ) : (
                              <div className="text-xs text-red-400 font-mono flex items-center gap-1.5 mt-2 font-semibold bg-red-950/30 border border-red-500/20 px-2.5 py-1 rounded-lg w-fit">
                                <Clock className="w-3.5 h-3.5 text-red-400" />
                                <span>Tempo de pagamento expirado</span>
                              </div>
                            );
                          })()}
                        </div>

                        <Badge variant={isCancelado ? 'outline' : isPassado ? 'neutral' : ag.status === 'PENDENTE' ? 'warning' : 'success'}>
                          {isCancelado ? 'CANCELADO' : isPassado ? 'REALIZADO' : ag.status}
                        </Badge>
                      </div>

                      <div className="flex items-center justify-between pt-3 border-t border-zinc-850 text-xs">
                        <span className="text-zinc-200 font-mono font-bold text-sm">
                          R$ {ag.valorTotal.toFixed(2)}
                        </span>

                        <div className="flex items-center gap-3">
                          {ag.status === 'PENDENTE' && (
                            <button
                              onClick={() => setAgendamentoPixModal(ag)}
                              className="text-xs bg-emerald-400 hover:bg-emerald-300 text-zinc-950 font-bold px-3.5 py-2 rounded-xl transition flex items-center gap-1.5 shadow-sm active:scale-[0.98]"
                            >
                              <QrCode className="w-3.5 h-3.5" />
                              <span>Pagar com Pix</span>
                            </button>
                          )}

                          {!isCancelado && (
                            <button
                              onClick={() => cancelarAgendamento(ag.id_agendamento)}
                              className="text-xs text-red-400 hover:text-red-300 font-semibold transition underline underline-offset-2 active:scale-95"
                            >
                              Cancelar
                            </button>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Modal Dedicado de Horários e Agendamento (BookingModal) */}
      <BookingModal
        isOpen={isBookingModalOpen}
        quadra={quadraAtual || quadras.find((q) => q.id_quadra === selectedQuadra) || null}
        dataSelecionada={dataSelecionada}
        setDataSelecionada={setDataSelecionada}
        diasDisponiveis={diasDisponiveis}
        horarios={horarios}
        slotsSelecionados={slotsSelecionados}
        toggleSlotSelection={toggleSlotSelection}
        getSlotsOrdenados={getSlotsOrdenados}
        isSelecaoContigua={isSelecaoContigua}
        loading={loading}
        onConfirmar={confirmarAgendamento}
        onClose={() => {
          setIsBookingModalOpen(false);
          setSlotsSelecionados([]);
        }}
        onOpenDetails={() => {
          const q = quadraAtual || quadras.find((item) => item.id_quadra === selectedQuadra);
          if (q) {
            setIsBookingModalOpen(false);
            setQuadraDetalhes(q);
          }
        }}
      />

      {/* Modal Pix para Pagamento */}
      <ModalPix
        isOpen={!!agendamentoPixModal}
        agendamento={agendamentoPixModal}
        onClose={() => setAgendamentoPixModal(null)}
        onSuccess={() => {
          setAgendamentoPixModal(null);
          setFeedback({ type: 'success', message: 'Pagamento Pix aprovado com sucesso! Sua reserva está confirmada.' });
          carregarMeusAgendamentos();
          if (selectedQuadra && dataSelecionada) {
            carregarHorarios(selectedQuadra, dataSelecionada);
          }
        }}
      />

      {/* Modal de Confirmação Estilizado */}
      <ConfirmModal
        isOpen={confirmModal.isOpen}
        title={confirmModal.title}
        description={confirmModal.description}
        isDestructive={confirmModal.isDestructive}
        confirmLabel={confirmModal.isDestructive ? 'Sim, Cancelar Reserva' : 'Confirmar'}
        cancelLabel="Voltar"
        onConfirm={confirmModal.onConfirm}
        onCancel={() => setConfirmModal((prev) => ({ ...prev, isOpen: false }))}
      />

      {/* Modal de Detalhes da Quadra com Carrossel */}
      <CourtDetailsModal
        isOpen={!!quadraDetalhes}
        quadra={quadraDetalhes}
        onClose={() => setQuadraDetalhes(null)}
        onSelectForBooking={(id) => {
          setSelectedQuadra(id);
          setQuadraDetalhes(null);
          setIsBookingModalOpen(true);
        }}
      />

      {/* Loading Overlay Global Esmaecido */}
      <LoadingOverlay isLoading={loading} message={loadingMessage} />
    </div>
  );
};


