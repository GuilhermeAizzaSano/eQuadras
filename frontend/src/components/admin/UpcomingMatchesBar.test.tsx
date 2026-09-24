import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { UpcomingMatchesBar } from './UpcomingMatchesBar';
import { Agendamento, Quadra } from '../../types';

describe('UpcomingMatchesBar', () => {
  const quadraMock: Quadra = {
    id_quadra: 1,
    nome: 'Quadra Society 1',
    tipoEsporte: 'FUTEBOL',
    valorHora: 100,
    descricao: 'Quadra de grama sintética',
    ativa: true,
    dataLimiteAgendamento: '2026-12-31',
    fotos: [],
  };

  const agendamentoMock: Agendamento = {
    id_agendamento: 101,
    quadraId: 1,
    nomeQuadra: 'Quadra Society 1',
    usuarioId: 10,
    nomeUsuario: 'Carlos Silva',
    telefoneUsuario: '(11) 99999-9999',
    dataHoraInicio: '2026-09-24T18:00:00',
    dataHoraFim: '2026-09-24T19:00:00',
    valorTotal: 100,
    status: 'CONFIRMADO',
    criadoEm: '2026-09-24T10:00:00',
  };

  it('não deve renderizar nada se a lista de proximosJogos estiver vazia', () => {
    const { container } = render(
      <UpcomingMatchesBar
        proximosJogos={[]}
        minhasQuadras={[quadraMock]}
        onAbrirAgendamento={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('deve renderizar os jogos fornecidos pelo servidor e responder ao clique', () => {
    const onAbrirMock = vi.fn();
    render(
      <UpcomingMatchesBar
        proximosJogos={[agendamentoMock]}
        minhasQuadras={[quadraMock]}
        onAbrirAgendamento={onAbrirMock}
      />
    );

    expect(screen.getByText('Partidas Imediatas (Hoje)')).toBeDefined();
    expect(screen.getByText('Quadra Society 1')).toBeDefined();
    expect(screen.getByText(/Carlos Silva/)).toBeDefined();

    const card = screen.getByText(/Carlos Silva/).closest('.cursor-pointer');
    expect(card).not.toBeNull();
    fireEvent.click(card!);
    expect(onAbrirMock).toHaveBeenCalledTimes(1);
    expect(onAbrirMock).toHaveBeenCalledWith(agendamentoMock);
  });
});
