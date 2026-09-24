import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ClientBookingsList } from './ClientBookingsList';
import { agendamentoApi } from '../../api/apiClient';
import type { Agendamento } from '../../types';

vi.mock('../../api/apiClient', () => ({
  agendamentoApi: {
    listarPaginado: vi.fn(),
    obterContadores: vi.fn(),
    cancelar: vi.fn(),
  },
}));

const mockAgendamento = (id: number, status: 'PENDENTE' | 'CONFIRMADO' | 'CANCELADO' = 'CONFIRMADO'): Agendamento => ({
  id_agendamento: id,
  usuarioId: 1,
  nomeUsuario: 'Teste User',
  quadraId: 10,
  nomeQuadra: `Quadra ${id}`,
  dataHoraInicio: '2026-10-01T14:00:00',
  dataHoraFim: '2026-10-01T15:00:00',
  valorTotal: 100,
  status,
  criadoEm: '2026-09-24T10:00:00',
});

describe('ClientBookingsList (Server-side Pagination)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('exibe itens retornados pelo servidor e busca contadores', async () => {
    vi.mocked(agendamentoApi.obterContadores).mockResolvedValue({
      ATIVOS: 2,
      REALIZADOS: 5,
      CANCELADOS: 1,
    });
    vi.mocked(agendamentoApi.listarPaginado).mockResolvedValue({
      content: [mockAgendamento(1), mockAgendamento(2)],
      page: 0,
      size: 5,
      totalElements: 2,
      totalPages: 1,
    });

    render(
      <ClientBookingsList
        onPayPix={vi.fn()}
        onCancelBooking={vi.fn()}
      />
    );

    // Contadores exibidos
    await waitFor(() => {
      expect(screen.getByText('2')).toBeDefined();
      expect(screen.getByText('5')).toBeDefined();
      expect(screen.getByText('1')).toBeDefined();
    });

    // Itens exibidos
    expect(await screen.findByText('Quadra 1')).toBeDefined();
    expect(screen.getByText('Quadra 2')).toBeDefined();
  });

  it('troca de aba dispara requisição com a aba correspondente e page 0', async () => {
    vi.mocked(agendamentoApi.obterContadores).mockResolvedValue({
      ATIVOS: 1,
      REALIZADOS: 1,
      CANCELADOS: 1,
    });
    vi.mocked(agendamentoApi.listarPaginado).mockResolvedValue({
      content: [mockAgendamento(1)],
      page: 0,
      size: 5,
      totalElements: 1,
      totalPages: 1,
    });

    render(
      <ClientBookingsList
        onPayPix={vi.fn()}
        onCancelBooking={vi.fn()}
      />
    );

    await screen.findByText('Quadra 1');

    // Clica na aba Realizados
    const tabRealizados = screen.getByRole('button', { name: /realizados/i });
    fireEvent.click(tabRealizados);

    await waitFor(() => {
      expect(agendamentoApi.listarPaginado).toHaveBeenCalledWith(
        expect.objectContaining({
          aba: 'REALIZADOS',
          page: 0,
          size: 5,
        })
      );
    });
  });

  it('exibe empty state quando servidor retorna content vazio', async () => {
    vi.mocked(agendamentoApi.obterContadores).mockResolvedValue({
      ATIVOS: 0,
      REALIZADOS: 0,
      CANCELADOS: 0,
    });
    vi.mocked(agendamentoApi.listarPaginado).mockResolvedValue({
      content: [],
      page: 0,
      size: 5,
      totalElements: 0,
      totalPages: 0,
    });

    render(
      <ClientBookingsList
        onPayPix={vi.fn()}
        onCancelBooking={vi.fn()}
      />
    );

    expect(await screen.findByText(/nenhuma reserva ativa no momento/i)).toBeDefined();
  });

  it('exibe estado de erro e permite tentar novamente com reload', async () => {
    vi.mocked(agendamentoApi.obterContadores).mockResolvedValue({
      ATIVOS: 0,
      REALIZADOS: 0,
      CANCELADOS: 0,
    });
    vi.mocked(agendamentoApi.listarPaginado)
      .mockRejectedValueOnce(new Error('Erro de conexão'))
      .mockResolvedValueOnce({
        content: [mockAgendamento(1)],
        page: 0,
        size: 5,
        totalElements: 1,
        totalPages: 1,
      });

    render(
      <ClientBookingsList
        onPayPix={vi.fn()}
        onCancelBooking={vi.fn()}
      />
    );

    const retryBtn = await screen.findByRole('button', { name: /tentar novamente/i });
    expect(retryBtn).toBeDefined();

    fireEvent.click(retryBtn);

    expect(await screen.findByText('Quadra 1')).toBeDefined();
  });

  it('paginação fica desabilitada durante o carregamento', async () => {
    vi.mocked(agendamentoApi.obterContadores).mockResolvedValue({
      ATIVOS: 10,
      REALIZADOS: 0,
      CANCELADOS: 0,
    });

    let resolvePromise: (val: any) => void;
    const promise = new Promise((resolve) => {
      resolvePromise = resolve;
    });

    vi.mocked(agendamentoApi.listarPaginado)
      .mockResolvedValueOnce({
        content: [mockAgendamento(1), mockAgendamento(2)],
        page: 0,
        size: 2,
        totalElements: 10,
        totalPages: 5,
      })
      .mockReturnValueOnce(promise as any);

    render(
      <ClientBookingsList
        onPayPix={vi.fn()}
        onCancelBooking={vi.fn()}
      />
    );

    // Aguarda carregar primeira página
    expect(await screen.findByText('Quadra 1')).toBeDefined();

    const proximaBtn = screen.getByRole('button', { name: /próxima página/i }) as HTMLButtonElement;
    expect(proximaBtn.disabled).toBe(false);

    // Clica para próxima página (dispara loading pendente)
    fireEvent.click(proximaBtn);

    // O botão deve estar desabilitado durante o carregamento
    await waitFor(() => {
      expect(proximaBtn.disabled).toBe(true);
    });

    // Resolve a requisição pendente
    resolvePromise!({
      content: [mockAgendamento(3), mockAgendamento(4)],
      page: 1,
      size: 2,
      totalElements: 10,
      totalPages: 5,
    });

    await waitFor(() => {
      expect(proximaBtn.disabled).toBe(false);
    });
  });

  it('após cancelar agendamento com sucesso: recarrega a lista e os contadores', async () => {
    vi.mocked(agendamentoApi.obterContadores)
      .mockResolvedValueOnce({ ATIVOS: 1, REALIZADOS: 0, CANCELADOS: 0 })
      .mockResolvedValueOnce({ ATIVOS: 0, REALIZADOS: 0, CANCELADOS: 1 });

    vi.mocked(agendamentoApi.listarPaginado)
      .mockResolvedValueOnce({
        content: [mockAgendamento(1)],
        page: 0,
        size: 5,
        totalElements: 1,
        totalPages: 1,
      })
      .mockResolvedValueOnce({
        content: [],
        page: 0,
        size: 5,
        totalElements: 0,
        totalPages: 0,
      });

    const onCancelBookingMock = vi.fn((_ag: Agendamento, onSuccess?: () => void) => {
      if (onSuccess) onSuccess();
    });

    render(
      <ClientBookingsList
        onPayPix={vi.fn()}
        onCancelBooking={onCancelBookingMock}
      />
    );

    expect(await screen.findByText('Quadra 1')).toBeDefined();

    const cancelarBtn = screen.getByRole('button', { name: /cancelar/i });
    fireEvent.click(cancelarBtn);

    expect(onCancelBookingMock).toHaveBeenCalledWith(
      expect.objectContaining({ id_agendamento: 1 }),
      expect.any(Function),
    );

    // Após cancelamento bem-sucedido, recarrega e exibe empty state
    expect(await screen.findByText(/nenhuma reserva ativa no momento/i)).toBeDefined();
    expect(agendamentoApi.obterContadores).toHaveBeenCalledTimes(2);
    expect(agendamentoApi.listarPaginado).toHaveBeenCalledTimes(2);
  });
});
