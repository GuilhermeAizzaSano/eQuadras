import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { quadraApi, agendamentoApi, bloqueioApi } from '../api/apiClient';
import { Quadra, HorarioDisponivel, Agendamento, DiaSemana, BloqueioHorario } from '../types';
import { FeedbackBanner, ConfirmModal, ModalPix, LoadingOverlay, CourtDetailsModal, BookingModal } from '../components/ui';
import { ClientBookingsList, CourtSearchBar, CourtCardGrid, ModoBusca, PendingPaymentAlert } from '../components/client';
import { parseDataHoraLocal, getAgoraBrasilia } from '../utils/dateUtils';

const DIA_SEMANA_MAP: DiaSemana[] = [
  'SUNDAY',
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
];

interface ClientDashboardProps {
  activeTab?: 'QUADRAS' | 'AGENDAS';
}

export const ClientDashboard: React.FC<ClientDashboardProps> = ({ activeTab = 'QUADRAS' }) => {
  const { user } = useAuth();
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
  const quadraAtual = useMemo(() => quadras.find((q) => q.id_quadra === selectedQuadra), [quadras, selectedQuadra]);

  useEffect(() => {
    if (isBookingModalOpen && selectedQuadra) {
      bloqueioApi.listar(selectedQuadra)
        .then(setBloqueiosQuadra)
        .catch(() => setBloqueiosQuadra([]));
    } else if (!isBookingModalOpen) {
      setBloqueiosQuadra([]);
    }
  }, [isBookingModalOpen, selectedQuadra]);

  const [loading, setLoading] = useState(false);
  const [loadingMessage, setLoadingMessage] = useState('Processando dados...');
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Modal Pix
  const [agendamentoPixModal, setAgendamentoPixModal] = useState<Agendamento | null>(null);

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

  const [modoBusca, setModoBusca] = useState<ModoBusca>('ATRIBUTOS');
  const [buscaNome, setBuscaNome] = useState('');
  const [buscaEndereco, setBuscaEndereco] = useState('');
  const [cepBusca, setCepBusca] = useState('');

  // Paginação de 6 em 6
  const [paginaAtual, setPaginaAtual] = useState(1);
  const [totalPaginas, setTotalPaginas] = useState(1);
  const [totalItens, setTotalItens] = useState(0);

  // Coordenadas ativas da busca por CEP (para manter paginação consistente em buscas por geolocalização)
  const [coordsAtivas, setCoordsAtivas] = useState<{ lat: number; lon: number } | null>(null);

  const alternarModoBusca = (novoModo: ModoBusca) => {
    setModoBusca(novoModo);
    setPaginaAtual(1);
    if (novoModo === 'ATRIBUTOS') {
      if (cepBusca || coordsAtivas) {
        setCepBusca('');
        setCoordsAtivas(null);
        setFeedback(null);
      }
    } else {
      setBuscaNome('');
      setBuscaEndereco('');
    }
  };

  const limparFiltrosAtributos = () => {
    setBuscaNome('');
    setBuscaEndereco('');
    setPaginaAtual(1);
  };

  const limparFiltroCep = () => {
    setCepBusca('');
    setCoordsAtivas(null);
    setPaginaAtual(1);
    setFeedback(null);
  };

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

  const cepAbortControllerRef = useRef<AbortController | null>(null);

  const buscarQuadrasPorLocalizacao = async () => {
    const cepNumerico = cepBusca.replace(/\D/g, '');
    if (cepNumerico.length !== 8) {
      carregarQuadras(); // carrega sem filtro de local
      return;
    }

    if (cepAbortControllerRef.current) {
      cepAbortControllerRef.current.abort();
    }
    cepAbortControllerRef.current = new AbortController();
    const signal = cepAbortControllerRef.current.signal;

    setLoadingMessage('Buscando quadras próximas ao CEP informado...');
    setLoading(true);
    try {
      const response = await fetch(`https://viacep.com.br/ws/${cepNumerico}/json/`, { signal });
      const data = await response.json();
      
      if (!data.erro) {
        let nominatimData: Array<{ lat: string; lon: string }> = [];

        const fetchNominatim = async (query: string) => {
          try {
            const res = await fetch(
              `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=1`,
              { headers: { 'User-Agent': 'eQuadras-App/1.0' }, signal }
            );
            if (res.ok) {
              return await res.json();
            }
          } catch {
            return [];
          }
          return [];
        };

        // 1. Tenta com logradouro + bairro + cidade + UF
        if (data.logradouro && data.bairro && data.localidade) {
          nominatimData = await fetchNominatim(
            `${data.logradouro}, ${data.bairro}, ${data.localidade}, ${data.uf || 'SP'}, Brasil`
          );
        }

        // 2. Se não achar, tenta logradouro + cidade + UF (caso o nome do bairro divirja no OSM)
        if ((!nominatimData || nominatimData.length === 0) && data.logradouro && data.localidade) {
          nominatimData = await fetchNominatim(
            `${data.logradouro}, ${data.localidade}, ${data.uf || 'SP'}, Brasil`
          );
        }

        // 3. Se ainda não achar, tenta bairro + cidade + UF (centro do bairro)
        if ((!nominatimData || nominatimData.length === 0) && data.bairro && data.localidade) {
          nominatimData = await fetchNominatim(
            `${data.bairro}, ${data.localidade}, ${data.uf || 'SP'}, Brasil`
          );
        }

        // NUNCA fazer fallback para o centro genérico da cidade (data.localidade)
        // pois distorceria buscas em raio curto (ex: 2 km) em cidades de pequeno/médio porte.
        if (nominatimData && nominatimData.length > 0) {
          const lat = parseFloat(nominatimData[0].lat);
          const lon = parseFloat(nominatimData[0].lon);
          setCoordsAtivas({ lat, lon });
          setPaginaAtual(1);
          await carregarQuadras(1, { lat, lon });
          
          return;
        }

        setFeedback({
          type: 'error',
          message: `Não foi possível localizar o endereço ou bairro do CEP ${cepBusca} no mapa para calcular o raio de 2 km.`
        });
        return;
      }
      
      setFeedback({ type: 'error', message: 'Não foi possível obter as coordenadas do CEP digitado.' });
    } catch (err: any) {
      console.error('Erro na busca por CEP:', err);
      setFeedback({ type: 'error', message: err?.message || 'Falha ao buscar localização do CEP.' });
    } finally {
      setLoading(false);
      setLoadingMessage('Processando dados...');
    }
  };

  useEffect(() => {
    carregarMeusAgendamentos();
  }, [user]);

  // Carregar quadras paginadas no backend com filtros
  const carregarQuadras = async (pagina = paginaAtual, coordsOverride?: { lat: number; lon: number } | null) => {
    try {
      const coords = coordsOverride !== undefined ? coordsOverride : coordsAtivas;
      const filtroParams: any = {
        tipoEsporte: filtroEsporte !== 'TODOS' ? filtroEsporte : undefined,
      };

      if (coords) {
        filtroParams.latitude = coords.lat;
        filtroParams.longitude = coords.lon;
        filtroParams.raioKm = 2.0;
      } else if (modoBusca === 'ATRIBUTOS') {
        if (buscaNome.trim()) filtroParams.nome = buscaNome.trim();
        if (buscaEndereco.trim()) filtroParams.endereco = buscaEndereco.trim();
      }

      const res = await quadraApi.listarPaginado(filtroParams, pagina - 1, 6);
      const quadrasConteudo = res.content ? res.content.filter((q: Quadra) => q.ativa) : [];
      setQuadras(quadrasConteudo);
      setTotalPaginas(Math.max(1, res.totalPages || 1));
      setTotalItens(res.totalElements || 0);

      if (quadrasConteudo.length > 0 && !selectedQuadra) {
        setSelectedQuadra(quadrasConteudo[0].id_quadra);
      }

      if (coords && quadrasConteudo.length === 0) {
        setFeedback({
          type: 'error',
          message: `Nenhuma quadra ativa encontrada em um raio de até 2 km do CEP ${cepBusca}.`,
        });
      } else if (coords && quadrasConteudo.length > 0 && pagina === 1) {
        setFeedback({
          type: 'success',
          message: `${res.totalElements || quadrasConteudo.length} quadra(s) encontrada(s) a até 2 km do seu CEP!`,
        });
      }
    } catch (err: any) {
      console.error(err);
    }
  };

  useEffect(() => {
    carregarQuadras(paginaAtual);
  }, [paginaAtual, filtroEsporte, buscaNome, buscaEndereco, modoBusca, coordsAtivas]);

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

  useEffect(() => {
    if (isBookingModalOpen && selectedQuadra && dataSelecionada) {
      carregarHorarios(selectedQuadra, dataSelecionada);
    } else if (!isBookingModalOpen) {
      setHorarios([]);
      setSlotsSelecionados([]);
    }
  }, [isBookingModalOpen, selectedQuadra, dataSelecionada]);

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
      description: `Quadra: ${quadraAtual.nome}\nData: ${dataFormatada}\nHorário: das ${inicioStr} às ${fimStr} (${ordenados.length}h)\nValor Total: R$ ${totalPagar.toFixed(2)}\n\nDeseja confirmar a reserva e gerar o pagamento Pix?`,
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
          setFeedback({
            type: 'success',
            message: 'Reserva pré-agendada com sucesso!\nConclua o pagamento Pix para confirmar seu jogo.',
          });
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
    const ag = meusAgendamentos.find((a) => a.id_agendamento === id);
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
      description: 'Deseja realmente cancelar esta reserva?\n\nO horário voltará a ficar disponível para outros praticantes no sistema.',
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

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8 space-y-7">
      {/* Feedback Unificado */}
      <FeedbackBanner feedback={feedback} onClose={() => setFeedback(null)} />

      {/* Top Header */}
      <div className="border-b border-white/[0.08] pb-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-[-0.03em] text-white">
            {activeTab === 'QUADRAS' ? 'Quadras' : 'Minhas Agendas'}
          </h1>
          <p className="text-xs sm:text-sm text-white/50 mt-1 tracking-tight">
            {activeTab === 'QUADRAS'
              ? 'Consulte disponibilidades em tempo real e garanta sua partida'
              : 'Acompanhe o status dos seus jogos e pagamentos Pix instantâneos'}
          </p>
        </div>

        <PendingPaymentAlert
          agendamentos={meusAgendamentos}
          onPayPix={(ag) => setAgendamentoPixModal(ag)}
        />
      </div>

      {/* ABA 1: EXPLORAR QUADRAS */}
      <div className={activeTab === 'QUADRAS' ? 'space-y-6' : 'hidden'}>
        {/* Barra de Filtros e Busca Clean Apple */}
        <CourtSearchBar
          filtroEsporte={filtroEsporte}
          onSelectEsporte={setFiltroEsporte}
          modoBusca={modoBusca}
          onAlternarModoBusca={alternarModoBusca}
          buscaNome={buscaNome}
          onChangeBuscaNome={setBuscaNome}
          buscaEndereco={buscaEndereco}
          onChangeBuscaEndereco={setBuscaEndereco}
          onLimparFiltrosAtributos={limparFiltrosAtributos}
          cepBusca={cepBusca}
          onChangeCepBusca={setCepBusca}
          onBuscarQuadrasPorLocalizacao={buscarQuadrasPorLocalizacao}
          onLimparFiltroCep={limparFiltroCep}
          loading={loading}
        />

        {/* Grade de Quadras */}
        <CourtCardGrid
          quadras={quadras}
          modoBusca={modoBusca}
          onSelectCourtDetails={(q) => setQuadraDetalhes(q)}
          onOpenBookingModal={(quadraId) => {
            setSelectedQuadra(quadraId);
            setIsBookingModalOpen(true);
          }}
          paginaAtual={paginaAtual}
          totalPaginas={totalPaginas}
          totalItens={totalItens}
          onMudarPagina={(p) => setPaginaAtual(p)}
        />
      </div>


      {/* ABA 2: MINHAS RESERVAS */}
      <div className={activeTab === 'AGENDAS' ? 'max-w-4xl mx-auto space-y-6' : 'hidden'}>
        <ClientBookingsList
          meusAgendamentos={meusAgendamentos}
          historicoCarregado={historicoCarregado}
          carregandoHistorico={carregandoHistorico}
          onCarregarHistorico={carregarHistorico}
          onPayPix={(ag) => setAgendamentoPixModal(ag)}
          onCancelBooking={cancelarAgendamento}
        />
      </div>


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
          setFeedback({
            type: 'success',
            message: 'Pagamento Pix aprovado com sucesso!\nSua reserva está confirmada.',
          });
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


