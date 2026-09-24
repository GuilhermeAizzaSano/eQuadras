import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CourtHistoryModal } from './CourtHistoryModal';
import { agendamentoApi } from '../../api/apiClient';
import type { Agendamento, Quadra } from '../../types';

vi.mock('../../api/apiClient', () => ({
  agendamentoApi: {
    listarPorQuadraPaginado: vi.fn(),
    obterContadoresPorQuadra: vi.fn(),
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

describe('CourtHistoryModal (Server-side Pagination)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('exibe agendamentos retornados pelo servidor e busca contadores da quadra', async () => {
    vi.mocked(agendamentoApi.obterContadoresPorQuadra).mockResolvedValue({
      TODOS: 3,
      ATIVOS: 2,
      REALIZADOS: 1,
      CANCELADOS: 0,
    });
    vi.mocked(agendamentoApi.listarPorQuadraPaginado).mockResolvedValue({
      content: [mockAgendamento(1), mockAgendamento(2)],
      page: 0,
      size: 5,
      totalElements: 2,
      totalPages: 1,
    });

    render(
      <CourtHistoryModal
        isOpen={true}
        quadra={mockQuadra}
        onClose={vi.fn()}
      />
    );

    // Contadores exibidos
    await waitFor(() => {
      expect(screen.getByText('3')).toBeDefined();
      expect(screen.getByText('2')).toBeDefined();
      expect(screen.getByText('1')).toBeDefined();
    });

    // Agendamentos exibidos
    expect(await screen.findByText('Atleta 1')).toBeDefined();
    expect(screen.getByText('Atleta 2')).toBeDefined();
    expect(agendamentoApi.listarPorQuadraPaginado).toHaveBeenCalledWith(
      expect.objectContaining({ quadraId: 10, page: 0, size: 5, aba: undefined })
    );
  });

  it('ao mudar de aba para ATIVOS, envia aba correspondente na requisição', async () => {
    vi.mocked(agendamentoApi.obterContadoresPorQuadra).mockResolvedValue({
      TODOS: 1,
      ATIVOS: 1,
      REALIZADOS: 0,
      CANCELADOS: 0,
    });
    vi.mocked(agendamentoApi.listarPorQuadraPaginado).mockResolvedValue({
      content: [mockAgendamento(1)],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    });

    render(
      <CourtHistoryModal
        isOpen={true}
        quadra={mockQuadra}
        onClose={vi.fn()}
      />
    );

    await screen.findByText('Atleta 1');

    const botaoAtivos = screen.getByRole('button', { name: /ativos/i });
    fireEvent.click(botaoAtivos);

    await waitFor(() => {
      expect(agendamentoApi.listarPorQuadraPaginado).toHaveBeenCalledWith(
        expect.objectContaining({ quadraId: 10, page: 0, aba: 'ATIVOS' })
      );
    });
  });
});
