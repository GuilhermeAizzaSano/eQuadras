import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { DayAgendaModal } from './DayAgendaModal';
import { agendamentoApi } from '../../api/apiClient';
import type { Agendamento, Quadra } from '../../types';

vi.mock('../../api/apiClient', () => ({
  agendamentoApi: {
    listarAgendaDoDiaPaginado: vi.fn(),
    obterContadoresAgendaDoDia: vi.fn(),
  },
}));

const mockQuadra: Quadra = {
  id_quadra: 10,
  nome: 'Quadra Central',
  tipoEsporte: 'FUTEBOL',
  valorHora: 120,
  ativa: true,
};

const mockAgendamento = (id: number, status: 'PENDENTE' | 'CONFIRMADO' | 'CANCELADO' = 'CONFIRMADO'): Agendamento => ({
  id_agendamento: id,
  usuarioId: 1,
  nomeUsuario: `Atleta ${id}`,
  telefoneUsuario: '11999990001',
  quadraId: 10,
  nomeQuadra: 'Quadra Central',
  dataHoraInicio: '2026-10-01T14:00:00',
  dataHoraFim: '2026-10-01T15:00:00',
  valorTotal: 120,
  status,
  criadoEm: '2026-09-24T10:00:00',
});

const defaultProps = {
  isOpen: true,
  dataSelecionada: '2026-10-01',
  minhasQuadras: [mockQuadra],
  mapaBloqueiosPorQuadra: {},
  horariosDisponiveisPorQuadra: {},
  loadingHorariosModal: false,
  quadraSelecionadaAgendaId: 'TODAS' as const,
  statusFiltroModal: 'TODOS' as const,
  visualizacaoAgendaAba: 'LISTA_RESERVAS' as const,
  filtroAgendaAdmin: 'ATIVOS' as const,
  highlightedAgendamentoId: null,
  onClose: vi.fn(),
  onQuadraChange: vi.fn(),
  onStatusFiltroChange: vi.fn(),
  onVisualizacaoAbaChange: vi.fn(),
  onFiltroAgendaAdminChange: vi.fn(),
  onSelectHighlightedAgendamento: vi.fn(),
  onCancelarAgendamento: vi.fn(),
  onVerQuadra: vi.fn(),
  getTempoRestantePix: vi.fn(() => null),
};

describe('DayAgendaModal (Server-side Pagination)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('exibe agendamentos paginados do dia e contadores por aba vindos da API', async () => {
    vi.mocked(agendamentoApi.obterContadoresAgendaDoDia).mockResolvedValue({
      ATIVOS: 5,
      REALIZADOS: 2,
      CANCELADOS: 1,
    });
    vi.mocked(agendamentoApi.listarAgendaDoDiaPaginado).mockResolvedValue({
      content: [mockAgendamento(1), mockAgendamento(2)],
      page: 0,
      size: 5,
      totalElements: 5,
      totalPages: 1,
    });

    render(<DayAgendaModal {...defaultProps} />);

    // Contadores exibidos nas abas
    expect(await screen.findByText('5')).toBeDefined();
    expect(screen.getByText('2')).toBeDefined();
    expect(screen.getByText('1')).toBeDefined();

    // Agendamentos exibidos na lista
    expect(await screen.findByText('Atleta 1')).toBeDefined();
    expect(screen.getByText('Atleta 2')).toBeDefined();

    expect(agendamentoApi.listarAgendaDoDiaPaginado).toHaveBeenCalledWith(
      expect.objectContaining({ data: '2026-10-01', page: 0, size: 5, aba: 'ATIVOS' })
    );
  });

  it('ao clicar na aba CANCELADOS, chama onFiltroAgendaAdminChange', async () => {
    vi.mocked(agendamentoApi.obterContadoresAgendaDoDia).mockResolvedValue({
      ATIVOS: 0,
      REALIZADOS: 0,
      CANCELADOS: 3,
    });
    vi.mocked(agendamentoApi.listarAgendaDoDiaPaginado).mockResolvedValue({
      content: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
    });

    const onFiltroAgendaAdminChange = vi.fn();
    render(<DayAgendaModal {...defaultProps} onFiltroAgendaAdminChange={onFiltroAgendaAdminChange} />);

    await waitFor(() => {
      expect(agendamentoApi.obterContadoresAgendaDoDia).toHaveBeenCalled();
    });

    const botaoCancelados = screen.getByRole('button', { name: /cancelados/i });
    fireEvent.click(botaoCancelados);

    expect(onFiltroAgendaAdminChange).toHaveBeenCalledWith('CANCELADOS');
  });
});
